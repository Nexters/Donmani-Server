package donmani.donmani_server.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UpdateUsernameRequestDTO {
	@NotBlank(message = "Device ID가 존재하지 않습니다.")
	private String userKey;
	@NotBlank(message = "변경하려는 이름이 존재하지 않습니다.")
	private String newUserName;
}
