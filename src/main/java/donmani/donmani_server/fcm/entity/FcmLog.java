package donmani.donmani_server.fcm.entity;

import donmani.donmani_server.common.log.BaseTimeEntity;
import donmani.donmani_server.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false)
	private String fcmTokenSnapshot;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private NotificationType notificationType;

	// @Column(nullable = false)
	// private String title;
	//
	// @Column(columnDefinition = "TEXT", nullable = false)
	// private String content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PushStatus status;

	private String errorCode;

	@Column(columnDefinition = "TEXT")
	private String errorMessage;

	@Builder
	public FcmLog(
		User user,
		String fcmTokenSnapshot,
		NotificationType notificationType,
		// String title,
		// String content,
		PushStatus status,
		String errorCode,
		String errorMessage
	) {
		this.user = user;
		this.fcmTokenSnapshot = fcmTokenSnapshot;
		this.notificationType = notificationType;
		// this.title = title;
		// this.content = content;
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