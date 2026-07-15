package donmani.donmani_server.fcm.dto;

import java.time.LocalDate;

import donmani.donmani_server.fcm.entity.Fortune;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FortuneHistoryResponseV1 {
	@Schema(description = "운세 일자", example = "2026-07-01")
	private LocalDate targetDate;

	@Schema(description = "운세 이미지 URL", example = "https://example.com/fortune-images/2026-07-01.png")
	private String imageUrl;

	@Schema(description = "운세 부제목", example = "7월의 첫 단추를 끼워봐요!")
	private String subtitle;

	@Schema(description = "운세 내용", example = "오늘은 가벼운 마음으로 지갑 속 영수증을 정리하며 마음을 정돈해 보세요.")
	private String content;

	@Schema(description = "운세 아이템", example = "행운의 색 : 연두색")
	private String item;

	public static FortuneHistoryResponseV1 from(Fortune fortune) {
		return FortuneHistoryResponseV1.builder()
			.targetDate(fortune.getTargetDate())
			.imageUrl(fortune.getImageUrl())
			.subtitle(fortune.getSubtitle())
			.content(fortune.getContent())
			.item(fortune.getItem())
			.build();
	}
}
