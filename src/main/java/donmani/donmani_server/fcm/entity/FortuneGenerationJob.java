package donmani.donmani_server.fcm.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import donmani.donmani_server.common.log.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
	name = "fortune_generation_jobs",
	uniqueConstraints = {@UniqueConstraint(name = "uk_fortune_generation_job_target_month", columnNames = "target_month")}
)
public class FortuneGenerationJob extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "target_month", nullable = false, columnDefinition = "DATE")
	private LocalDate targetMonth;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private FortuneGenerationJobStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "trigger_type", nullable = false)
	private FortuneGenerationTriggerType triggerType;

	@Column(name = "fortune_count")
	private Integer fortuneCount;

	@Column(name = "fortunes_generated_at")
	private LocalDateTime fortunesGeneratedAt;

	@Column(name = "approval_requested_at")
	private LocalDateTime approvalRequestedAt;

	@Column(name = "approved_at")
	private LocalDateTime approvedAt;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	@Column(name = "webhook_sent_at")
	private LocalDateTime webhookSentAt;

	@Column(name = "failure_message", columnDefinition = "TEXT")
	private String failureMessage;

	@Builder
	public FortuneGenerationJob(
		LocalDate targetMonth,
		FortuneGenerationJobStatus status,
		FortuneGenerationTriggerType triggerType
	) {
		this.targetMonth = targetMonth;
		this.status = status;
		this.triggerType = triggerType;
	}

	public static FortuneGenerationJob start(
		LocalDate targetMonth,
		FortuneGenerationTriggerType triggerType
	) {
		return FortuneGenerationJob.builder()
			.targetMonth(targetMonth)
			.status(FortuneGenerationJobStatus.GENERATING_FORTUNES)
			.triggerType(triggerType)
			.build();
	}

	public void markGeneratingFortunes(FortuneGenerationTriggerType triggerType) {
		this.status = FortuneGenerationJobStatus.GENERATING_FORTUNES;
		this.triggerType = triggerType;
		this.fortuneCount = null;
		this.fortunesGeneratedAt = null;
		this.approvalRequestedAt = null;
		this.approvedAt = null;
		this.completedAt = null;
		this.webhookSentAt = null;
		this.failureMessage = null;
	}

	public void markWaitingForApproval(
		int fortuneCount,
		LocalDateTime fortunesGeneratedAt,
		LocalDateTime approvalRequestedAt
	) {
		this.status = FortuneGenerationJobStatus.WAITING_FOR_APPROVAL;
		this.fortuneCount = fortuneCount;
		this.fortunesGeneratedAt = fortunesGeneratedAt;
		this.approvalRequestedAt = approvalRequestedAt;
		this.approvedAt = null;
		this.completedAt = null;
		this.webhookSentAt = null;
		this.failureMessage = null;
	}

	public void markApproved(LocalDateTime approvedAt) {
		this.status = FortuneGenerationJobStatus.APPROVED;
		this.approvedAt = approvedAt;
		this.failureMessage = null;
	}

	public void markGeneratingImages(FortuneGenerationTriggerType triggerType) {
		this.status = FortuneGenerationJobStatus.GENERATING_IMAGES;
		this.triggerType = triggerType;
		this.completedAt = null;
		this.webhookSentAt = null;
		this.failureMessage = null;
	}

	public void markCompleted(
		int fortuneCount,
		LocalDateTime completedAt,
		LocalDateTime webhookSentAt
	) {
		this.status = FortuneGenerationJobStatus.COMPLETED;
		this.fortuneCount = fortuneCount;
		this.completedAt = completedAt;
		this.webhookSentAt = webhookSentAt;
		this.failureMessage = null;
	}

	public void markFailed(String failureMessage) {
		this.status = FortuneGenerationJobStatus.FAILED;
		this.completedAt = null;
		this.failureMessage = failureMessage;
	}

	public void markWebhookSent(LocalDateTime webhookSentAt) {
		this.webhookSentAt = webhookSentAt;
		this.failureMessage = null;
	}

	public void markReviewWebhookSent(
		int fortuneCount,
		LocalDateTime webhookSentAt
	) {
		this.status = FortuneGenerationJobStatus.WAITING_FOR_APPROVAL;
		this.fortuneCount = fortuneCount;
		if (this.fortunesGeneratedAt == null) {
			this.fortunesGeneratedAt = webhookSentAt;
		}
		this.approvalRequestedAt = webhookSentAt;
		this.approvedAt = null;
		this.completedAt = null;
		this.webhookSentAt = webhookSentAt;
		this.failureMessage = null;
	}
}
