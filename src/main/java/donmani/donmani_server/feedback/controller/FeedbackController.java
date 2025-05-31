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
			// 1. 아직 열지 않은 피드백이 하나라도 있으면 true, 모든 피드백을 열었으면 false
			//  - 최근 생성된 피드백부터 내림차순으로 정렬
			Boolean isNotOpened = feedbackService.isNotOpenedFeedback(userKey);

			// 2. 피드백을 처음 여는 상황이면 true, 이미 열은 피드백이 하나라도 있으면 false
			//  - 최근 생성된 피드백부터 내림차순으로 정렬
			Boolean isFirstOpened = feedbackService.isFirstOpenedFeedback(userKey);

			// 3. ver 2.0.0 이후로 기록된 소비 확인
			Integer totalCount = expenseService.getTotalExpensesCount(userKey);

			FeedbackNotOpenedResponseDTO feedbackNotOpenedResponseDTO = new FeedbackNotOpenedResponseDTO(isNotOpened, isFirstOpened, totalCount);

			// 1. 피드백 조회 성공 -> 200
			return ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.OK.value(), "성공", feedbackNotOpenedResponseDTO));
		} catch (Exception e) {
			// 2. 피드백 조회 실패 -> 500
			return ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.INTERNAL_SERVER_ERROR.value(), "모든 피드백을 열었습니다.", null));
		}
	}

	@GetMapping("api/v1/feedback/content/{userKey}")
	public ResponseEntity<HttpStatusDTO<FeedbackOpenResponseDTO>> getFeedbackContentV1(@Valid @PathVariable String userKey) {
		try {
			FeedbackOpenResponseDTO response = feedbackService.getNotOpenedFeedbackContent(userKey);

			// 1. 피드백 열기 성공 -> 201
			return ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.CREATED.value(), "성공", response));
		} catch (Exception e) {
			// 2. 피드백 열기 실패 -> 500
			return ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.INTERNAL_SERVER_ERROR.value(), "모든 피드백을 열었습니다.", null));
		}
	}
}
