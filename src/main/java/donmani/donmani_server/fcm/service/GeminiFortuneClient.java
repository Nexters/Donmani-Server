package donmani.donmani_server.fcm.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import donmani.donmani_server.fcm.entity.Fortune;
import donmani.donmani_server.fcm.entity.FortuneAiCallType;
import donmani.donmani_server.fcm.entity.FortuneProvider;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GeminiFortuneClient implements FortuneTextGenerator, FortuneImageGenerator {

	private static final String DEFAULT_IMAGE_MODEL = "gemini-3.1-flash-lite-image";
	private static final String DEFAULT_IMAGE_ASPECT_RATIO = "4:5";
	private static final String DEFAULT_IMAGE_MIME_TYPE = "image/jpeg";
	private static final String REFERENCE_IMAGE_PATH = "fortune/reference/fortune_rabbit.png";
	private static final String REFERENCE_IMAGE_MIME_TYPE = "image/png";
	private static final String INTERACTIONS_URL = "https://generativelanguage.googleapis.com/v1beta/interactions";
	private static final int IMAGE_RESPONSE_MAX_IN_MEMORY_SIZE = 20 * 1024 * 1024;
	private final WebClient webClient;
	private final ChatModel chatModel;
	private final ObjectMapper objectMapper;
	private final FortunePromptService fortunePromptService;
	private final FortuneAiCallLogService fortuneAiCallLogService;

	@Value("${spring.ai.google.genai.api-key:}")
	private String apiKey;

	@Value("${spring.ai.google.genai.chat.options.model:}")
	private String textModel;

	@Override
	public FortuneProvider supports() {
		return FortuneProvider.GEMINI;
	}

	@Override
	public List<GeneratedFortunePayload> generateMonthlyFortunes(java.time.YearMonth targetMonth) {
		assertApiKeyConfigured();

		String prompt = fortunePromptService.buildMonthlyPrompt(targetMonth);
		String jsonText;
		try {
			jsonText = callGeminiTextGeneration(prompt);
			fortuneAiCallLogService.recordSuccess(
				FortuneProvider.GEMINI,
				FortuneAiCallType.TEXT,
				targetMonth,
				null,
				textModel,
				prompt,
				jsonText
			);
		} catch (Exception e) {
			fortuneAiCallLogService.recordFailure(
				FortuneProvider.GEMINI,
				FortuneAiCallType.TEXT,
				targetMonth,
				null,
				textModel,
				prompt,
				e.getMessage()
			);
			throw e;
		}

		return parseMonthlyFortunes(targetMonth, jsonText);
	}

	List<GeneratedFortunePayload> parseMonthlyFortunes(
		java.time.YearMonth targetMonth,
		String jsonText
	) {
		try {
			FortuneEnvelope envelope = objectMapper.readValue(jsonText, FortuneEnvelope.class);
			return fortunePromptService.validateFortunes(targetMonth, envelope.fortunes());
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Gemini 운세 응답을 파싱하지 못했습니다.", e);
		} catch (Exception e) {
			throw new IllegalStateException("Gemini 운세 응답 검증에 실패했습니다: " + e.getMessage(), e);
		}
	}

	@Override
	public GeneratedImagePayload generateImage(Fortune fortune) {
		assertApiKeyConfigured();

		String prompt = fortunePromptService.buildImagePrompt(fortune);
		String model = DEFAULT_IMAGE_MODEL;
		try {
			GeminiImageRequest requestBody = buildImageGenerationRequest(prompt);
			JsonNode response = callGeminiImageGeneration(requestBody);
			InlineImage inlineImage = extractFirstInlineImage(response);
			byte[] imageBytes = Base64.getDecoder().decode(inlineImage.base64Data());

			fortuneAiCallLogService.recordSuccess(
				FortuneProvider.GEMINI,
				FortuneAiCallType.IMAGE,
				null,
				fortune.getTargetDate(),
				model,
				prompt,
				summarizeImageResponse(response)
			);
			return new GeneratedImagePayload(
				imageBytes,
				inlineImage.mimeType(),
				prompt
			);
		} catch (Exception e) {
			fortuneAiCallLogService.recordFailure(
				FortuneProvider.GEMINI,
				FortuneAiCallType.IMAGE,
				null,
				fortune.getTargetDate(),
				model,
				prompt,
				e.getMessage()
			);
			throw e;
		}
	}

	private JsonNode callGeminiImageGeneration(GeminiImageRequest requestBody) {
		return geminiImageWebClient().post()
			.uri(INTERACTIONS_URL)
			.header("x-goog-api-key", apiKey)
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(requestBody)
			.retrieve()
			.onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
				.defaultIfEmpty("")
				.map(body -> new IllegalStateException(
					"Gemini 이미지 생성 API 호출이 실패했습니다. status=%s, body=%s".formatted(
						response.statusCode().value(),
						body
					)
				)))
			.bodyToMono(JsonNode.class)
			.block();
	}

	private WebClient geminiImageWebClient() {
		ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
			.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(IMAGE_RESPONSE_MAX_IN_MEMORY_SIZE))
			.build();

		return webClient.mutate()
			.exchangeStrategies(exchangeStrategies)
			.build();
	}

	private String callGeminiTextGeneration(String prompt) {
		String content = ChatClient.create(chatModel)
			.prompt(prompt)
			.call()
			.content();

		if (!hasText(content)) {
			throw new IllegalStateException("Gemini Spring AI 응답에서 텍스트를 찾지 못했습니다.");
		}

		return content;
	}

	GeminiImageRequest buildImageGenerationRequest(String prompt) {
		return new GeminiImageRequest(
			DEFAULT_IMAGE_MODEL,
			List.of(
				GeminiInputPart.text(prompt),
				resolveReferenceImagePart()
			),
			new GeminiImageResponseFormat(
				"image",
				DEFAULT_IMAGE_MIME_TYPE,
				DEFAULT_IMAGE_ASPECT_RATIO,
				null
			)
		);
	}

	private GeminiInputPart resolveReferenceImagePart() {
		try {
			byte[] imageBytes = readReferenceImageBytes();
			return GeminiInputPart.image(
				REFERENCE_IMAGE_MIME_TYPE,
				Base64.getEncoder().encodeToString(imageBytes)
			);
		} catch (IOException e) {
			throw new IllegalStateException("Gemini reference image 파일을 읽지 못했습니다: " + REFERENCE_IMAGE_PATH, e);
		}
	}

	private byte[] readReferenceImageBytes() throws IOException {
		try (InputStream inputStream = new ClassPathResource(REFERENCE_IMAGE_PATH).getInputStream()) {
			return inputStream.readAllBytes();
		}
	}

	private InlineImage extractFirstInlineImage(JsonNode response) {
		JsonNode outputImage = firstExisting(response, "output_image", "outputImage");
		if (outputImage != null) {
			InlineImage inlineImage = readInlineImage(outputImage);
			if (inlineImage != null) {
				return inlineImage;
			}
		}

		InlineImage nestedImage = findInlineImage(response);
		if (nestedImage != null) {
			return nestedImage;
		}

		for (JsonNode candidate : response.path("candidates")) {
			for (JsonNode part : candidate.path("content").path("parts")) {
				JsonNode inlineData = part.has("inlineData") ? part.get("inlineData") : part.get("inline_data");
				if (inlineData != null && !inlineData.isMissingNode()) {
					InlineImage inlineImage = readInlineImage(inlineData);
					if (inlineImage != null) {
						return inlineImage;
					}
				}
			}
		}
		throw new IllegalStateException("Gemini 이미지 응답에서 이미지 데이터를 찾지 못했습니다.");
	}

	private void assertApiKeyConfigured() {
		if (!hasText(apiKey)) {
			throw new IllegalStateException("spring.ai.google.genai.api-key 설정이 필요합니다.");
		}
	}

	private InlineImage findInlineImage(JsonNode node) {
		if (node == null || node.isMissingNode() || node.isNull()) {
			return null;
		}

		InlineImage inlineImage = readInlineImage(node);
		if (inlineImage != null) {
			return inlineImage;
		}

		if (node.isObject() || node.isArray()) {
			for (JsonNode child : node) {
				InlineImage childImage = findInlineImage(child);
				if (childImage != null) {
					return childImage;
				}
			}
		}
		return null;
	}

	private InlineImage readInlineImage(JsonNode node) {
		String base64Data = readText(node, "data");
		if (!hasText(base64Data)) {
			return null;
		}

		String mimeType = readText(node, "mime_type", "mimeType");
		return new InlineImage(base64Data, hasText(mimeType) ? mimeType : DEFAULT_IMAGE_MIME_TYPE);
	}

	private JsonNode firstExisting(
		JsonNode node,
		String... fieldNames
	) {
		for (String fieldName : fieldNames) {
			JsonNode field = node.get(fieldName);
			if (field != null && !field.isMissingNode() && !field.isNull()) {
				return field;
			}
		}
		return null;
	}

	private String readText(
		JsonNode node,
		String... fieldNames
	) {
		for (String fieldName : fieldNames) {
			if (node.hasNonNull(fieldName)) {
				return node.get(fieldName).asText();
			}
		}
		return null;
	}

	private String summarizeImageResponse(JsonNode response) {
		JsonNode sanitized = response.deepCopy();
		omitImageData(sanitized);
		return sanitized.toString();
	}

	private void omitImageData(JsonNode node) {
		if (node == null || node.isMissingNode() || node.isNull()) {
			return;
		}
		if (node.isObject()) {
			ObjectNode objectNode = (ObjectNode)node;
			Iterator<Entry<String, JsonNode>> fields = objectNode.fields();
			while (fields.hasNext()) {
				Entry<String, JsonNode> field = fields.next();
				if (isImageDataField(field.getKey(), field.getValue())) {
					objectNode.put(field.getKey(), "[omitted]");
				} else {
					omitImageData(field.getValue());
				}
			}
			return;
		}
		if (node.isArray()) {
			for (JsonNode child : node) {
				omitImageData(child);
			}
		}
	}

	private boolean isImageDataField(
		String fieldName,
		JsonNode value
	) {
		return "b64_json".equals(fieldName) || ("data".equals(fieldName) && value.isTextual());
	}

	private boolean hasText(String value) {
		return StringUtils.hasText(value);
	}

	private record InlineImage(
		String base64Data,
		String mimeType
	) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	record GeminiImageRequest(
		String model,
		Object input,
		@JsonProperty("response_format") GeminiImageResponseFormat responseFormat
	) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	record GeminiInputPart(
		String type,
		String text,
		@JsonProperty("mime_type") String mimeType,
		String data
	) {
		static GeminiInputPart text(String text) {
			return new GeminiInputPart("text", text, null, null);
		}

		static GeminiInputPart image(
			String mimeType,
			String data
		) {
			return new GeminiInputPart("image", null, mimeType, data);
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	record GeminiImageResponseFormat(
		String type,
		@JsonProperty("mime_type") String mimeType,
		@JsonProperty("aspect_ratio") String aspectRatio,
		@JsonProperty("image_size") String imageSize
	) {
	}

	private record FortuneEnvelope(
		List<GeneratedFortunePayload> fortunes
	) {
	}
}
