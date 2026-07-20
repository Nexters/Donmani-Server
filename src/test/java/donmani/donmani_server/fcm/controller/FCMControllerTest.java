package donmani.donmani_server.fcm.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.ZoneId;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import donmani.donmani_server.common.exception.ExceptionWebhookService;
import donmani.donmani_server.common.exception.FailureResponseBodyAdvice;
import donmani.donmani_server.common.exception.GlobalExceptionHandler;
import donmani.donmani_server.common.httpStatus.HttpStatusDTO;
import donmani.donmani_server.fcm.dto.FortuneHistoryResponseV1;
import donmani.donmani_server.fcm.service.FCMService;
import donmani.donmani_server.fcm.service.FortuneService;
import reactor.core.publisher.Mono;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FCMControllerTest {
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	@Mock
	private FCMService fcmService;

	@Mock
	private FortuneService fortuneService;

	@Test
	void getDailyFortuneUsesFortunePath() throws NoSuchMethodException {
		GetMapping getMapping = FCMController.class
			.getMethod("getDailyFortuneV1", String.class)
			.getAnnotation(GetMapping.class);

		assertThat(Arrays.asList(getMapping.value())).containsExactly("/fortune/{userKey}");
	}

	@Test
	void getFortuneHistoriesUsesListPath() throws NoSuchMethodException {
		GetMapping getMapping = FCMController.class
			.getMethod("getFortuneHistoriesV1", String.class, String.class, String.class)
			.getAnnotation(GetMapping.class);

		assertThat(Arrays.asList(getMapping.value())).containsExactly("/fortune/list/{userKey}");
	}

	@Test
	void getFortuneHistoriesParsesIsoLocalDates() {
		FCMController controller = new FCMController(fcmService, fortuneService);
		List<FortuneHistoryResponseV1> histories = List.of(
			FortuneHistoryResponseV1.builder()
				.targetDate(LocalDate.of(2026, 7, 1))
				.imageUrl("image-url")
				.subtitle("subtitle")
				.content("content")
				.item("item")
				.build()
		);
		when(fortuneService.getFortuneHistories(
			"user-1234",
			LocalDate.of(2026, 7, 1),
			LocalDate.of(2026, 7, 31)
		)).thenReturn(histories);

		ResponseEntity<HttpStatusDTO<List<FortuneHistoryResponseV1>>> response =
			controller.getFortuneHistoriesV1("user-1234", "2026-07-01", "2026-07-31");

		ArgumentCaptor<LocalDate> startDate = ArgumentCaptor.forClass(LocalDate.class);
		ArgumentCaptor<LocalDate> endDate = ArgumentCaptor.forClass(LocalDate.class);
		verify(fortuneService).getFortuneHistories(
			org.mockito.ArgumentMatchers.eq("user-1234"),
			startDate.capture(),
			endDate.capture()
		);
		assertThat(startDate.getValue()).isEqualTo(LocalDate.of(2026, 7, 1));
		assertThat(endDate.getValue()).isEqualTo(LocalDate.of(2026, 7, 31));
		assertThat(response.getBody().getStatusCode()).isEqualTo(HttpStatus.OK.value());
		assertThat(response.getBody().getResponseMessage()).isEqualTo("성공");
		assertThat(response.getBody().getResponseData()).isEqualTo(histories);
	}

	@Test
	void getFortuneHistoriesDefaultsToMondayThroughTodayWhenDatesAreMissingOnMonday() {
		FCMController controller = controllerOn(LocalDate.of(2026, 7, 6));
		when(fortuneService.getFortuneHistories(
			"user-1234",
			LocalDate.of(2026, 7, 6),
			LocalDate.of(2026, 7, 6)
		)).thenReturn(List.of());

		ResponseEntity<HttpStatusDTO<List<FortuneHistoryResponseV1>>> response =
			controller.getFortuneHistoriesV1("user-1234", null, null);

		ArgumentCaptor<LocalDate> startDate = ArgumentCaptor.forClass(LocalDate.class);
		ArgumentCaptor<LocalDate> endDate = ArgumentCaptor.forClass(LocalDate.class);
		verify(fortuneService).getFortuneHistories(
			org.mockito.ArgumentMatchers.eq("user-1234"),
			startDate.capture(),
			endDate.capture()
		);
		assertThat(startDate.getValue()).isEqualTo(LocalDate.of(2026, 7, 6));
		assertThat(endDate.getValue()).isEqualTo(LocalDate.of(2026, 7, 6));
		assertThat(response.getBody().getStatusCode()).isEqualTo(HttpStatus.OK.value());
		assertThat(response.getBody().getResponseMessage()).isEqualTo("성공");
	}

	@Test
	void getFortuneHistoriesDefaultsToMondayThroughTodayWhenDatesAreMissingOnSunday() {
		FCMController controller = controllerOn(LocalDate.of(2026, 7, 12));
		when(fortuneService.getFortuneHistories(
			"user-1234",
			LocalDate.of(2026, 7, 6),
			LocalDate.of(2026, 7, 12)
		)).thenReturn(List.of());

		ResponseEntity<HttpStatusDTO<List<FortuneHistoryResponseV1>>> response =
			controller.getFortuneHistoriesV1("user-1234", null, null);

		ArgumentCaptor<LocalDate> startDate = ArgumentCaptor.forClass(LocalDate.class);
		ArgumentCaptor<LocalDate> endDate = ArgumentCaptor.forClass(LocalDate.class);
		verify(fortuneService).getFortuneHistories(
			org.mockito.ArgumentMatchers.eq("user-1234"),
			startDate.capture(),
			endDate.capture()
		);
		assertThat(startDate.getValue()).isEqualTo(LocalDate.of(2026, 7, 6));
		assertThat(endDate.getValue()).isEqualTo(LocalDate.of(2026, 7, 12));
		assertThat(response.getBody().getStatusCode()).isEqualTo(HttpStatus.OK.value());
		assertThat(response.getBody().getResponseMessage()).isEqualTo("성공");
	}

	@Test
	void getFortuneHistoriesReturnsBadRequestEnvelopeWhenDateFormatIsInvalid() throws Exception {
		FCMController controller = new FCMController(fcmService, fortuneService);
		MockMvc mockMvc = mockMvc(controller);

		mockMvc.perform(get("/api/v1/fortune/list/user-1234")
				.param("startDate", "20260701")
				.param("endDate", "2026-07-31"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.statusCode").value(HttpStatus.BAD_REQUEST.value()))
			.andExpect(jsonPath("$.responseMessage").value("잘못된 요청"))
			.andExpect(jsonPath("$.responseData").doesNotExist());

		verify(fortuneService, never()).getFortuneHistories(
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any()
		);
	}

	@Test
	void getFortuneHistoriesReturnsBadRequestEnvelopeWhenStartDateIsAfterEndDate() throws Exception {
		FCMController controller = new FCMController(fcmService, fortuneService);
		MockMvc mockMvc = mockMvc(controller);

		mockMvc.perform(get("/api/v1/fortune/list/user-1234")
				.param("startDate", "2026-07-31")
				.param("endDate", "2026-07-01"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.statusCode").value(HttpStatus.BAD_REQUEST.value()))
			.andExpect(jsonPath("$.responseMessage").value("잘못된 요청"))
			.andExpect(jsonPath("$.responseData").doesNotExist());

		verify(fortuneService, never()).getFortuneHistories(
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any()
		);
	}

	@Test
	void getFortuneHistoriesReturnsBadRequestEnvelopeWhenOnlyStartDateIsProvided() throws Exception {
		FCMController controller = new FCMController(fcmService, fortuneService);
		MockMvc mockMvc = mockMvc(controller);

		mockMvc.perform(get("/api/v1/fortune/list/user-1234")
				.param("startDate", "2026-07-01"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.statusCode").value(HttpStatus.BAD_REQUEST.value()))
			.andExpect(jsonPath("$.responseMessage").value("잘못된 요청"))
			.andExpect(jsonPath("$.responseData").doesNotExist());

		verify(fortuneService, never()).getFortuneHistories(
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any()
		);
	}

	@Test
	void getFortuneHistoriesReturnsBadRequestEnvelopeWhenOnlyEndDateIsProvided() throws Exception {
		FCMController controller = new FCMController(fcmService, fortuneService);
		MockMvc mockMvc = mockMvc(controller);

		mockMvc.perform(get("/api/v1/fortune/list/user-1234")
				.param("endDate", "2026-07-31"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.statusCode").value(HttpStatus.BAD_REQUEST.value()))
			.andExpect(jsonPath("$.responseMessage").value("잘못된 요청"))
			.andExpect(jsonPath("$.responseData").doesNotExist());

		verify(fortuneService, never()).getFortuneHistories(
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any()
		);
	}

	private MockMvc mockMvc(FCMController controller) {
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

	private FCMController controllerOn(LocalDate today) {
		return new FCMController(
			fcmService,
			fortuneService,
			Clock.fixed(today.atStartOfDay(KST).toInstant(), KST)
		);
	}
}
