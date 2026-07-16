package donmani.donmani_server.fcm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import donmani.donmani_server.fcm.entity.Fortune;
import donmani.donmani_server.fcm.entity.FortuneAiCallType;
import donmani.donmani_server.fcm.entity.FortuneProvider;
import reactor.core.publisher.Mono;

class OpenAIFortuneClientTest {

	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

	@Test
	void parseMonthlyFortunesSeparatesValidationFailureFromJsonParsingFailure() {
		OpenAIFortuneClient client = clientWithRealPromptService();
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
			.hasMessageContaining("GPT 운세 응답 검증에 실패했습니다")
			.hasMessageContaining("생성된 운세 개수가 대상 월 일수와 맞지 않습니다.");
	}

	@Test
	void parseMonthlyFortunesKeepsJsonParsingFailureMessage() {
		OpenAIFortuneClient client = clientWithRealPromptService();

		assertThatThrownBy(() -> client.parseMonthlyFortunes(YearMonth.of(2026, 12), "{"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("GPT 운세 응답을 파싱하지 못했습니다.");
	}

	@Test
	void generateImageStoresPromptAndSanitizedResponseLog() {
		String imageBase64 = Base64.getEncoder().encodeToString(new byte[] {1, 2, 3});
		OpenAIFortuneClient client = clientWithResponse("""
			{
			  "data": [
			    {
			      "b64_json": "%s"
			    }
			  ]
			}
			""".formatted(imageBase64));
		ReflectionTestUtils.setField(client, "apiKey", "test-key");
		ReflectionTestUtils.setField(client, "imageModel", "");
		ReflectionTestUtils.setField(client, "imageSize", "");
		when(promptService(client).buildImagePrompt(any(Fortune.class))).thenReturn("image prompt");

		GeneratedImagePayload payload = client.generateImage(fortune());

		assertThat(payload.bytes()).containsExactly(1, 2, 3);
		assertThat(payload.mimeType()).isEqualTo("image/png");
		assertThat(payload.prompt()).isEqualTo("image prompt");
		verify(logService(client)).recordSuccess(
			eq(FortuneProvider.GPT),
			eq(FortuneAiCallType.IMAGE),
			isNull(),
			eq(LocalDate.of(2026, 7, 15)),
			eq("gpt-image-2"),
			eq("image prompt"),
			argThat(response -> response.contains("\"b64_json\":\"[omitted]\""))
		);
	}

	@Test
	void generateImageStoresFailureLogWhenImageDataIsMissing() {
		OpenAIFortuneClient client = clientWithResponse("""
			{
			  "data": [
			    {}
			  ]
			}
			""");
		ReflectionTestUtils.setField(client, "apiKey", "test-key");
		ReflectionTestUtils.setField(client, "imageModel", "");
		ReflectionTestUtils.setField(client, "imageSize", "");
		when(promptService(client).buildImagePrompt(any(Fortune.class))).thenReturn("image prompt");

		assertThatThrownBy(() -> client.generateImage(fortune()))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("GPT 이미지 응답에서 이미지 데이터를 찾지 못했습니다.");
		verify(logService(client)).recordFailure(
			eq(FortuneProvider.GPT),
			eq(FortuneAiCallType.IMAGE),
			isNull(),
			eq(LocalDate.of(2026, 7, 15)),
			eq("gpt-image-2"),
			eq("image prompt"),
			argThat(error -> error.contains("GPT 이미지 응답에서 이미지 데이터를 찾지 못했습니다."))
		);
	}

	private OpenAIFortuneClient clientWithResponse(String responseBody) {
		WebClient webClient = WebClient.builder()
			.exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.OK)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.body(responseBody)
				.build()))
			.build();
		return new OpenAIFortuneClient(
			webClient,
			objectMapper,
			org.mockito.Mockito.mock(FortunePromptService.class),
			org.mockito.Mockito.mock(FortuneAiCallLogService.class)
		);
	}

	private OpenAIFortuneClient clientWithRealPromptService() {
		WebClient webClient = WebClient.builder()
			.exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.OK).build()))
			.build();
		return new OpenAIFortuneClient(
			webClient,
			objectMapper,
			new FortunePromptService(),
			org.mockito.Mockito.mock(FortuneAiCallLogService.class)
		);
	}

	private FortunePromptService promptService(OpenAIFortuneClient client) {
		return (FortunePromptService)ReflectionTestUtils.getField(client, "fortunePromptService");
	}

	private FortuneAiCallLogService logService(OpenAIFortuneClient client) {
		return (FortuneAiCallLogService)ReflectionTestUtils.getField(client, "fortuneAiCallLogService");
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
