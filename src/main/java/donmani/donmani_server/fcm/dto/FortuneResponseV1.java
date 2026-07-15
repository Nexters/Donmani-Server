package donmani.donmani_server.fcm.dto;

import java.time.LocalDate;

import donmani.donmani_server.fcm.entity.Fortune;
import donmani.donmani_server.fcm.entity.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FortuneResponseV1 {
	// @Schema(description = "운세 ID", example = "102")
	// private Long id;

	@Schema(description = "운세 일자", example = "2026-02-02")
	private LocalDate targetDate;

	@Schema(description = "운세 제목", example = "내일이 월요일이라니!")
	private String title;

	@Schema(description = "운세 부제목", example = "일요일 밤을 달래줄 힐링 비법 🌙")
	private String subtitle;

	@Schema(description = "운세 내용", example = "내일부터 시작될 일상을 위해 오늘은 평소보다 일찍 잠자리에 들어 충분한 휴식을 취하는 것이 가장 큰 투자예요.")
	private String content;

	@Schema(description = "운세 아이템", example = "행운의 시간 : 오후 10시")
	private String item;

	@Schema(description = "운세 이미지 URL", example = "https://example.com/fortune-images/2026-02-02.png")
	private String imageUrl;

	public static FortuneResponseV1 from(Fortune fortune) {
		return FortuneResponseV1.builder()
			.targetDate(fortune.getTargetDate())
			.title(fortune.getTitle())
			.subtitle(fortune.getSubtitle())
			.content(fortune.getContent())
			.item(fortune.getItem())
			.imageUrl(fortune.getImageUrl())
			.build();
	}
}
