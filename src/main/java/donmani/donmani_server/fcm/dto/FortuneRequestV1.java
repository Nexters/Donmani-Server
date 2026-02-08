package donmani.donmani_server.fcm.dto;

import donmani.donmani_server.fcm.entity.ReadSource;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FortuneRequestV1 {
	@Schema(description = "유저 키", example = "user-1234")
	private String userKey;

	@Schema(description = "운세 진입", example = "NOTIFICATION")
	private ReadSource readSource;
}