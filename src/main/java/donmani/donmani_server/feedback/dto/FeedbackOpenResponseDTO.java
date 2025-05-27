package donmani.donmani_server.feedback.dto;

import donmani.donmani_server.expense.entity.CategoryType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
public class FeedbackOpenResponseDTO {
	private String title;

	private String content;

	private String name;

	@Enumerated(EnumType.STRING)
	private CategoryType category;

	private boolean flagType;
}