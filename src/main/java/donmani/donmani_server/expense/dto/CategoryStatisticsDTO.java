package donmani.donmani_server.expense.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import donmani.donmani_server.expense.entity.CategoryType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "사용자별 월별 소비 카테고리 통계 DTO")
public class CategoryStatisticsDTO {
    @Schema(description = "연도", example = "2025")
    private int year;

    @Schema(description = "월", example = "2")
    private int month;

    @Schema(description = "각 카테고리별 소비 기록 수", example = "{FOOD: 5, TRANSPORT: 3}")
    private Map<CategoryType, Integer> categoryCounts;
}
