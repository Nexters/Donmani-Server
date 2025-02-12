package donmani.donmani_server.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UpdateUsernameResponseDTO {
	private String userKey;
	private String updatedUserName;
}
