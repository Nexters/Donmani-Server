package donmani.donmani_server.reward.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "사용자별 리워드 접속 여부 DTO")
public class RewardCheckDTO {
    @Schema(description = "리워드 접속 여부", example = "true")
    private boolean checked;
}
