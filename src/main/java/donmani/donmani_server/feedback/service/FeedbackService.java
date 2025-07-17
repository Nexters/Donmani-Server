package donmani.donmani_server.feedback.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import donmani.donmani_server.expense.entity.CategoryType;
import donmani.donmani_server.expense.repository.ExpenseRepository;
import donmani.donmani_server.reward.entity.UserItem;
import donmani.donmani_server.reward.repository.UserItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import donmani.donmani_server.expense.dto.ExpenseRequestDTO;
import donmani.donmani_server.expense.entity.Expense;
import donmani.donmani_server.feedback.dto.FeedbackOpenResponseDTO;
import donmani.donmani_server.feedback.provider.FeedbackTemplate;
import donmani.donmani_server.feedback.provider.FeedbackTemplateProvider;
import donmani.donmani_server.user.entity.User;
import donmani.donmani_server.user.service.UserService;
import donmani.donmani_server.feedback.entity.Feedback;
import donmani.donmani_server.feedback.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeedbackService {
	private final UserService userService;
	private final ExpenseRepository expenseRepository;
	private final FeedbackRepository feedbackRepository;
	private final FeedbackTemplateProvider feedbackTemplateProvider;
	private final UserItemRepository userItemRepository;

	@Transactional
	public void addFeedback(ExpenseRequestDTO requestDTO) {
		LocalDateTime localDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

		// 유저 정보 확인
		String userKey = requestDTO.getUserKey();
		User user = userService.getUser(userKey);

		// 오늘 기록된 소비 확인
		// ver 2.0.0 이후로 기록된 소비만
		LocalDateTime createdAt = requestDTO.getRecords().get(0).getDate().atStartOfDay(); // 실제 생성일자말고 사용자가 기록하려는 일자
		LocalDateTime baseTime = LocalDateTime.of(2025, 7, 18, 0, 0);  // 2025-07-18 00:00

		// 획득한 피드백 카드가 12개 이상 -> 피드백 카드를 생성하지 않음
		List<Feedback> feedbacks = feedbackRepository.findFeedbacksByUserId(user.getId());

		if (feedbacks.size() < 12 && (createdAt.isEqual(baseTime) || createdAt.isAfter(baseTime))) {
			Expense expense = expenseRepository.findExpenseByUserIdAndAndCreatedAt(user.getId(), createdAt);

			// GOOD, BAD 일 경우 feedback 생성
			// 무소비일때도 feedback 생성
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

	public Boolean isNotOpenedFeedback(String userKey) {
		// LocalDateTime localDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

		// 1. 유저 정보 확인
		User user = userService.getUser(userKey);

		// List<Feedback> feedbacks = feedbackRepository.findFeedbackByUserIdAndCreatedAt(user.getId(), localDateTime);

		// 2. 열지 않은 피드백 확인
		//  - 최근 생성된 피드백부터 내림차순으로 정렬
		List<Feedback> feedbacks = feedbackRepository.findFeedbackByUserIdOrderByCreatedDateDesc(user.getId());

		if (feedbacks == null || feedbacks.isEmpty() || feedbacks.get(0).isOpened()) {
			return false;
		}

		/* *
		 * BUG: 열지 않은 피드백 카드가 존재하더라도, 해당 로직으로 인해 false가 반환
		 *      불필요한 로직으로 판단되어 로직 제거
		 */
		// 3. 이미 12개를 모두 열었다면 isNotOpened를 false로
		// List<UserItem> acquiredItems = userItemRepository.findByUserOrderByAcquiredAtDesc(user);
		//
		// if(acquiredItems.size() == 12) {
		// 	return false;
		// }

		// 4. 그 외는 true로 처리
		return true;
	}

	public Boolean isFirstOpenedFeedback(String userKey) {
		// 1. 유저 정보 확인
		User user = userService.getUser(userKey);

		// 2. 첫 선물받기 여부 확인
		List<Feedback> feedbacks = feedbackRepository.findFeedbackByUserIdIsFirstOpen(user.getId());

		return feedbacks == null || feedbacks.isEmpty() ? true : false;
	}

	public void openFeedback(ExpenseRequestDTO request) {
		LocalDateTime localDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

		User user = userService.getUser(request.getUserKey());

		// 1. 열지 않은 피드백 확인
		//  - 최근 생성된 피드백부터 내림차순으로 정렬
		List<Feedback> feedbacks = feedbackRepository.findFeedbackByUserIdOrderByCreatedDateDesc(user.getId());

		if (!feedbacks.isEmpty() && !feedbacks.get(0).isOpened()) {
			Feedback notOpenedFeedback = feedbacks.get(0);

			// 2. 획득한 피드백의 템플릿 확인
			List<FeedbackTemplate> remainTemplates;

			List<String> usedTitles = feedbackRepository.findFeedbackByUserIdUsedTitle(user.getId());

			// 3. 해당 카테고리에 해당하는 템플릿 전체
			List<FeedbackTemplate> allTemplates = feedbackTemplateProvider.getTemplates(notOpenedFeedback.getExpense().getCategory() == null ? CategoryType.NONE : notOpenedFeedback.getExpense().getCategory());

			// 4. 이미 사용된 템플릿 제외
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

			// 5. 이전 피드백과 겹치지 않게 피드백 update
			FeedbackTemplate remainTemplate = remainTemplates.get(new Random().nextInt(remainTemplates.size()));

			notOpenedFeedback.setTitle(remainTemplate.getTitle());
			notOpenedFeedback.setContent(remainTemplate.getContent());
			notOpenedFeedback.setUpdateDate(localDateTime);

			feedbackRepository.save(notOpenedFeedback);
		}
	}

	@Transactional(readOnly = true)
	public FeedbackOpenResponseDTO getNotOpenedFeedbackContent(String userKey) {
		LocalDateTime localDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

		User user = userService.getUser(userKey);
		Feedback notOpenedFeedback = feedbackRepository.findFirstByUserAndIsOpenedFalseOrderByCreatedDateDesc(user).orElseThrow();

		Boolean isToday = notOpenedFeedback.getExpense().getCreatedAt().format(formatter).equals(localDateTime.format(formatter));
		FeedbackOpenResponseDTO response = new FeedbackOpenResponseDTO(
				notOpenedFeedback.getTitle(),
				notOpenedFeedback.getContent(),
				notOpenedFeedback.getUser().getName(),
				notOpenedFeedback.getExpense().getCategory() == null ? CategoryType.NONE : notOpenedFeedback.getExpense().getCategory(),
				isToday);

		return response;
	}
}


