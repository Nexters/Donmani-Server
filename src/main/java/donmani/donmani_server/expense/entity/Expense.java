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

	private LocalDateTime createdAt; // 사용자가 기록하려는 일자 -> 어제 기록을 오늘 등록한 경우 어제일자로 들어감

	@Enumerated(EnumType.STRING)
	private CategoryType category;

	@Enumerated(EnumType.STRING)
	private FlagType flag;

	private String memo;

	/*
	 - 2025.04.16
	 - 생성일자, 최종변경일자 칼럼 추가
	*/
	private LocalDateTime createdDate; // 서버에 기록된 일자
	private LocalDateTime updateDate;
}