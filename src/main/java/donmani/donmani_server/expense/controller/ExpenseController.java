package donmani.donmani_server.expense.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import donmani.donmani_server.expense.dto.ExpenseRequestDTO;
import donmani.donmani_server.expense.dto.ExpenseResponseDTO;
import donmani.donmani_server.expense.service.ExpenseService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/expenses")
@RequiredArgsConstructor
public class ExpenseController {
	private final ExpenseService expenseService;

	@GetMapping("/{deviceId}")
	public ResponseEntity<ExpenseResponseDTO> getExpenses(
		@PathVariable String deviceId,
		@RequestParam int year,
		@RequestParam int month) {
		ExpenseResponseDTO response = expenseService.getExpenses(deviceId, year, month);
		return ResponseEntity.ok(response);
	}

	@PostMapping
	public ResponseEntity<Void> addExpense(@RequestBody ExpenseRequestDTO request) {
		expenseService.addExpense(request);
		return ResponseEntity.ok().build();
	}
}
