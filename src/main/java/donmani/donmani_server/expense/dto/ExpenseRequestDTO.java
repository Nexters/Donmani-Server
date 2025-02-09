package donmani.donmani_server.expense.dto;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseRequestDTO {
	@NotBlank(message = "Device ID가 존재하지 않습니다.")
	private String deviceId;

	@NotNull(message = "연도가 입력되지 않았습니다.")
	@Min(value = 2025, message = "올바른 연도를 입력하세요.")
	private int year;

	@NotNull(message = "월이 입력되지 않았습니다.")
	@Min(value = 1, message = "월은 1 이상이어야 합니다.")
	@Max(value = 12, message = "월은 12 이하이어야 합니다.")
	private int month;

	@NotEmpty(message = "소비 기록이 입력되지 않았습니다.")
	private List<RecordDTO> records;
}
