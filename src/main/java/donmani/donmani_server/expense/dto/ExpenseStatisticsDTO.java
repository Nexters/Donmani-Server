package donmani.donmani_server.expense.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "사용자별 월별 소비 기록 통계 DTO")
public class ExpenseStatisticsDTO {
    @Schema(description = "연도", example = "2025")
    private int year;

    @Schema(description = "월", example = "2")
    private int month;

    @Schema(description = "행복한 소비 기록 수", example = "8")
    private int goodCount;

    @Schema(description = "후회한 소비 기록 수", example = "2")
    private int badCount;

    @Schema(description = "소비 기록이 존재하는지 여부", example = "true")
    private boolean hasRecords;

    @Schema(description = "소비 기록 리스트")
    private List<RecordDTO> records;
}
