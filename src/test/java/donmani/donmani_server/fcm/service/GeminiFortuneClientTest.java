package donmani.donmani_server.fcm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import donmani.donmani_server.fcm.entity.Fortune;
import donmani.donmani_server.fcm.entity.FortuneAiCallType;
import donmani.donmani_server.fcm.entity.FortuneProvider;
import reactor.core.publisher.Mono;

class GeminiFortuneClientTest {

	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

	@Test
	void parseMonthlyFortunesSeparatesValidationFailureFromJsonParsingFailure() {
		GeminiFortuneClient client = clientWithRealPromptService();
		String jsonText = """
			{
			  "fortunes": [
			    {
			      "targetDate": "2026-07-01",
			      "title": "title",
			      "subtitle": "subtitle",
			      "content": "content",
			      "item": "item"
			    }
			  ]
			}
			""";

		assertThatThrownBy(() -> client.parseMonthlyFortunes(YearMonth.of(2026, 12), jsonText))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Gemini 운세 응답 검증에 실패했습니다")
			.hasMessageContaining("생성된 운세 개수가 대상 월 일수와 맞지 않습니다.");
	}

	@Test
	void parseMonthlyFortunesKeepsJsonParsingFailureMessage() {
		GeminiFortuneClient client = clientWithRealPromptService();

		assertThatThrownBy(() -> client.parseMonthlyFortunes(YearMonth.of(2026, 12), "{"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Gemini 운세 응답을 파싱하지 못했습니다.");
	}

	@Test
	void buildImageGenerationRequestUsesNanoBananaInteractionsShape() throws Exception {
		GeminiFortuneClient client = clientWithResponse("{}");

		JsonNode request = objectMapper.valueToTree(client.buildImageGenerationRequest("image prompt"));

		assertThat(request.path("model").asText()).isEqualTo("gemini-3.1-flash-lite-image");
		assertReferenceImageInput(request, "image prompt");
		assertThat(request.path("response_format").path("type").asText()).isEqualTo("image");
		assertThat(request.path("response_format").path("mime_type").asText()).isEqualTo("image/jpeg");
		assertThat(request.path("response_format").path("aspect_ratio").asText()).isEqualTo("4:5");
		assertThat(request.path("response_format").has("image_size")).isFalse();
	}

	@Test
	void buildImageGenerationRequestIncludesDefaultReferenceImage() throws Exception {
		GeminiFortuneClient client = clientWithResponse("{}");

		JsonNode request = objectMapper.valueToTree(client.buildImageGenerationRequest("image prompt"));

		assertReferenceImageInput(request, "image prompt");
	}

	@Test
	void generateImageParsesOutputImageDataWithMimeFallback() {
		String imageBase64 = Base64.getEncoder().encodeToString(new byte[] {1, 2, 3});
		GeminiFortuneClient client = clientWithResponse("""
			{
			  "output_image": {
			    "data": "%s"
			  }
			}
			""".formatted(imageBase64));
		ReflectionTestUtils.setField(client, "apiKey", "test-key");
		when(promptService(client).buildImagePrompt(any(Fortune.class))).thenReturn("image prompt");

		GeneratedImagePayload payload = client.generateImage(fortune());

		assertThat(payload.bytes()).containsExactly(1, 2, 3);
		assertThat(payload.mimeType()).isEqualTo("image/jpeg");
		assertThat(payload.prompt()).isEqualTo("image prompt");
		verify(logService(client)).recordSuccess(
			eq(FortuneProvider.GEMINI),
			eq(FortuneAiCallType.IMAGE),
			isNull(),
			eq(LocalDate.of(2026, 7, 15)),
			eq("gemini-3.1-flash-lite-image"),
			eq("image prompt"),
			argThat(response -> response.contains("\"data\":\"[omitted]\""))
		);
	}

	@Test
	void generateImageParsesLargeOutputImageResponse() {
		byte[] imageBytes = new byte[300_000];
		imageBytes[0] = 1;
		imageBytes[imageBytes.length - 1] = 2;
		String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);
		GeminiFortuneClient client = clientWithResponse("""
			{
			  "output_image": {
			    "data": "%s",
			    "mime_type": "image/jpeg"
			  }
			}
			""".formatted(imageBase64));
		ReflectionTestUtils.setField(client, "apiKey", "test-key");
		when(promptService(client).buildImagePrompt(any(Fortune.class))).thenReturn("image prompt");

		GeneratedImagePayload payload = client.generateImage(fortune());

		assertThat(payload.bytes()).hasSize(300_000);
		assertThat(payload.bytes()[0]).isEqualTo((byte)1);
		assertThat(payload.bytes()[payload.bytes().length - 1]).isEqualTo((byte)2);
		assertThat(payload.mimeType()).isEqualTo("image/jpeg");
	}

	@Test
	void generateImageIncludesGoogleErrorBodyWhenRequestFails() {
		GeminiFortuneClient client = clientWithStatus(
			HttpStatus.BAD_REQUEST,
			"""
				{
				  "error": {
				    "message": "Unsupported response format"
				  }
				}
				"""
		);
		ReflectionTestUtils.setField(client, "apiKey", "test-key");
		when(promptService(client).buildImagePrompt(any(Fortune.class))).thenReturn("image prompt");

		assertThatThrownBy(() -> client.generateImage(fortune()))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("status=400")
			.hasMessageContaining("Unsupported response format");
		verify(logService(client)).recordFailure(
			eq(FortuneProvider.GEMINI),
			eq(FortuneAiCallType.IMAGE),
			isNull(),
			eq(LocalDate.of(2026, 7, 15)),
			eq("gemini-3.1-flash-lite-image"),
			eq("image prompt"),
			argThat(error -> error.contains("Unsupported response format"))
		);
	}

	private GeminiFortuneClient clientWithResponse(String responseBody) {
		return clientWithStatus(HttpStatus.OK, responseBody);
	}

	private GeminiFortuneClient clientWithRealPromptService() {
		WebClient webClient = WebClient.builder()
			.exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.OK).build()))
			.build();
		return new GeminiFortuneClient(
			webClient,
			org.mockito.Mockito.mock(ChatModel.class),
			objectMapper,
			new FortunePromptService(),
			org.mockito.Mockito.mock(FortuneAiCallLogService.class)
		);
	}

	private GeminiFortuneClient clientWithStatus(
		HttpStatus status,
		String responseBody
	) {
		ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
			.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(20 * 1024 * 1024))
			.build();
		WebClient webClient = WebClient.builder()
			.exchangeFunction(request -> Mono.just(ClientResponse.create(status, exchangeStrategies)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.body(responseBody)
				.build()))
			.build();
		GeminiFortuneClient client = new GeminiFortuneClient(webClient, org.mockito.Mockito.mock(ChatModel.class), objectMapper,
			org.mockito.Mockito.mock(FortunePromptService.class), org.mockito.Mockito.mock(FortuneAiCallLogService.class));
		return client;
	}

	private FortunePromptService promptService(GeminiFortuneClient client) {
		return (FortunePromptService)ReflectionTestUtils.getField(client, "fortunePromptService");
	}

	private FortuneAiCallLogService logService(GeminiFortuneClient client) {
		return (FortuneAiCallLogService)ReflectionTestUtils.getField(client, "fortuneAiCallLogService");
	}

	private void assertReferenceImageInput(JsonNode request, String prompt) throws Exception {
		assertThat(request.path("input").size()).isEqualTo(2);
		assertThat(request.path("input").path(0).path("type").asText()).isEqualTo("text");
		assertThat(request.path("input").path(0).path("text").asText()).isEqualTo(prompt);
		assertThat(request.path("input").path(1).path("type").asText()).isEqualTo("image");
		assertThat(request.path("input").path(1).path("mime_type").asText()).isEqualTo("image/png");
		assertThat(request.path("input").path(1).path("data").asText()).isEqualTo(referenceImageBase64());
	}

	private String referenceImageBase64() throws Exception {
		try (InputStream inputStream = new ClassPathResource("fortune/reference/fortune_rabbit.png").getInputStream()) {
			return Base64.getEncoder().encodeToString(inputStream.readAllBytes());
		}
	}

	private Fortune fortune() {
		return Fortune.builder()
			.targetDate(LocalDate.of(2026, 7, 15))
			.title("title")
			.subtitle("subtitle")
			.content("content")
			.item("item")
			.build();
	}
}
