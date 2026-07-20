package donmani.donmani_server.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.HandlerMapping;

import reactor.core.publisher.Mono;

class ExceptionWebhookServiceTest {

	@Test
	void notifyOnceDoesNothingWhenWebhookUrlIsMissing() {
		AtomicInteger requestCount = new AtomicInteger();
		ExceptionWebhookService service = newService(requestCount, false);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");

		service.notifyOnce(request, HttpStatus.INTERNAL_SERVER_ERROR.value(), new RuntimeException("boom"), "failed");

		assertThat(requestCount).hasValue(0);
	}

	@Test
	void buildPayloadContainsRequestExceptionAndProfile() {
		ExceptionWebhookService service = newService(new AtomicInteger(), true);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/1");
		request.setQueryString("token=abc&month=2026-07&secretKey=hidden");
		request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/v1/users/{userKey}");

		Map<String, Object> payload = service.buildPayload(
			request,
			HttpStatus.INTERNAL_SERVER_ERROR.value(),
			new IllegalStateException("broken"),
			"server failed"
		);

		String content = (String)payload.get("content");
		assertThat(content).contains("500");
		assertThat(content).contains("test");
		assertThat(content).contains("GET /api/v1/users/{userKey}?token=***&month=2026-07&secretKey=***");
		assertThat(content).contains("server failed");
		assertThat(content).contains("java.lang.IllegalStateException: broken");
		assertThat(content).contains("**BODY**");
		assertThat(content).doesNotContain("stackTop");
		assertThat(content).doesNotContain("token=abc");
		assertThat(content).doesNotContain("secretKey=hidden");
	}

	@Test
	void buildPayloadIncludesRedactedJsonBody() throws Exception {
		ExceptionWebhookService service = newService(new AtomicInteger(), true);
		ContentCachingRequestWrapper request = cachedRequest(
			"POST",
			"/api/v1/user/update",
			MediaType.APPLICATION_JSON_VALUE,
			"""
				{"userKey":"user-1","password":"pw","apiKey":"key-1","nested":{"token":"token-1","name":"donmani"}}
				"""
		);

		Map<String, Object> payload = service.buildPayload(
			request,
			HttpStatus.INTERNAL_SERVER_ERROR.value(),
			new IllegalStateException("broken"),
			"server failed"
		);

		String content = (String)payload.get("content");
		assertThat(content).contains("\"userKey\" : \"user-1\"");
		assertThat(content).contains("\"password\" : \"***\"");
		assertThat(content).contains("\"apiKey\" : \"***\"");
		assertThat(content).contains("\"token\" : \"***\"");
		assertThat(content).contains("\"name\" : \"donmani\"");
		assertThat(content).doesNotContain("pw");
		assertThat(content).doesNotContain("key-1");
		assertThat(content).doesNotContain("token-1");
		assertThat(content.indexOf("**REQUEST**")).isLessThan(content.indexOf("**MESSAGE**"));
		assertThat(content.indexOf("**MESSAGE**")).isLessThan(content.indexOf("**BODY**"));
	}

	@Test
	void buildPayloadOmitsBinaryBody() throws Exception {
		ExceptionWebhookService service = newService(new AtomicInteger(), true);
		ContentCachingRequestWrapper request = cachedRequest(
			"POST",
			"/api/v1/upload",
			MediaType.APPLICATION_OCTET_STREAM_VALUE,
			"raw-binary-content"
		);

		Map<String, Object> payload = service.buildPayload(
			request,
			HttpStatus.INTERNAL_SERVER_ERROR.value(),
			new IllegalStateException("broken"),
			"server failed"
		);

		String content = (String)payload.get("content");
		assertThat(content).contains("[binary body omitted]");
		assertThat(content).doesNotContain("raw-binary-content");
	}

	@Test
	void notifyOnceSendsMultipartWithStackTraceFileWhenExceptionExists() {
		AtomicReference<ClientRequest> capturedRequest = new AtomicReference<>();
		ExceptionWebhookService service = newService(capturedRequest);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");
		RuntimeException exception = new RuntimeException("boom");

		service.notifyOnce(request, HttpStatus.INTERNAL_SERVER_ERROR.value(), exception, "failed");

		MockClientHttpRequest renderedRequest = render(capturedRequest.get());
		String body = renderedRequest.getBodyAsString().block();
		assertThat(renderedRequest.getHeaders().getContentType().toString()).startsWith("multipart/form-data");
		assertThat(body).contains("name=\"payload_json\"");
		assertThat(body).contains("Donmani API EXCEPTION");
		assertThat(body).contains("**BODY**");
		assertThat(body).doesNotContain("stackTop");
		assertThat(body).contains("name=\"files[0]\"");
		assertThat(body).contains("filename=\"exception-stacktrace.log\"");
		assertThat(body).contains("java.lang.RuntimeException: boom");
		assertThat(body).contains("notifyOnceSendsMultipartWithStackTraceFileWhenExceptionExists");
		assertThat(body.indexOf("name=\"payload_json\"")).isLessThan(body.indexOf("name=\"files[0]\""));
	}

	@Test
	void notifyOnceSendsJsonWithoutFileWhenExceptionIsMissing() {
		AtomicReference<ClientRequest> capturedRequest = new AtomicReference<>();
		ExceptionWebhookService service = newService(capturedRequest);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");

		service.notifyOnce(request, HttpStatus.INTERNAL_SERVER_ERROR.value(), null, "body failed");

		MockClientHttpRequest renderedRequest = render(capturedRequest.get());
		String body = renderedRequest.getBodyAsString().block();
		assertThat(renderedRequest.getHeaders().getContentType()).isEqualTo(org.springframework.http.MediaType.APPLICATION_JSON);
		assertThat(body).contains("Donmani API EXCEPTION");
		assertThat(body).contains("body failed");
		assertThat(body).doesNotContain("files[0]");
		assertThat(body).doesNotContain("exception-stacktrace.log");
	}

	@Test
	void notifyOnceDoesNotPropagateWebhookFailure() {
		WebClient webClient = WebClient.builder()
			.exchangeFunction(request -> Mono.error(new IllegalStateException("discord down")))
			.build();
		ExceptionWebhookService service = new ExceptionWebhookService(webClient, new MockEnvironment());
		ReflectionTestUtils.setField(service, "webhookUrl", "https://discord.test/webhook");
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");

		assertThatCode(() -> service.notifyOnce(
			request,
			HttpStatus.INTERNAL_SERVER_ERROR.value(),
			new RuntimeException("boom"),
			"failed"
		)).doesNotThrowAnyException();
	}

	@Test
	void notifyOnceSendsOnlyOnceForSameRequest() {
		AtomicInteger requestCount = new AtomicInteger();
		ExceptionWebhookService service = newService(requestCount, true);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");

		service.notifyOnce(request, HttpStatus.INTERNAL_SERVER_ERROR.value(), new RuntimeException("boom"), "failed");
		service.notifyOnce(request, HttpStatus.INTERNAL_SERVER_ERROR.value(), null, null);

		assertThat(requestCount).hasValue(1);
	}

	private ExceptionWebhookService newService(
		AtomicInteger requestCount,
		boolean configured
	) {
		WebClient webClient = WebClient.builder()
			.exchangeFunction(request -> {
				requestCount.incrementAndGet();
				return Mono.just(ClientResponse.create(HttpStatus.NO_CONTENT).build());
			})
			.build();
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("test");
		ExceptionWebhookService service = new ExceptionWebhookService(webClient, environment);
		if (configured) {
			ReflectionTestUtils.setField(service, "webhookUrl", "https://discord.test/webhook");
		}
		return service;
	}

	private ExceptionWebhookService newService(AtomicReference<ClientRequest> capturedRequest) {
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

	private ContentCachingRequestWrapper cachedRequest(
		String method,
		String path,
		String contentType,
		String body
	) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(method, path);
		request.setContentType(contentType);
		request.setContent(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
		ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(request);
		wrapper.getInputStream().readAllBytes();
		return wrapper;
	}
}
