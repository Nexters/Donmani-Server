package donmani.donmani_server.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserRegisterResponseDTO {
	private boolean isNew;
	private String userKey;
	private String userName;
}