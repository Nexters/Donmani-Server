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
import org.springframework.web.bind.annotation.GetMapping;

import donmani.donmani_server.common.httpStatus.HttpStatusDTO;
import donmani.donmani_server.fcm.dto.FortuneHistoryResponseV1;
import donmani.donmani_server.fcm.service.FCMService;
import donmani.donmani_server.fcm.service.FortuneService;

@ExtendWith(MockitoExtension.class)
class FCMControllerTest {
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	@Mock
	private FCMService fcmService;

	@Mock
	private FortuneService fortuneService;

	@Test
	void getDailyFortuneUsesFortunesPath() throws NoSuchMethodException {
		GetMapping getMapping = FCMController.class
			.getMethod("getDailyFortuneV1", String.class)
			.getAnnotation(GetMapping.class);

		assertThat(Arrays.asList(getMapping.value())).containsExactly("/fortunes/{userKey}");
	}

	@Test
	void getFortuneHistoriesUsesListPath() throws NoSuchMethodException {
		GetMapping getMapping = FCMController.class
			.getMethod("getFortuneHistoriesV1", String.class, String.class, String.class)
			.getAnnotation(GetMapping.class);

		assertThat(Arrays.asList(getMapping.value())).containsExactly("/fortunes/list/{userKey}");
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
	void getFortuneHistoriesReturnsBadRequestEnvelopeWhenDateFormatIsInvalid() {
		FCMController controller = new FCMController(fcmService, fortuneService);

		ResponseEntity<HttpStatusDTO<List<FortuneHistoryResponseV1>>> response =
			controller.getFortuneHistoriesV1("user-1234", "20260701", "2026-07-31");

		verify(fortuneService, never()).getFortuneHistories(
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any()
		);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(response.getBody().getResponseMessage()).isEqualTo("잘못된 요청");
		assertThat(response.getBody().getResponseData()).isNull();
	}

	@Test
	void getFortuneHistoriesReturnsBadRequestEnvelopeWhenStartDateIsAfterEndDate() {
		FCMController controller = new FCMController(fcmService, fortuneService);

		ResponseEntity<HttpStatusDTO<List<FortuneHistoryResponseV1>>> response =
			controller.getFortuneHistoriesV1("user-1234", "2026-07-31", "2026-07-01");

		verify(fortuneService, never()).getFortuneHistories(
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any()
		);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(response.getBody().getResponseMessage()).isEqualTo("잘못된 요청");
		assertThat(response.getBody().getResponseData()).isNull();
	}

	@Test
	void getFortuneHistoriesReturnsBadRequestEnvelopeWhenOnlyStartDateIsProvided() {
		FCMController controller = new FCMController(fcmService, fortuneService);

		ResponseEntity<HttpStatusDTO<List<FortuneHistoryResponseV1>>> response =
			controller.getFortuneHistoriesV1("user-1234", "2026-07-01", null);

		verify(fortuneService, never()).getFortuneHistories(
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any()
		);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(response.getBody().getResponseMessage()).isEqualTo("잘못된 요청");
		assertThat(response.getBody().getResponseData()).isNull();
	}

	@Test
	void getFortuneHistoriesReturnsBadRequestEnvelopeWhenOnlyEndDateIsProvided() {
		FCMController controller = new FCMController(fcmService, fortuneService);

		ResponseEntity<HttpStatusDTO<List<FortuneHistoryResponseV1>>> response =
			controller.getFortuneHistoriesV1("user-1234", null, "2026-07-31");

		verify(fortuneService, never()).getFortuneHistories(
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any()
		);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(response.getBody().getResponseMessage()).isEqualTo("잘못된 요청");
		assertThat(response.getBody().getResponseData()).isNull();
	}

	private FCMController controllerOn(LocalDate today) {
		return new FCMController(
			fcmService,
			fortuneService,
			Clock.fixed(today.atStartOfDay(KST).toInstant(), KST)
		);
	}
}
