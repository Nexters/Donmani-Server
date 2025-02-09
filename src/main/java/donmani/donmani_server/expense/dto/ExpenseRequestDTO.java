package donmani.donmani_server.expense.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
	private String userKey;

	@NotEmpty(message = "소비 기록이 입력되지 않았습니다.")
	private List<RecordDTO> records;
}
