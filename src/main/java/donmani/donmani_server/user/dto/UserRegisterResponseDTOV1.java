package donmani.donmani_server.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterResponseDTOV1 {
	private boolean isNew;
	private String userKey;
	private String userName;
}
