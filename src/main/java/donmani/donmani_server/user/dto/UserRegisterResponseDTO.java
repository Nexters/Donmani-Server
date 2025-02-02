package donmani.donmani_server.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserRegisterResponseDTO {
	private String userKey;
	private String nickname;
}