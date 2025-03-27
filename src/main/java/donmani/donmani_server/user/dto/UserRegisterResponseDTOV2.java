package donmani.donmani_server.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserRegisterResponseDTOV2{
	private boolean isNew;
	private String userKey;
	private String userName;
}
