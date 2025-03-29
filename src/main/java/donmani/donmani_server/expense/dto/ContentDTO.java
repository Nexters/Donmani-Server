package donmani.donmani_server.expense.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import donmani.donmani_server.expense.entity.CategoryType;
import donmani.donmani_server.expense.entity.FlagType;

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
@JsonInclude(JsonInclude.Include.NON_NULL) // null 값 제외
@Schema(description = "소비 기록의 상세 항목 DTO")
public class ContentDTO {

	@Schema(description = "소비 유형", example = "GOOD")
	private FlagType flag;

	@Schema(description = "소비 카테고리", example = "FOOD")
	private CategoryType category;

	@Schema(description = "소비 메모", example = "Lunch at restaurant")
	private String memo;
}

