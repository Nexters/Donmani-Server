package donmani.donmani_server.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserNoticeEnableRequestDTO {
	private String userKey;
	private boolean noticeEnable;
}
