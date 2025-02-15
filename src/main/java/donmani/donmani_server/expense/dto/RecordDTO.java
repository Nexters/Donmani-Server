package donmani.donmani_server.expense.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

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
public class RecordDTO {
	private LocalDate date;
	private List<ContentDTO> contents; // null 가능

	public static RecordDTO of(LocalDate localDate, List<ContentDTO> contents) {
		RecordDTO recordDTO = new RecordDTO();

		recordDTO.setDate(localDate);
		recordDTO.setContents(contents.get(0) == null ? null : contents);

		return recordDTO;
	}
}
