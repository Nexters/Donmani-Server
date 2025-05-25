package donmani.donmani_server.feedback.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import donmani.donmani_server.common.httpStatus.HttpStatusDTO;
import donmani.donmani_server.expense.service.ExpenseService;
import donmani.donmani_server.feedback.dto.FeedbackNotOpenedResponseDTO;
import donmani.donmani_server.feedback.dto.FeedbackOpenResponseDTO;
import donmani.donmani_server.feedback.entity.Feedback;
import donmani.donmani_server.feedback.service.FeedbackService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
public class FeedbackController {
	private final FeedbackService feedbackService;
	private final ExpenseService expenseService;

	@GetMapping("api/v1/feedback/{userKey}")
	public ResponseEntity<HttpStatusDTO<FeedbackNotOpenedResponseDTO>> getNotOpenedFeedbackV1(@Valid @PathVariable String userKey) {
		try {
			Boolean isNotOpened = feedbackService.getNotOpenedFeedback(userKey);
			Boolean isFirstOpened = feedbackService.isFirstOpenedFeedback(userKey);

			if (!isNotOpened && isFirstOpened) {
				isFirstOpened = false;
			}

			Integer totalCount = expenseService.getTotalExpensesCount(userKey);

			FeedbackNotOpenedResponseDTO feedbackNotOpenedResponseDTO = new FeedbackNotOpenedResponseDTO(isNotOpened, isFirstOpened, totalCount);

			// 1. 피드백 조회 성공 -> 200
			return ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.OK.value(), "성공", feedbackNotOpenedResponseDTO));
		} catch (Exception e) {
			// 2. 피드백 조회 실패 -> 500
			return ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.INTERNAL_SERVER_ERROR.value(), "모든 피드백을 열었습니다.", null));
		}
	}

	@PostMapping("api/v1/feedback/{userKey}")
	public ResponseEntity<HttpStatusDTO<FeedbackOpenResponseDTO>> openFeedbackV1(@Valid @PathVariable String userKey) {
		try {
			FeedbackOpenResponseDTO response = feedbackService.openFeedback(userKey);

			// 1. 피드백 열기 성공 -> 201
			return ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.CREATED.value(), "성공", response));
		} catch (Exception e) {
			// 2. 피드백 열기 실패 -> 500
			return ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.INTERNAL_SERVER_ERROR.value(), "모든 피드백을 열었습니다.", null));
		}
	}
}
