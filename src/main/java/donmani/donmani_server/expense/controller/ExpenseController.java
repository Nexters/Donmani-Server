package donmani.donmani_server.expense.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import donmani.donmani_server.common.httpStatus.HttpStatusDTO;
import donmani.donmani_server.expense.dto.ExpenseRequestDTO;
import donmani.donmani_server.expense.dto.ExpenseResponseDTO;
import donmani.donmani_server.expense.service.ExpenseService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ExpenseController {
	private final ExpenseService expenseService;

	@GetMapping("expenses/calendar/{userKey}")
	public ResponseEntity<ExpenseResponseDTO> getExpensesCalendarV1(
		@PathVariable String userKey,
		@RequestParam int year,
		@RequestParam int month) {
		ExpenseResponseDTO response = expenseService.getExpenses(userKey, year, month, false);
		return ResponseEntity.ok(response);
	}

	@GetMapping("api/v1/expenses/calendar/{userKey}")
	public ResponseEntity<HttpStatusDTO<ExpenseResponseDTO>> getExpensesCalendarV2(
		@PathVariable String userKey,
		@RequestParam int year,
		@RequestParam int month) {
		try {
			ExpenseResponseDTO response = expenseService.getExpenses(userKey, year, month, false);
			return ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.OK.value(), "성공", response));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.ok(
				HttpStatusDTO.response(HttpStatus.INTERNAL_SERVER_ERROR.value(), "유저 정보 없음", null));
		}
	}

	@GetMapping("api/v1/expenses/list/{userKey}")
	public ResponseEntity<HttpStatusDTO<ExpenseResponseDTO>> getExpensesListV2(
		@PathVariable String userKey,
		@RequestParam int year,
		@RequestParam int month) {
		try {
			ExpenseResponseDTO response = expenseService.getExpenses(userKey, year, month, true);
			return ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.OK.value(), "성공", response));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.ok(
				HttpStatusDTO.response(HttpStatus.INTERNAL_SERVER_ERROR.value(), "유저 정보 없음", null));
		}
	}

	@GetMapping("expenses/list/{userKey}")
	public ResponseEntity<ExpenseResponseDTO> getExpensesListV1(
		@PathVariable String userKey,
		@RequestParam int year,
		@RequestParam int month) {
		ExpenseResponseDTO response = expenseService.getExpenses(userKey, year, month, true);
		return ResponseEntity.ok(response);
	}

	@PostMapping("expenses")
	public ResponseEntity<Void> addExpenseV1(@RequestBody ExpenseRequestDTO request) {
		expenseService.addExpense(request);
		return ResponseEntity.ok().build();
	}

	@PostMapping("api/v1/expenses")
	public ResponseEntity<HttpStatusDTO<Void>> addExpenseV2(@RequestBody ExpenseRequestDTO request) {
		expenseService.addExpense(request);

		return ResponseEntity.ok(
			HttpStatusDTO.response(HttpStatus.CREATED.value(), "성공", null));
	}






}
