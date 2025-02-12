package donmani.donmani_server.expense.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long userId;

	private LocalDateTime createdAt;

	@Enumerated(EnumType.STRING)
	private CategoryType category;

	@Enumerated(EnumType.STRING)
	private FlagType flag;

	private String memo;
}