package donmani.donmani_server.expense.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

	@Transactional
	public void addExpense(ExpenseRequestDTO request) {
		Long userId = userService.getUserIdByUserKey(request.getDeviceId());

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
	public ExpenseResponseDTO getExpenses(String deviceId, int year, int month) {
		Long userId = userService.getUserIdByUserKey(deviceId);

		if (userId == null) {
			throw new EntityNotFoundException("유저 정보를 찾을 수 없습니다.");
		}

		// userId와 year, month로 직접 expense를 조회
		List<Expense> expenses = expenseRepository.findByUserIdAndMonth(userId, year, month);

		// 하루에 하나의 기록이라면, groupBy 없이 바로 리스트로 변환
		List<RecordDTO> records = expenses.stream()
			.map(expense -> {
				// 해당 expense에서 contents가 없으면 null로 설정
				List<ContentDTO> contents = (expense.getCategory() == null || expense.getFlag() == null || expense.getMemo() == null)
					? Collections.emptyList()
					: List.of(ContentDTO.builder()
					.flag(expense.getFlag())
					.category(expense.getCategory())
					.memo(expense.getMemo())
					.build());

				return RecordDTO.builder()
					.date(expense.getCreatedAt().toLocalDate()) // 날짜 설정
					.contents(contents)  // 내용이 없으면 null
					.build();
			})
			.collect(Collectors.toList());

		return ExpenseResponseDTO.builder()
			.deviceId(deviceId)
			.records(records)
			.build();
	}

}