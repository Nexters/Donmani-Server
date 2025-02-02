package donmani.donmani_server.user.service;

import donmani.donmani_server.user.dto.UserRegisterResponseDTO;
import donmani.donmani_server.user.dto.UpdateUsernameResponseDTO;
import donmani.donmani_server.user.entity.User;
import donmani.donmani_server.user.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;

	@Transactional
	public UserRegisterResponseDTO registerUser(String userKey) {
		User user = userRepository.findByUserKey(userKey)
			.orElseGet(() -> {
				String randomUsername = User.generateRandomUsername();

				User newUser = User.builder()
					.userKey(userKey)
					.name(randomUsername)
					.level(1)
					.build();

				return userRepository.save(newUser);
			});

		UserRegisterResponseDTO response = new UserRegisterResponseDTO(user.getUserKey(), user.getName());

		return response;
	}

	@Transactional
	public UpdateUsernameResponseDTO updateUsername(String userKey, String newUserName) {
		User user = userRepository
			.findByUserKey(userKey)
			.orElseThrow(() -> new IllegalArgumentException("유저 정보를 찾을 수 없습니다."));

		user.setName(newUserName);
		userRepository.save(user);

		UpdateUsernameResponseDTO response = new UpdateUsernameResponseDTO(user.getUserKey(), user.getName());

		return response;
	}
}