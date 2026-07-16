package donmani.donmani_server.fcm.entity;

import java.time.LocalDate;
import java.time.YearMonth;

import donmani.donmani_server.common.log.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "fortune_ai_call_logs")
public class FortuneAiCallLog extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private FortuneProvider provider;

	@Enumerated(EnumType.STRING)
	@Column(name = "call_type", nullable = false)
	private FortuneAiCallType callType;

	@Column(name = "target_month", columnDefinition = "DATE")
	private LocalDate targetMonth;

	@Column(name = "target_date", columnDefinition = "DATE")
	private LocalDate targetDate;

	@Column(name = "model")
	private String model;

	@Column(columnDefinition = "LONGTEXT", nullable = false)
	private String prompt;

	@Column(name = "response_text", columnDefinition = "LONGTEXT")
	private String responseText;

	@Column(nullable = false)
	private boolean success;

	@Column(name = "error_message", columnDefinition = "TEXT")
	private String errorMessage;

	@Builder
	public FortuneAiCallLog(
		FortuneProvider provider,
		FortuneAiCallType callType,
		YearMonth targetMonth,
		LocalDate targetDate,
		String model,
		String prompt,
		String responseText,
		boolean success,
		String errorMessage
	) {
		this.provider = provider;
		this.callType = callType;
		this.targetMonth = targetMonth == null ? null : targetMonth.atDay(1);
		this.targetDate = targetDate;
		this.model = model;
		this.prompt = prompt;
		this.responseText = responseText;
		this.success = success;
		this.errorMessage = errorMessage;
	}
}
