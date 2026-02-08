package donmani.donmani_server.fcm.entity;

import java.time.LocalDateTime;

import donmani.donmani_server.common.log.BaseTimeEntity;
import donmani.donmani_server.user.entity.User;
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
@Table(name = "fortune_histories")
public class FortuneHistory extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "fortune_id")
	private Fortune fortune;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Enumerated(EnumType.STRING)
	private ReadSource readSource;

	private LocalDateTime readAt;

	@Builder
	public FortuneHistory(
		Fortune fortune,
		User user
	) {
		this.fortune = fortune;
		this.user = user;
	}

	public void markAsRead(
		ReadSource readSource,
		LocalDateTime localDateTime
	) {
		if (this.readAt == null) {
			this.readSource = readSource;
			this.readAt = localDateTime;
		}
	}
}