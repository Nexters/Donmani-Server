package donmani.donmani_server.fcm.entity;

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
@Table(name = "fcm_logs")
public class FcmLog extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String fcmTokenSnapshot;

	@Column(nullable = false)
	private String title;

	@Column(columnDefinition = "TEXT", nullable = false)
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PushStatus status;

	private String errorCode;

	@Column(columnDefinition = "TEXT")
	private String errorMessage;

	@Builder
	public FcmLog(
		String fcmTokenSnapshot,
		String title,
		String content,
		PushStatus status,
		String errorCode,
		String errorMessage
	) {
		this.fcmTokenSnapshot = fcmTokenSnapshot;
		this.title = title;
		this.content = content;
		this.status = status;
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}

	public void updateStatus(
		PushStatus status,
		String errorCode,
		String errorMessage
	) {
		this.status = status;
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}
}