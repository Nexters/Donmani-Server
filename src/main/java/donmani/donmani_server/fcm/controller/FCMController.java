package donmani.donmani_server.fcm.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import donmani.donmani_server.common.httpStatus.HttpStatusDTO;
import donmani.donmani_server.fcm.dto.FortuneRequestV1;
import donmani.donmani_server.fcm.dto.FortuneResponseV1;
import donmani.donmani_server.fcm.service.FCMService;
import donmani.donmani_server.fcm.service.FortuneService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/v1")
@RequiredArgsConstructor
public class FCMController {

	private final FCMService fcmService;
	private final FortuneService fortuneService;

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
			LocalDate targetDate = LocalDate.now(ZoneId.of("Asia/Seoul"));
			FortuneResponseV1 fortuneResponseV1 = fortuneService.getDailyFortune(userKey, targetDate);

			return ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.OK.value(), "성공", fortuneResponseV1));
		} catch (EntityNotFoundException e) {
			return ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.NOT_FOUND.value(), "운세 정보 없음", null));
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
}
