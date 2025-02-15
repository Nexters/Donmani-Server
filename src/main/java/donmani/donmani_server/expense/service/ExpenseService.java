package donmani.donmani_server.expense.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import donmani.donmani_server.expense.dto.ContentDTO;
import donmani.donmani_server.expense.dto.ExpenseRequestDTO;
import donmani.donmani_server.expense.dto.ExpenseResponseDTO;
import donmani.donmani_server.expense.dto.RecordDTO;
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

}