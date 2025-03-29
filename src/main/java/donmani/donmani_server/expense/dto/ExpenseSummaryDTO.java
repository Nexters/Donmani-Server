package donmani.donmani_server.expense.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "사용자별 연도별 소비 기록 요약 DTO")
public class ExpenseSummaryDTO {
    @Schema(description = "연도", example = "2025")
    private int year;

    @Schema(description = "각 월별 소비 기록 정보", example = "{1: {recordCount: 10, totalDaysInMonth: 28}, 2: {recordCount: 15, totalDaysInMonth: 28}}")
    private Map<Integer, RecordInfoDTO> monthlyRecords;
}
