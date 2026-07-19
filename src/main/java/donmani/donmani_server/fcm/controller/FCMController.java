package donmani.donmani_server.fcm.controller;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import donmani.donmani_server.common.httpStatus.HttpStatusDTO;
import donmani.donmani_server.fcm.dto.FortuneHistoryResponseV1;
import donmani.donmani_server.fcm.dto.FortuneRequestV1;
import donmani.donmani_server.fcm.dto.FortuneResponseV1;
import donmani.donmani_server.fcm.service.FCMService;
import donmani.donmani_server.fcm.service.FortuneService;
import jakarta.persistence.EntityNotFoundException;

@RestController
@RequestMapping("api/v1")
public class FCMController {
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final FCMService fcmService;
	private final FortuneService fortuneService;
	private final Clock clock;

	@Autowired
	public FCMController(
		FCMService fcmService,
		FortuneService fortuneService
	) {
		this(fcmService, fortuneService, Clock.system(KST));
	}

	FCMController(
		FCMService fcmService,
		FortuneService fortuneService,
		Clock clock
	) {
		this.fcmService = fcmService;
		this.fortuneService = fortuneService;
		this.clock = clock;
	}

	@PostMapping("/{userKey}/token")
	public ResponseEntity<String> saveOrUpdateToken(
		@PathVariable String userKey,
		@RequestBody String token
	) {
		fcmService.saveOrUpdateToken(userKey, token);
		return ResponseEntity.ok("SUCCESS"); // TODO : 응답 포맷팅 작업에서 수정 필요
	}

	@GetMapping("/fortune/{userKey}")
	public ResponseEntity<HttpStatusDTO<FortuneResponseV1>> getDailyFortuneV1(
		@PathVariable String userKey
	) {
		try {
			LocalDate targetDate = LocalDate.now(KST);
			FortuneResponseV1 fortuneResponseV1 = fortuneService.getDailyFortune(userKey, targetDate);

			return ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.OK.value(), "성공", fortuneResponseV1));
		} catch (EntityNotFoundException e) {
			return ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.NOT_FOUND.value(), "운세 정보 없음", null));
		}
	}

	@GetMapping("/fortune/list/{userKey}")
	public ResponseEntity<HttpStatusDTO<List<FortuneHistoryResponseV1>>> getFortuneHistoriesV1(
		@PathVariable String userKey,
		@RequestParam(required = false) String startDate,
		@RequestParam(required = false) String endDate
	) {
		try {
			DateRange dateRange = resolveDateRange(startDate, endDate);

			List<FortuneHistoryResponseV1> response = fortuneService.getFortuneHistories(
				userKey,
				dateRange.startDate(),
				dateRange.endDate()
			);

			return ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.OK.value(), "성공", response));
		} catch (DateTimeParseException | IllegalArgumentException e) {
			return ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.BAD_REQUEST.value(), "잘못된 요청", null));
		}
	}

	@PutMapping("/fortune/read")
	public ResponseEntity<HttpStatusDTO<Void>> markFortuneAsReadV1(
		@RequestBody FortuneRequestV1 request
	) {
		try {
			LocalDateTime localDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
			fortuneService.markFortuneAsRead(request.getUserKey(), request.getReadSource(), localDateTime);

			return ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.OK.value(), null, null));
		} catch (EntityNotFoundException e) {
			return ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.NOT_FOUND.value(), null, null));
		}
	}

	@PostMapping("/test/send-push/{userKey}/{userToken}")
	public void sendDailyPushTest(
		@PathVariable String userKey,
		@PathVariable String userToken
	) {
		fortuneService.sendDailyPushTest(userKey, userToken);
	}

	@PostMapping("/test/send-fortune/{userKey}/{userToken}")
	public void sendDailyFortunesTest(
		@PathVariable String userKey,
		@PathVariable String userToken
	) {
		fortuneService.sendDailyFortuneTest(userKey, userToken);
	}

	private LocalDate parseBasicDate(String date) {
		return LocalDate.parse(date.trim(), DATE_FORMATTER);
	}

	private DateRange resolveDateRange(
		String startDate,
		String endDate
	) {
		boolean hasStartDate = StringUtils.hasText(startDate);
		boolean hasEndDate = StringUtils.hasText(endDate);

		if (!hasStartDate && !hasEndDate) {
			LocalDate today = LocalDate.now(clock);
			return new DateRange(
				today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
				today
			);
		}

		if (hasStartDate != hasEndDate) {
			throw new IllegalArgumentException("조회 시작일과 종료일은 함께 입력해야 합니다.");
		}

		LocalDate parsedStartDate = parseBasicDate(startDate);
		LocalDate parsedEndDate = parseBasicDate(endDate);

		if (parsedStartDate.isAfter(parsedEndDate)) {
			throw new IllegalArgumentException("조회 시작일은 종료일보다 늦을 수 없습니다.");
		}

		return new DateRange(parsedStartDate, parsedEndDate);
	}

	private record DateRange(
		LocalDate startDate,
		LocalDate endDate
	) {
	}
}
