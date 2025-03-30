package donmani.donmani_server.expense.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import donmani.donmani_server.expense.dto.*;
import donmani.donmani_server.expense.entity.CategoryType;
import donmani.donmani_server.expense.entity.FlagType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import donmani.donmani_server.expense.entity.Expense;
import donmani.donmani_server.expense.repository.ExpenseRepository;
import donmani.donmani_server.user.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExpenseService {

	private final ExpenseRepository expenseRepository;
	private final UserService userService;

	private final static int SIZE = 20; // 전체 조회 페이지 사이즈 고정

	@Transactional
	public void addExpense(ExpenseRequestDTO request) {
		Long userId = userService.getUserIdByUserKey(request.getUserKey());

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
						.build());
				}

				// 기록이 있을 때
				return contents.stream().map(content -> Expense.builder()
					.userId(userId)
					.createdAt(record.getDate().atStartOfDay())
					.flag(content.getFlag())
					.category(content.getCategory())
					.memo(content.getMemo())
					.build());
			})
			.collect(Collectors.toList());

		expenseRepository.saveAll(expenses);
	}


	@Transactional(readOnly = true)
	public ExpenseResponseDTO getExpenses(String userKey, int year, int month, boolean sortedDesc) {
		Long userId = userService.getUserIdByUserKey(userKey);

		if (userId == null) {
			throw new EntityNotFoundException("유저 정보를 찾을 수 없습니다.");
		}

		// userId와 year, month로 직접 expense를 조회
		List<Expense> expenses = expenseRepository.findByUserIdAndMonth(userId, year, month);

		if(expenses.isEmpty()) {
			return ExpenseResponseDTO.builder()
					.userKey(userKey)
					.records(null)
					.build();
		}

		return ExpenseResponseDTO.builder()
			.userKey(userKey)
			.records(expenseToDto(expenses, sortedDesc))
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
	// 미사용
	@Transactional(readOnly = true)
	public ExpenseResponseDTO getAllExpenses(String userKey, int page) {
		Long userId = userService.getUserIdByUserKey(userKey);

		if (userId == null) {
			throw new EntityNotFoundException("유저 정보를 찾을 수 없습니다.");
		}

		// 시간순 정렬 20개
		Page<LocalDateTime> localDateTimes = expenseRepository.findDistinctCreatedAt(userId, PageRequest.of(page, SIZE));
		if (localDateTimes.isEmpty()) {
			return ExpenseResponseDTO.builder()
					.userKey(userKey)
					.records(null)
					.build();
		}

		List<Expense> expenses = expenseRepository.findByCreatedAtIn(userId, localDateTimes.getContent());

		return ExpenseResponseDTO.builder()
				.userKey(userKey)
				.records(expenseToDto(expenses, true))
				.build();
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

		// RecordInfoDTO 변환
		Map<Integer, RecordInfoDTO> monthlyRecords = monthlyCounts.entrySet().stream()
			.collect(Collectors.toMap(
				Map.Entry::getKey, // 월(MonthValue)
				entry -> new RecordInfoDTO(
					entry.getValue(), // 해당 월의 기록이 있는 날짜 수
					YearMonth.of(year, entry.getKey()).lengthOfMonth() // 해당 월의 총 날짜 수
				)
			));

		// Map<Integer, RecordInfoDTO> monthlyRecords = expenses.stream()
		// 		.filter(exp -> exp.getCreatedAt().getYear() == year)
		// 		.collect(Collectors.groupingBy(
		// 				exp -> exp.getCreatedAt().getMonthValue(),
		// 				Collectors.collectingAndThen(
		// 						Collectors.toList(),
		// 						list -> {
		// 							long recordCount = list.size();
		// 							int totalDaysInMonth = YearMonth.of(year, list.get(0).getCreatedAt().getMonthValue()).lengthOfMonth();
		// 							return new RecordInfoDTO(recordCount, totalDaysInMonth);
		// 						}
		// 				)
		// 		));

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

		expenses.forEach(expense -> categoryCounts.put(expense.getCategory(), categoryCounts.get(expense.getCategory()) + 1));

		return CategoryStatisticsDTO.builder()
				.year(year)
				.month(month)
				.categoryCounts(categoryCounts)
				.build();
	}

}