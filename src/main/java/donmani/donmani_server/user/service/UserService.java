package donmani.donmani_server.user.service;

import java.util.Optional;

import donmani.donmani_server.user.dto.UserRegisterResponseDTO;
import donmani.donmani_server.user.dto.UpdateUsernameResponseDTO;
import donmani.donmani_server.user.dto.UserRegisterResponseDTOV2;
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
	public UserRegisterResponseDTOV2 registerUserV2(String userKey) {
		UserRegisterResponseDTOV2 response;
		Optional<User> user = userRepository.findByUserKey(userKey);

		// 1. 신규 유저
		if (user.isEmpty()) {
			String randomUsername = User.generateRandomUsername();

			User newUser = User.builder()
				.userKey(userKey)
				.name(randomUsername + "의 별통이")
				.level(1)
				.build();

			userRepository.save(newUser);

			response = new UserRegisterResponseDTOV2(true, newUser.getUserKey(), newUser.getName());
		}
		// 2. 기존 유저
		else {
			response = new UserRegisterResponseDTOV2(false, user.get().getUserKey(), user.get().getName());
		}

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

	@Transactional
	public Long getUserIdByUserKey(String userKey) {
		return userRepository.findByUserKey(userKey)
			.orElseThrow(() -> new IllegalArgumentException("유저 정보를 찾을 수 없습니다."))
			.getId();
	}

	@Transactional
	public Long getUserIdByUserKeyV2(String userKey) {
		return userRepository.findByUserKey(userKey)
			.map(User::getId)
			.orElse(-1L); // 기본값으로 -1을 리턴

	}
}