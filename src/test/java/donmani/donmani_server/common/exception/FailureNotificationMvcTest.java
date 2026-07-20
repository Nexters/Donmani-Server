package donmani.donmani_server.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import donmani.donmani_server.common.httpStatus.HttpStatusDTO;
import reactor.core.publisher.Mono;

class FailureNotificationMvcTest {

	@Test
	void unhandledExceptionReturns500AndSendsWebhookOnce() throws Exception {
		AtomicInteger requestCount = new AtomicInteger();
		MockMvc mockMvc = mockMvc(requestCount);

		mockMvc.perform(get("/api/v1/test/unhandled"))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.status").value(500))
			.andExpect(jsonPath("$.message").value("boom"));

		assertThat(requestCount).hasValue(1);
	}

	@Test
	void hiddenItemExceptionKeepsBadRequestResponseAndSendsWebhookOnce() throws Exception {
		AtomicInteger requestCount = new AtomicInteger();
		MockMvc mockMvc = mockMvc(requestCount);

		mockMvc.perform(get("/api/v1/test/hidden"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.statusCode").value(400))
			.andExpect(jsonPath("$.responseMessage").value("이미 히든 아이템을 열었습니다."))
			.andExpect(jsonPath("$.responseData").doesNotExist());

		assertThat(requestCount).hasValue(1);
	}

	@Test
	void bodyLevelFailureSendsWebhookWithoutChangingResponse() throws Exception {
		AtomicInteger requestCount = new AtomicInteger();
		MockMvc mockMvc = mockMvc(requestCount);

		mockMvc.perform(get("/api/v1/test/body-failure"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.statusCode").value(500))
			.andExpect(jsonPath("$.responseMessage").value("body failed"));

		assertThat(requestCount).hasValue(1);
	}

	@Test
	void httpStatusFailureWithoutBodySendsWebhookOnce() throws Exception {
		AtomicInteger requestCount = new AtomicInteger();
		MockMvc mockMvc = mockMvc(requestCount);

		mockMvc.perform(get("/api/v1/test/unauthorized-empty"))
			.andExpect(status().isUnauthorized());

		assertThat(requestCount).hasValue(1);
	}

	@Test
	void httpStatusFailureWithFailureBodyStillSendsWebhookOnce() throws Exception {
		AtomicInteger requestCount = new AtomicInteger();
		MockMvc mockMvc = mockMvc(requestCount);

		mockMvc.perform(get("/api/v1/test/unauthorized-body"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.statusCode").value(401));

		assertThat(requestCount).hasValue(1);
	}

	@Test
	void apiExceptionReturnsConfiguredHttpStatusAndEnvelope() throws Exception {
		AtomicInteger requestCount = new AtomicInteger();
		MockMvc mockMvc = mockMvc(requestCount);

		mockMvc.perform(get("/api/v1/test/api-exception"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.statusCode").value(500))
			.andExpect(jsonPath("$.responseMessage").value("유저 정보 없음"))
			.andExpect(jsonPath("$.responseData").doesNotExist());

		assertThat(requestCount).hasValue(1);
	}

	@Test
	void apiExceptionCanReturnActualUnauthorizedStatus() throws Exception {
		AtomicInteger requestCount = new AtomicInteger();
		MockMvc mockMvc = mockMvc(requestCount);

		mockMvc.perform(get("/api/v1/test/api-unauthorized"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.statusCode").value(401))
			.andExpect(jsonPath("$.responseMessage").value("인증 실패"))
			.andExpect(jsonPath("$.responseData").doesNotExist());

		assertThat(requestCount).hasValue(1);
	}

	@Test
	void apiExceptionUsesDynamicMessage() throws Exception {
		AtomicInteger requestCount = new AtomicInteger();
		MockMvc mockMvc = mockMvc(requestCount);

		mockMvc.perform(get("/api/v1/test/api-dynamic-message"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.statusCode").value(400))
			.andExpect(jsonPath("$.responseMessage").value("targetMonth는 yyyy-MM 형식이어야 합니다."))
			.andExpect(jsonPath("$.responseData").doesNotExist());

		assertThat(requestCount).hasValue(1);
	}

	@Test
	void noResourceFoundReturns404WithoutWebhook() throws Exception {
		AtomicInteger requestCount = new AtomicInteger();
		MockMvc mockMvc = mockMvc(requestCount);

		mockMvc.perform(get("/geoserver/web"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.message").value("요청한 리소스를 찾을 수 없습니다."));

		assertThat(requestCount).hasValue(0);
	}

	@Test
	void responseStatusNotFoundDoesNotSendWebhook() throws Exception {
		AtomicInteger requestCount = new AtomicInteger();
		MockMvc mockMvc = mockMvc(requestCount);

		mockMvc.perform(get("/api/v1/test/not-found"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404));

		assertThat(requestCount).hasValue(0);
	}

	@Test
	void requestBodyIsIncludedAndRedactedAfterRequestBodyIsConsumed() throws Exception {
		AtomicReference<ClientRequest> capturedRequest = new AtomicReference<>();
		MockMvc mockMvc = mockMvc(capturedRequest);

		mockMvc.perform(post("/api/v1/test/body-exception")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"userKey":"user-123","password":"pw","apiKey":"api-key","memo":"hello"}
					"""))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.status").value(500));

		MockClientHttpRequest renderedRequest = render(capturedRequest.get());
		String body = renderedRequest.getBodyAsString().block();
		assertThat(body).contains("\\\"userKey\\\" : \\\"user-123\\\"");
		assertThat(body).contains("\\\"password\\\" : \\\"***\\\"");
		assertThat(body).contains("\\\"apiKey\\\" : \\\"***\\\"");
		assertThat(body).contains("\\\"memo\\\" : \\\"hello\\\"");
		assertThat(body).doesNotContain("pw");
		assertThat(body).doesNotContain("api-key");
		assertThat(body).doesNotContain("stackTop");
	}

	private MockMvc mockMvc(AtomicInteger requestCount) {
		ExceptionWebhookService exceptionWebhookService = exceptionWebhookService(requestCount);
		return MockMvcBuilders.standaloneSetup(new TestFailureController())
			.setControllerAdvice(
				new GlobalExceptionHandler(exceptionWebhookService),
				new FailureResponseBodyAdvice(exceptionWebhookService)
			)
			.addFilters(new FailureStatusFilter(exceptionWebhookService))
			.build();
	}

	private MockMvc mockMvc(AtomicReference<ClientRequest> capturedRequest) {
		ExceptionWebhookService exceptionWebhookService = exceptionWebhookService(capturedRequest);
		return MockMvcBuilders.standaloneSetup(new TestFailureController())
			.setControllerAdvice(
				new GlobalExceptionHandler(exceptionWebhookService),
				new FailureResponseBodyAdvice(exceptionWebhookService)
			)
			.addFilters(new FailureStatusFilter(exceptionWebhookService))
			.build();
	}

	private ExceptionWebhookService exceptionWebhookService(AtomicInteger requestCount) {
		WebClient webClient = WebClient.builder()
			.exchangeFunction(request -> {
				requestCount.incrementAndGet();
				return Mono.just(ClientResponse.create(HttpStatus.NO_CONTENT).build());
			})
			.build();
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("test");
		ExceptionWebhookService service = new ExceptionWebhookService(webClient, environment);
		ReflectionTestUtils.setField(service, "webhookUrl", "https://discord.test/webhook");
		return service;
	}

	private ExceptionWebhookService exceptionWebhookService(AtomicReference<ClientRequest> capturedRequest) {
		WebClient webClient = WebClient.builder()
			.exchangeFunction(request -> {
				capturedRequest.set(request);
				return Mono.just(ClientResponse.create(HttpStatus.NO_CONTENT).build());
			})
			.build();
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("test");
		ExceptionWebhookService service = new ExceptionWebhookService(webClient, environment);
		ReflectionTestUtils.setField(service, "webhookUrl", "https://discord.test/webhook");
		return service;
	}

	private MockClientHttpRequest render(ClientRequest clientRequest) {
		MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.POST, clientRequest.url());
		clientRequest.writeTo(request, ExchangeStrategies.withDefaults()).block();
		return request;
	}

	@RestController
	static class TestFailureController {
		@GetMapping("/api/v1/test/unhandled")
		String unhandled() {
			throw new RuntimeException("boom");
		}

		@GetMapping("/api/v1/test/hidden")
		String hidden() {
			throw ApiException.of(ApiErrorCode.HIDDEN_ITEM_ALREADY_OPENED);
		}

		@GetMapping("/api/v1/test/body-failure")
		ResponseEntity<HttpStatusDTO<Void>> bodyFailure() {
			return ResponseEntity.ok(HttpStatusDTO.response(
				HttpStatus.INTERNAL_SERVER_ERROR.value(),
				"body failed",
				null
			));
		}

		@GetMapping("/api/v1/test/unauthorized-empty")
		ResponseEntity<Void> unauthorizedEmpty() {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		@GetMapping("/api/v1/test/unauthorized-body")
		ResponseEntity<HttpStatusDTO<Void>> unauthorizedBody() {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(HttpStatusDTO.response(HttpStatus.UNAUTHORIZED.value(), "인증 실패", null));
		}

		@GetMapping("/api/v1/test/api-exception")
		String apiException() {
			throw ApiException.of(ApiErrorCode.USER_NOT_FOUND);
		}

		@GetMapping("/api/v1/test/api-unauthorized")
		String apiUnauthorized() {
			throw ApiException.of(ApiErrorCode.AUTOMATION_UNAUTHORIZED);
		}

		@GetMapping("/api/v1/test/api-dynamic-message")
		String apiDynamicMessage() {
			throw ApiException.of(ApiErrorCode.AUTOMATION_BAD_REQUEST, "targetMonth는 yyyy-MM 형식이어야 합니다.");
		}

		@GetMapping("/geoserver/web")
		String noResourceFound() throws NoResourceFoundException {
			throw new NoResourceFoundException(HttpMethod.GET, "geoserver/web");
		}

		@GetMapping("/api/v1/test/not-found")
		String responseStatusNotFound() {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "missing");
		}

		@PostMapping("/api/v1/test/body-exception")
		String bodyException(@RequestBody Map<String, Object> body) {
			throw new RuntimeException("body boom");
		}
	}
}
