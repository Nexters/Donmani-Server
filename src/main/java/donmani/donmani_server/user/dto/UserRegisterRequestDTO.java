package donmani.donmani_server.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRegisterRequestDTO {
	@NotBlank(message = "Device ID가 존재하지 않습니다.")
	private String userKey;
}