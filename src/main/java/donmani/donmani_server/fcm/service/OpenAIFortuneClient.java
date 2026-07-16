package donmani.donmani_server.fcm.service;

import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseOutputText;

import donmani.donmani_server.fcm.entity.FortuneAiCallType;
import donmani.donmani_server.fcm.entity.Fortune;
import donmani.donmani_server.fcm.entity.FortuneProvider;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OpenAIFortuneClient implements FortuneTextGenerator, FortuneImageGenerator {

	private static final String DEFAULT_TEXT_MODEL = "gpt-5.4-mini";
	private static final String DEFAULT_IMAGE_MODEL = "gpt-image-2";
	private static final String DEFAULT_IMAGE_SIZE = "1024x1024";
	private static final String IMAGE_GENERATION_URL = "https://api.openai.com/v1/images/generations";

	private final WebClient webClient;
	private final ObjectMapper objectMapper;
	private final FortunePromptService fortunePromptService;
	private final FortuneAiCallLogService fortuneAiCallLogService;

	@Value("${fortune.automation.gpt.api-key:}")
	private String apiKey;

	@Value("${fortune.automation.gpt.text-model:}")
	private String textModel;

	@Value("${fortune.automation.gpt.image-model:}")
	private String imageModel;

	@Value("${fortune.automation.gpt.image-size:}")
	private String imageSize;

	@Value("${fortune.automation.gpt.image-quality:}")
	private String imageQuality;

	@Override
	public FortuneProvider supports() {
		return FortuneProvider.GPT;
	}

	@Override
	public List<GeneratedFortunePayload> generateMonthlyFortunes(java.time.YearMonth targetMonth) {
		assertApiKeyConfigured();

		String prompt = fortunePromptService.buildMonthlyPrompt(targetMonth);
		String model = resolveTextModel();
		String jsonText;
		try {
			jsonText = callOpenAIResponses(prompt, model);
			fortuneAiCallLogService.recordSuccess(
				FortuneProvider.GPT,
				FortuneAiCallType.TEXT,
				targetMonth,
				null,
				model,
				prompt,
				jsonText
			);
		} catch (Exception e) {
			fortuneAiCallLogService.recordFailure(
				FortuneProvider.GPT,
				FortuneAiCallType.TEXT,
				targetMonth,
				null,
				model,
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
			throw new IllegalStateException("GPT 운세 응답을 파싱하지 못했습니다.", e);
		} catch (Exception e) {
			throw new IllegalStateException("GPT 운세 응답 검증에 실패했습니다: " + e.getMessage(), e);
		}
	}

	@Override
	public GeneratedImagePayload generateImage(Fortune fortune) {
		assertApiKeyConfigured();

		String prompt = fortunePromptService.buildImagePrompt(fortune);
		String model = resolveImageModel();
		try {
			JsonNode response = callOpenAI(buildImageGenerationRequest(prompt));
			String base64Image = response.path("data").path(0).path("b64_json").asText(null);

			if (!StringUtils.hasText(base64Image)) {
				throw new IllegalStateException("GPT 이미지 응답에서 이미지 데이터를 찾지 못했습니다.");
			}
			byte[] imageBytes = Base64.getDecoder().decode(base64Image);

			fortuneAiCallLogService.recordSuccess(
				FortuneProvider.GPT,
				FortuneAiCallType.IMAGE,
				null,
				fortune.getTargetDate(),
				model,
				prompt,
				summarizeImageResponse(response)
			);
			return new GeneratedImagePayload(
				imageBytes,
				"image/png",
				prompt
			);
		} catch (Exception e) {
			fortuneAiCallLogService.recordFailure(
				FortuneProvider.GPT,
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

	private JsonNode callOpenAI(Map<String, Object> requestBody) {
		JsonNode response = webClient.post()
			.uri(IMAGE_GENERATION_URL)
			.header("Authorization", "Bearer " + apiKey)
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(requestBody)
			.retrieve()
			.bodyToMono(JsonNode.class)
			.block();

		if (response == null) {
			throw new IllegalStateException("OpenAI 응답이 비어 있습니다.");
		}

		return response;
	}

	private String callOpenAIResponses(
		String prompt,
		String model
	) {
		OpenAIClient client = OpenAIOkHttpClient.builder()
			.apiKey(apiKey)
			.build();
		ResponseCreateParams params = ResponseCreateParams.builder()
			.model(model)
			.instructions("너는 한국어 운세 콘텐츠 에디터다. 반드시 JSON 스키마만 반환해.")
			.input(prompt)
			.build();
		Response response = client.responses().create(params);
		String outputText = response.output().stream()
			.flatMap(outputItem -> outputItem.message().stream())
			.flatMap(message -> message.content().stream())
			.flatMap(content -> content.outputText().stream())
			.map(ResponseOutputText::text)
			.filter(StringUtils::hasText)
			.findFirst()
			.orElse(null);

		if (!StringUtils.hasText(outputText)) {
			throw new IllegalStateException("GPT 운세 응답에서 출력 텍스트를 찾지 못했습니다.");
		}
		return outputText;
	}

	private Map<String, Object> buildImageGenerationRequest(String prompt) {
		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("model", resolveImageModel());
		requestBody.put("prompt", prompt);
		requestBody.put("size", resolveImageSize());

		if (StringUtils.hasText(imageQuality)) {
			requestBody.put("quality", imageQuality);
		}

		return requestBody;
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

	private String resolveTextModel() {
		return StringUtils.hasText(textModel) ? textModel : DEFAULT_TEXT_MODEL;
	}

	private String resolveImageModel() {
		return StringUtils.hasText(imageModel) ? imageModel : DEFAULT_IMAGE_MODEL;
	}

	private String resolveImageSize() {
		return StringUtils.hasText(imageSize) ? imageSize : DEFAULT_IMAGE_SIZE;
	}

	private void assertApiKeyConfigured() {
		if (!StringUtils.hasText(apiKey)) {
			throw new IllegalStateException("fortune.automation.gpt.api-key 설정이 필요합니다.");
		}
	}

	private record FortuneEnvelope(
		List<GeneratedFortunePayload> fortunes
	) {
	}
}
