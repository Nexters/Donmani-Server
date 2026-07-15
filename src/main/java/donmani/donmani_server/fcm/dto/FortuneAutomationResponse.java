package donmani.donmani_server.fcm.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import donmani.donmani_server.fcm.entity.FortuneGenerationJob;
import donmani.donmani_server.fcm.entity.FortuneGenerationJobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FortuneAutomationResponse {
	private String targetMonth;
	private LocalDate targetDate;
	private FortuneGenerationJobStatus status;
	private Integer fortuneCount;
	private LocalDateTime fortunesGeneratedAt;
	private LocalDateTime approvalRequestedAt;
	private LocalDateTime approvedAt;
	private LocalDateTime completedAt;
	private LocalDateTime webhookSentAt;
	private String imageUrl;
	private String failureMessage;
	private boolean executed;

	public static FortuneAutomationResponse from(
		FortuneGenerationJob job,
		boolean executed
	) {
		return FortuneAutomationResponse.builder()
			.targetMonth(job.getTargetMonth().toString().substring(0, 7))
			.status(job.getStatus())
			.fortuneCount(job.getFortuneCount())
			.fortunesGeneratedAt(job.getFortunesGeneratedAt())
			.approvalRequestedAt(job.getApprovalRequestedAt())
			.approvedAt(job.getApprovedAt())
			.completedAt(job.getCompletedAt())
			.webhookSentAt(job.getWebhookSentAt())
			.failureMessage(job.getFailureMessage())
			.executed(executed)
			.build();
	}

	public static FortuneAutomationResponse from(
		FortuneGenerationJob job,
		boolean executed,
		LocalDate targetDate,
		String imageUrl
	) {
		FortuneAutomationResponse response = from(job, executed);
		response.targetDate = targetDate;
		response.imageUrl = imageUrl;
		return response;
	}
}
