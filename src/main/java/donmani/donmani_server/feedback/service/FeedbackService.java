package donmani.donmani_server.feedback.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import donmani.donmani_server.expense.dto.ExpenseRequestDTO;
import donmani.donmani_server.expense.entity.Expense;
import donmani.donmani_server.expense.service.ExpenseService;
import donmani.donmani_server.feedback.dto.FeedbackOpenResponseDTO;
import donmani.donmani_server.feedback.entity.FeedbackTemplate;
import donmani.donmani_server.feedback.entity.FeedbackTemplateProvider;
import donmani.donmani_server.user.entity.User;
import donmani.donmani_server.user.service.UserService;
import donmani.donmani_server.feedback.entity.Feedback;
import donmani.donmani_server.feedback.repository.FeedbackRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeedbackService {
	private final UserService userService;
	private final ExpenseService expenseService;
	private final FeedbackRepository feedbackRepository;
	private final FeedbackTemplateProvider feedbackTemplateProvider;

	@Transactional
	public void addFeedback(ExpenseRequestDTO requestDTO) {
		LocalDateTime localDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

		// 유저 정보 확인
		String userKey = requestDTO.getUserKey();
		User user = userService.getUser(userKey);

		// 오늘 기록된 소비 확인
		// ver 2.0.0 이후로 기록된 소비만
		LocalDateTime createdAt = requestDTO.getRecords().get(0).getDate().atStartOfDay(); // 실제 생성일자말고 사용자가 기록하려는 일자
		LocalDateTime baseTime = LocalDateTime.of(2025, 5, 23, 0, 0);  // 2025-05-30 00:00

		if (createdAt.isEqual(baseTime) || createdAt.isAfter(baseTime)) {
			Expense expense = expenseService.getExpensesSubmitToday(user.getId(), createdAt);

			// GOOD, BAD 일 경우 feedback 생성
			if (expense.getFlag() != null) {
				Feedback feedback = Feedback
					.builder()
					.createdDate(localDateTime)
					.updateDate(localDateTime)
					.user(user)
					.expense(expense)
					.isOpened(false)
					.title(null)
					.content(null)
					.build();

				feedbackRepository.save(feedback);
			}
		}
	}

	public Boolean getNotOpenedFeedback(String userKey) {
		LocalDateTime localDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

		// 1. 유저 정보 확인
		User user = userService.getUser(userKey);

		// 2. 열지 않은 피드백 확인
		// List<Feedback> feedbacks = feedbackRepository.findFeedbackByUserIdAndCreatedAt(user.getId(), localDateTime);

		List<Feedback> feedbacks = feedbackRepository.findFeedbackByUserIdAndCreatedDate(user.getId(), localDateTime);

		if (feedbacks == null || feedbacks.isEmpty()) {
			return false;
		} else {
			for (Feedback feedback : feedbacks) {
				if (feedback.isOpened()) {
					return false;
				}

				break;
			}
		}

		return true;
	}

	public Boolean isFirstOpenedFeedback(String userKey) {
		// 1. 유저 정보 확인
		User user = userService.getUser(userKey);

		// 2. 첫 선물받기 여부 확인
		List<Feedback> feedbacks = feedbackRepository.findFeedbackByUserIdIsFirstOpen(user.getId());

		return feedbacks == null || feedbacks.isEmpty() ? true : false;
	}

	public FeedbackOpenResponseDTO openFeedback(String userKey) {
		LocalDateTime localDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

		User user = userService.getUser(userKey);

		// 1. 열지 않은 피드백 확인
		List<Feedback> feedbacks = feedbackRepository.findFeedbackByUserIdAndCreatedDate(user.getId(), localDateTime);

		Feedback notOpenedFeedback = new Feedback();

		for (Feedback feedback : feedbacks) {
			if (feedback.isOpened()) {
				throw new EntityNotFoundException("모든 피드백을 열었습니다.");
			}

			notOpenedFeedback = feedback;
			break;
		}

		// 2. 피드백에 해당하는 기록 확인
		// Expense expense = expenseService.getExpense(notOpenedFeedback.getId());

		// 3. 오늘 기록인지 어제 기록인지 확인 -> flagType
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
		Boolean isToday = notOpenedFeedback.getExpense().getCreatedAt().format(formatter).equals(localDateTime.format(formatter));

		// 3. 획득한 피드백의 템플릿 확인
		List<FeedbackTemplate> remainTemplates;

		List<String> usedTitles = feedbackRepository.findFeedbackByUserIdUsedTitle(user.getId());

		// 4. 해당 카테고리에 해당하는 템플릿 전체
		List<FeedbackTemplate> allTemplates = feedbackTemplateProvider.getTemplates(notOpenedFeedback.getExpense().getCategory());

		// 5. 이미 사용된 템플릿 제외
		if (usedTitles.isEmpty()) {
			remainTemplates = allTemplates;
		} else {
			List<FeedbackTemplate> notUsedTemplates = allTemplates
				.stream()
				.filter(feedbackTemplate -> !usedTitles
					.contains(feedbackTemplate.getTitle())
				)
				.collect(Collectors.toList());

			// 모두 사용했으면 해당 카테고리에 해당하는 템플릿 전체
			if (notUsedTemplates.isEmpty()) {
				notUsedTemplates = allTemplates;
			}

			remainTemplates = notUsedTemplates;
		}

		// 6. 이전 피드백과 겹치지 않게 피드백 update
		FeedbackTemplate remainTemplate = remainTemplates.get(new Random().nextInt(remainTemplates.size()));

		notOpenedFeedback.setTitle(remainTemplate.getTitle());
		notOpenedFeedback.setContent(remainTemplate.getContent());
		notOpenedFeedback.setOpened(true);
		notOpenedFeedback.setUpdateDate(localDateTime);

		feedbackRepository.save(notOpenedFeedback);

		FeedbackOpenResponseDTO response = new FeedbackOpenResponseDTO(
			notOpenedFeedback.getTitle(),
			notOpenedFeedback.getContent(),
			notOpenedFeedback.getUser().getName(),
			notOpenedFeedback.getExpense().getCategory(),
			isToday);

		return response;
	}
}


