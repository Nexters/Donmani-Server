package donmani.donmani_server.expense.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "사용자별 공지사항 읽음 여부 DTO")
public class NoticeReadDTO {
    @Schema(description = "공지사항 읽음 여부", example = "true")
    private boolean read;
}
