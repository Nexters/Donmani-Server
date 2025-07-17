package donmani.donmani_server.expense.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import donmani.donmani_server.expense.dto.*;
import donmani.donmani_server.expense.entity.CategoryType;
import donmani.donmani_server.expense.entity.FlagType;

import donmani.donmani_server.feedback.service.FeedbackService;
import donmani.donmani_server.reward.service.RewardService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import donmani.donmani_server.expense.entity.Expense;
import donmani.donmani_server.expense.repository.ExpenseRepository;
import donmani.donmani_server.user.entity.User;
import donmani.donmani_server.user.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExpenseService {

	private final ExpenseRepository expenseRepository;
	private final UserService userService;
	private final FeedbackService feedbackService;
	private final RewardService rewardService;

	private final static int SIZE = 20; // 전체 조회 페이지 사이즈 고정

	@Transactional
	public void addExpense(ExpenseRequestDTO request) {
		Long userId = userService.getUserIdByUserKey(request.getUserKey());
		LocalDateTime localDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

		List<Expense> expenses = request.getRecords().stream()
			.flatMap(record -> {
				List<ContentDTO> contents = record.getContents() != null
					? record.getContents()
					: Collections.emptyList();

				// 기록이 없을 때 (무지출)
				if (contents.isEmpty()) {
					return Stream.of(Expense.builder()
						.userId(userId)
						.createdAt(record.getDate().atStartOfDay())
						.createdDate(localDateTime)
						.updateDate(localDateTime)
						.build());
				}

				// 기록이 있을 때
				return contents.stream().map(content -> Expense.builder()
					.userId(userId)
					.createdAt(record.getDate().atStartOfDay())
					.createdDate(localDateTime)
					.updateDate(localDateTime)
					.flag(content.getFlag())
					.category(content.getCategory())
					.memo(content.getMemo())
					.build());
			})
			.collect(Collectors.toList());

		expenseRepository.saveAll(expenses);

		// 피드백 카드 받기 & 선물 받기
		feedbackService.addFeedback(request);
		feedbackService.openFeedback(request);
		rewardService.acquireRandomItems(request.getUserKey(), request.getRecords().get(0).getDate());
	}


	@Transactional
	public ExpenseResponseDTO getExpenses(String userKey, int year, int month, boolean sortedDesc) {
		Long userId = userService.getUserIdByUserKey(userKey);

		if (userId == null) {
			throw new EntityNotFoundException("유저 정보를 찾을 수 없습니다.");
		}

		// userId와 year, month로 직접 expense를 조회
		List<Expense> expenses = expenseRepository.findByUserIdAndMonth(userId, year, month);

		// 오픈하지 않은 선물 여부 확인
		boolean hasNotOpenedRewards = rewardService.hasNotOpenedRewards(userKey);

		// 현재 기록 개수 확인
		Integer totalExpensesCount = getTotalExpensesCount(userKey);

		if(expenses.isEmpty()) {
			return ExpenseResponseDTO.builder()
					.userKey(userKey)
					.records(null)
					.saveItems(rewardService.getSavedItem(userKey, year, month))
					.hasNotOpenedRewards(hasNotOpenedRewards)
					.totalExpensesCount(totalExpensesCount)
					.build();
		}

		return ExpenseResponseDTO.builder()
			.userKey(userKey)
			.records(expenseToDto(expenses, sortedDesc))
			.saveItems(rewardService.getSavedItem(userKey, year, month))
			.hasNotOpenedRewards(hasNotOpenedRewards)
			.totalExpensesCount(totalExpensesCount)
			.build();
	}

	private List<RecordDTO> expenseToDto(List<Expense> expenses, boolean sortedDesc) {
		Map<LocalDate, List<ContentDTO>> groupedContents = expenses.stream()
				.collect(Collectors.groupingBy(
						expense -> expense.getCreatedAt().toLocalDate(), // 날짜별 그룹핑 Map 형태로 담아줌
						Collectors.mapping(exp -> {
							if (exp.getCategory() == null && exp.getFlag() == null && exp.getMemo() == null) {
								return null;
							}
							return ContentDTO.builder()
									.flag(exp.getFlag())
									.category(exp.getCategory())
									.memo(exp.getMemo())
									.build();
						}, Collectors.toList())
				));

		return groupedContents.entrySet().stream()
				.map(entry -> RecordDTO.of(entry.getKey(), entry.getValue())
				)
				.sorted(sortedDesc ? Comparator.comparing(RecordDTO::getDate).reversed() : Comparator.comparing(RecordDTO::getDate))
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public ExpenseSummaryDTO getYearlyExpenseSummary(String userKey, int year) {
		Long userId = userService.getUserIdByUserKey(userKey);
		List<Expense> expenses = expenseRepository.findByUserId(userId);

		// 일 기준으로 그룹핑
		Set<LocalDate> dailyCounts = expenses.stream()
			.filter(exp -> exp.getCreatedAt().getYear() == year)
			.map(exp -> exp.getCreatedAt().toLocalDate())
			.collect(Collectors.toSet());

		// 월 기준으로 다시 그룹핑
		Map<Integer, Long> monthlyCounts = dailyCounts.stream()
			.collect(Collectors.groupingBy(
				LocalDate::getMonthValue,
				Collectors.counting()
			));

		// 현재 월 계산
		int currentMonth = (year == LocalDate.now(ZoneId.of("Asia/Seoul")).getYear()) ?
				LocalDate.now(ZoneId.of("Asia/Seoul")).getMonthValue() : 12;

		// RecordInfoDTO 변환 : 1월부터 현재 월까지 반복
		Map<Integer, RecordInfoDTO> monthlyRecords = IntStream.rangeClosed(1, currentMonth)
				.boxed()
				.collect(Collectors.toMap(
						month -> month,
						month -> new RecordInfoDTO(
								monthlyCounts.getOrDefault(month, 0L),
								YearMonth.of(year, month).lengthOfMonth()
						)
				));

		return ExpenseSummaryDTO.builder()
				.year(year)
				.monthlyRecords(monthlyRecords)
				.build();
	}

	@Transactional(readOnly = true)
	public ExpenseStatisticsDTO getMonthlyExpenseStatistics(String userKey, int year, int month) {
		Long userId = userService.getUserIdByUserKey(userKey);
		List<Expense> expenses = expenseRepository.findByUserIdAndMonth(userId, year, month);

		int goodCount = (int) expenses.stream().filter(e -> e.getFlag() == FlagType.GOOD).count();
		int badCount = (int) expenses.stream().filter(e -> e.getFlag() == FlagType.BAD).count();
		boolean hasRecords = !expenses.isEmpty();

		return ExpenseStatisticsDTO.builder()
				.year(year)
				.month(month)
				.goodCount(goodCount)
				.badCount(badCount)
				.hasRecords(hasRecords)
				.records(expenses.stream().map(e -> RecordDTO.of(e.getCreatedAt().toLocalDate(), List.of(ContentDTO.builder()
						.flag(e.getFlag())
						.category(e.getCategory())
						.memo(e.getMemo())
						.build()))).collect(Collectors.toList()))
				.build();
	}

	@Transactional(readOnly = true)
	public CategoryStatisticsDTO getCategoryStatistics(String userKey, int year, int month) {
		Long userId = userService.getUserIdByUserKey(userKey);
		List<Expense> expenses = expenseRepository.findByUserIdAndMonth(userId, year, month);

		Map<CategoryType, Integer> categoryCounts = Arrays.stream(CategoryType.values())
				.collect(Collectors.toMap(category -> category, category -> 0));

		expenses.forEach(expense -> {
			if (expense.getCategory() != null) {
				categoryCounts.put(
						expense.getCategory(), categoryCounts.get(expense.getCategory()) + 1);
			}
		});


		return CategoryStatisticsDTO.builder()
				.year(year)
				.month(month)
				.categoryCounts(categoryCounts)
				.build();
	}

	public Integer getTotalExpensesCount(String userKey) {
		// 1. 유저 정보 확인
		User user = userService.getUser(userKey);

		// ver 2.0.0 이후로 기록된 소비만
		List<LocalDateTime> createdAts = expenseRepository.findTotalExpensesCount(user.getId());

		return createdAts == null || createdAts.isEmpty() ? 0 : createdAts.size();
	}
}