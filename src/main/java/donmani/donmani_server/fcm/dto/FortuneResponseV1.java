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
public class FortuneResponseV1 {
	// @Schema(description = "운세 ID", example = "102")
	// private Long id;

	@Schema(description = "운세 일자", example = "2026-02-02")
	private LocalDate targetDate;

	@Schema(description = "운세 제목", example = "뜻밖의 행운이 찾아오는 날")
	private String title;

	@Schema(description = "운세 내용", example = "오늘은 파이썬 코드 에러가 한 번에 해결될 운명입니다.")
	private String content;

	public static FortuneResponseV1 from(Fortune fortune) {
		return FortuneResponseV1.builder()
			.targetDate(fortune.getTargetDate())
			.title(fortune.getTitle())
			.content(fortune.getContent())
			.build();
	}
}