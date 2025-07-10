package donmani.donmani_server.expense.dto;

import java.util.List;

import donmani.donmani_server.reward.dto.RewardItemResponseDTO;
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
public class ExpenseResponseDTO {
	private String userKey;
	private List<RecordDTO> records;
	private List<RewardItemResponseDTO> saveItems;
	private boolean hasNotOpenedRewards;
	private int totalExpensesCount;
}
