package donmani.donmani_server.expense.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import donmani.donmani_server.expense.entity.CategoryType;
import donmani.donmani_server.expense.entity.FlagType;

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
public class ContentDTO {
	private FlagType flag;
	private CategoryType category;
	private String memo;
}

