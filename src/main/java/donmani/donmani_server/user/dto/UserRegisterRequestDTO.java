package donmani.donmani_server.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterRequestDTO {
	@NotBlank(message = "Device ID가 존재하지 않습니다.")
	private String userKey;
}