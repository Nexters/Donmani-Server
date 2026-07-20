package donmani.donmani_server.fcm.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import donmani.donmani_server.common.exception.ExceptionWebhookService;
import donmani.donmani_server.common.exception.FailureResponseBodyAdvice;
import donmani.donmani_server.common.exception.GlobalExceptionHandler;
import donmani.donmani_server.common.httpStatus.HttpStatusDTO;
import donmani.donmani_server.fcm.dto.FortuneAutomationResponse;
import donmani.donmani_server.fcm.service.FortuneAutomationAccessService;
import donmani.donmani_server.fcm.service.FortuneAutomationService;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class FortuneAutomationControllerTest {

	@Mock
	private FortuneAutomationService fortuneAutomationService;

	@Mock
	private FortuneAutomationAccessService fortuneAutomationAccessService;

	@Test
	void postMappingsUseOnlyCurrentAutomationPaths() {
		List<String> postMappings = Arrays.stream(FortuneAutomationController.class.getDeclaredMethods())
			.map(method -> method.getAnnotation(PostMapping.class))
			.filter(annotation -> annotation != null)
			.flatMap(annotation -> Arrays.stream(annotation.value()))
			.toList();

		assertThat(postMappings).containsExactlyInAnyOrder(
			"/generate/fortunes",
			"/webhook/fortunes",
			"/webhook/images",
			"/approve",
			"/generate/images"
		);
		assertThat(postMappings).doesNotContain(
			"/fortunes/generate",
			"/fortunes/webhook",
			"/fortunes/with-images/webhook",
			"/webhook",
			"/run",
			"/images/test",
			"/images/generate",
			"/images/webhook"
		);
	}

	@Test
	void generateFortunesUsesGenerateFortunesPath() throws NoSuchMethodException {
		PostMapping postMapping = postMapping("generateFortunes", String.class, String.class, String.class, boolean.class);

		assertThat(Arrays.asList(postMapping.value())).containsExactly("/generate/fortunes");
	}

	@Test
	void generateImagesUsesGenerateImagesPath() throws NoSuchMethodException {
		PostMapping postMapping = postMapping(
			"generateImages",
			String.class,
			String.class,
			String.class,
			String.class
		);

		assertThat(Arrays.asList(postMapping.value())).containsExactly("/generate/images");
	}

	@Test
	void sendFortuneWebhookUsesWebhookFortunesPath() throws NoSuchMethodException {
		PostMapping postMapping = postMapping("sendFortuneWebhook", String.class, String.class);

		assertThat(Arrays.asList(postMapping.value())).containsExactly("/webhook/fortunes");
	}

	@Test
	void sendFortuneWithImageWebhookUsesWebhookImagesPathAndService() {
		FortuneAutomationController controller = new FortuneAutomationController(
			fortuneAutomationService,
			fortuneAutomationAccessService
		);
		FortuneAutomationResponse serviceResponse = FortuneAutomationResponse.builder()
			.targetMonth("2026-08")
			.executed(true)
			.build();

		when(fortuneAutomationAccessService.isAuthorized("token")).thenReturn(true);
		when(fortuneAutomationService.sendFortuneWithImageWebhook("2026-08")).thenReturn(serviceResponse);

		ResponseEntity<HttpStatusDTO<FortuneAutomationResponse>> response =
			controller.sendFortuneWithImageWebhook("token", "2026-08");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().getStatusCode()).isEqualTo(HttpStatus.OK.value());
		assertThat(response.getBody().getResponseData()).isEqualTo(serviceResponse);
		verify(fortuneAutomationService).sendFortuneWithImageWebhook("2026-08");
	}

	@Test
	void generateFortunesReturnsUnauthorizedEnvelopeThroughGlobalHandler() throws Exception {
		FortuneAutomationController controller = new FortuneAutomationController(
			fortuneAutomationService,
			fortuneAutomationAccessService
		);
		MockMvc mockMvc = mockMvc(controller);
		when(fortuneAutomationAccessService.isAuthorized(null)).thenReturn(false);

		mockMvc.perform(post("/api/v1/fortune/automation/generate/fortunes"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.statusCode").value(HttpStatus.UNAUTHORIZED.value()))
			.andExpect(jsonPath("$.responseMessage").value("인증 실패"))
			.andExpect(jsonPath("$.responseData").doesNotExist());

		verify(fortuneAutomationService, never()).generateFortunes(any(), any(Boolean.class), any());
	}

	@Test
	void generateFortunesReturnsBadRequestEnvelopeThroughGlobalHandler() throws Exception {
		FortuneAutomationController controller = new FortuneAutomationController(
			fortuneAutomationService,
			fortuneAutomationAccessService
		);
		MockMvc mockMvc = mockMvc(controller);
		when(fortuneAutomationAccessService.isAuthorized("token")).thenReturn(true);
		when(fortuneAutomationService.generateFortunes("invalid", false, null))
			.thenThrow(new IllegalArgumentException("targetMonth는 yyyy-MM 형식이어야 합니다."));

		mockMvc.perform(post("/api/v1/fortune/automation/generate/fortunes")
				.header("X-Automation-Token", "token")
				.param("targetMonth", "invalid"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.statusCode").value(HttpStatus.BAD_REQUEST.value()))
			.andExpect(jsonPath("$.responseMessage").value("targetMonth는 yyyy-MM 형식이어야 합니다."))
			.andExpect(jsonPath("$.responseData").doesNotExist());
	}

	private PostMapping postMapping(
		String methodName,
		Class<?>... parameterTypes
	) throws NoSuchMethodException {
		Method method = FortuneAutomationController.class.getMethod(methodName, parameterTypes);
		return method.getAnnotation(PostMapping.class);
	}

	private MockMvc mockMvc(FortuneAutomationController controller) {
		ExceptionWebhookService exceptionWebhookService = new ExceptionWebhookService(
			WebClient.builder()
				.exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.NO_CONTENT).build()))
				.build(),
			new MockEnvironment()
		);
		return MockMvcBuilders.standaloneSetup(controller)
			.setControllerAdvice(
				new GlobalExceptionHandler(exceptionWebhookService),
				new FailureResponseBodyAdvice(exceptionWebhookService)
			)
			.build();
	}
}
