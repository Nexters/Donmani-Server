package donmani.donmani_server.fcm.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import donmani.donmani_server.common.exception.ApiErrorCode;
import donmani.donmani_server.common.exception.ApiException;
import donmani.donmani_server.common.httpStatus.HttpStatusDTO;
import donmani.donmani_server.fcm.dto.FortuneAutomationResponse;
import donmani.donmani_server.fcm.service.FortuneAutomationAccessService;
import donmani.donmani_server.fcm.service.FortuneAutomationService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/v1/fortune/automation")
@RequiredArgsConstructor
public class FortuneAutomationController {

	private final FortuneAutomationService fortuneAutomationService;
	private final FortuneAutomationAccessService fortuneAutomationAccessService;

	@PostMapping("/generate/fortunes")
	public ResponseEntity<HttpStatusDTO<FortuneAutomationResponse>> generateFortunes(
		@RequestHeader(value = "X-Automation-Token", required = false) String automationToken,
		@RequestParam(required = false) String targetMonth,
		@RequestParam(required = false) String provider,
		@RequestParam(defaultValue = "false") boolean force
	) {
		assertAuthorized(automationToken);

		try {
			FortuneAutomationResponse response = fortuneAutomationService.generateFortunes(targetMonth, force, provider);
			return ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.OK.value(), "성공", response));
		} catch (IllegalArgumentException | IllegalStateException e) {
			throw ApiException.of(ApiErrorCode.AUTOMATION_BAD_REQUEST, e.getMessage(), e);
		}
	}

	@PostMapping("/webhook/fortunes")
	public ResponseEntity<HttpStatusDTO<FortuneAutomationResponse>> sendFortuneWebhook(
		@RequestHeader(value = "X-Automation-Token", required = false) String automationToken,
		@RequestParam String targetMonth
	) {
		assertAuthorized(automationToken);

		try {
			FortuneAutomationResponse response = fortuneAutomationService.sendFortuneReviewWebhook(targetMonth);
			return ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.OK.value(), "성공", response));
		} catch (IllegalArgumentException | IllegalStateException e) {
			throw ApiException.of(ApiErrorCode.AUTOMATION_BAD_REQUEST, e.getMessage(), e);
		}
	}

	@PostMapping("/webhook/images")
	public ResponseEntity<HttpStatusDTO<FortuneAutomationResponse>> sendFortuneWithImageWebhook(
		@RequestHeader(value = "X-Automation-Token", required = false) String automationToken,
		@RequestParam String targetMonth
	) {
		assertAuthorized(automationToken);

		try {
			FortuneAutomationResponse response = fortuneAutomationService.sendFortuneWithImageWebhook(targetMonth);
			return ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.OK.value(), "성공", response));
		} catch (IllegalArgumentException | IllegalStateException e) {
			throw ApiException.of(ApiErrorCode.AUTOMATION_BAD_REQUEST, e.getMessage(), e);
		}
	}

	@PostMapping("/approve")
	public ResponseEntity<HttpStatusDTO<FortuneAutomationResponse>> approve(
		@RequestHeader(value = "X-Automation-Token", required = false) String automationToken,
		@RequestParam String targetMonth
	) {
		assertAuthorized(automationToken);

		try {
			FortuneAutomationResponse response = fortuneAutomationService.approve(targetMonth);
			return ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.OK.value(), "성공", response));
		} catch (IllegalArgumentException | IllegalStateException e) {
			throw ApiException.of(ApiErrorCode.AUTOMATION_BAD_REQUEST, e.getMessage(), e);
		}
	}

	@PostMapping("/generate/images")
	public ResponseEntity<HttpStatusDTO<FortuneAutomationResponse>> generateImages(
		@RequestHeader(value = "X-Automation-Token", required = false) String automationToken,
		@RequestParam(required = false) String targetMonth,
		@RequestParam(required = false) String targetDate,
		@RequestParam(required = false) String provider
	) {
		assertAuthorized(automationToken);

		try {
			FortuneAutomationResponse response = fortuneAutomationService.generateImages(targetMonth, targetDate, provider);
			return ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.OK.value(), "성공", response));
		} catch (IllegalArgumentException | IllegalStateException e) {
			throw ApiException.of(ApiErrorCode.AUTOMATION_BAD_REQUEST, e.getMessage(), e);
		}
	}

	@GetMapping
	public ResponseEntity<HttpStatusDTO<FortuneAutomationResponse>> getStatus(
		@RequestHeader(value = "X-Automation-Token", required = false) String automationToken,
		@RequestParam String targetMonth
	) {
		assertAuthorized(automationToken);

		try {
			FortuneAutomationResponse response = fortuneAutomationService.getStatus(targetMonth);
			return ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.OK.value(), "성공", response));
		} catch (IllegalArgumentException e) {
			throw ApiException.of(ApiErrorCode.AUTOMATION_BAD_REQUEST, e.getMessage(), e);
		}
	}

	private void assertAuthorized(String automationToken) {
		if (!fortuneAutomationAccessService.isAuthorized(automationToken)) {
			throw ApiException.of(ApiErrorCode.AUTOMATION_UNAUTHORIZED);
		}
	}
}
