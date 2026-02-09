package donmani.donmani_server.fcm.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "fortune", uniqueConstraints = {@UniqueConstraint(name = "uk_fortune_target_date", columnNames = "target_date")})
public class Fortune {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "target_date", nullable = false, columnDefinition = "DATE")
	private LocalDate targetDate;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private String subtitle;

	@Column(columnDefinition = "TEXT", nullable = false)
	private String content;

	@Column(nullable = false)
	private String item;

	@Builder
	public Fortune(
		LocalDate targetDate,
		String title,
		String subtitle,
		String content,
		String item
	) {
		this.targetDate = targetDate;
		this.title = title;
		this.subtitle = subtitle;
		this.content = content;
		this.item = item;
	}
}