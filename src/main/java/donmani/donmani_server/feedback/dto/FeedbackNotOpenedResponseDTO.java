package donmani.donmani_server.feedback.dto;

import java.util.List;

import donmani.donmani_server.expense.dto.RecordDTO;
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
public class FeedbackNotOpenedResponseDTO {
	private Boolean isNotOpened;
	private Boolean isFirstOpen;
	private Integer totalCount;
}
