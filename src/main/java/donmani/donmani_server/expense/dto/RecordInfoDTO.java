package donmani.donmani_server.expense.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "월별 소비 기록 정보 DTO")
public class RecordInfoDTO {
    @Schema(description = "해당 월의 소비 기록 개수", example = "10")
    private long recordCount;

    @Schema(description = "해당 월의 일수", example = "28")
    private int totalDaysInMonth;
}
