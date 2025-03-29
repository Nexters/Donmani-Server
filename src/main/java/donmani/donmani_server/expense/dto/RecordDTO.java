package donmani.donmani_server.expense.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "소비 기록 DTO")
public class RecordDTO {

	@Schema(description = "소비 기록 날짜", example = "2025-02-15")
	private LocalDate date;

	@Schema(description = "소비 기록의 상세 항목 리스트", nullable = true)
	private List<ContentDTO> contents;

	public static RecordDTO of(LocalDate localDate, List<ContentDTO> contents) {
		RecordDTO recordDTO = new RecordDTO();

		recordDTO.setDate(localDate);
		recordDTO.setContents(contents.get(0) == null ? null : contents);

		return recordDTO;
	}
}
