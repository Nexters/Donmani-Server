package donmani.donmani_server.user.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import donmani.donmani_server.expense.dto.NoticeReadDTO;
import donmani.donmani_server.reward.dto.RewardCheckDTO;
import donmani.donmani_server.reward.entity.UserItem;
import donmani.donmani_server.reward.repository.UserItemRepository;
import donmani.donmani_server.user.dto.UserRegisterResponseDTO;
import donmani.donmani_server.user.dto.UpdateUsernameResponseDTO;
import donmani.donmani_server.user.dto.UserRegisterResponseDTOV1;
import donmani.donmani_server.user.entity.User;
import donmani.donmani_server.user.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final UserItemRepository userItemRepository;

	@Transactional
	public UserRegisterResponseDTO registerUser(String userKey) {
		User user = userRepository.findByIdentifier(userKey)
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
	public UserRegisterResponseDTOV1 registerUserV1(String userKey) {
		UserRegisterResponseDTOV1 response;
		Optional<User> user = userRepository.findByIdentifier(userKey);

		LocalDateTime localDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

		// 1. 신규 유저
		if (user.isEmpty()) {
			String randomUsername = User.generateRandomUsername();

			User newUser = User.builder()
				.userKey(userKey)
				.name(randomUsername + "의 별통이")
				.level(1)
				.createdDate(localDateTime)
				.updateDate(localDateTime)
				.lastLoginDate(localDateTime)
				.isNoticeEnable(false)
				.build();

			userRepository.save(newUser);

			response = new UserRegisterResponseDTOV1(true, newUser.getUserKey(), newUser.getName());
		}
		// 2. 기존 유저
		else {
			User oldUser = user.get();

			response = new UserRegisterResponseDTOV1(false, oldUser.getUserKey(), oldUser.getName());
		}

		return response;
	}

	@Transactional
	public UpdateUsernameResponseDTO updateUsername(String userKey, String newUserName) {
		LocalDateTime localDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

		User user = userRepository
			.findByIdentifier(userKey)
			.orElseThrow(() -> new IllegalArgumentException("유저 정보를 찾을 수 없습니다."));

		user.setName(newUserName);
		user.setUpdateDate(localDateTime);

		UpdateUsernameResponseDTO response = new UpdateUsernameResponseDTO(user.getUserKey(), user.getName());

		return response;
	}

	@Transactional(readOnly = true)
	public Long getUserIdByUserKey(String userKey) {
		return userRepository.findByIdentifier(userKey)
			.orElseThrow(() -> new IllegalArgumentException("유저 정보를 찾을 수 없습니다."))
			.getId();
	}

	@Transactional(readOnly = true)
	public Long getUserIdByUserKeyV1(String userKey) {
		return userRepository.findByIdentifier(userKey)
			.map(User::getId)
			.orElse(-1L); // 기본값으로 -1을 리턴
	}

	@Transactional
	public void markNoticeAsRead(String userKey) {
		LocalDateTime localDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

		User user = userRepository.findByIdentifier(userKey)
				.orElseThrow(() -> new IllegalArgumentException("User not found"));

		user.setNoticeRead(true); // 공지사항 읽음 처리
		user.setUpdateDate(localDateTime);

		userRepository.save(user);
	}

	@Transactional(readOnly = true)
	public NoticeReadDTO getNoticeReadStatus(String userKey) {
		User user = userRepository.findByIdentifier(userKey)
				.orElseThrow(() -> new IllegalArgumentException("User not found"));

		return NoticeReadDTO.builder().read(user.isNoticeRead()).build(); // 읽음 여부 반환
	}

	@Transactional
	public void updateUserNoticeEnable(String userKey, boolean isNoticeEnable) {
		LocalDateTime localDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

		User user = userRepository
			.findByIdentifier(userKey)
			.orElseThrow(() -> new IllegalArgumentException("유저 정보를 찾을 수 없습니다."));

		user.setNoticeEnable(isNoticeEnable);
		user.setUpdateDate(localDateTime);
	}

	@Transactional
	public void updateUserLastLoginDate(String userKey) {
		LocalDateTime localDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

		User user = userRepository
			.findByIdentifier(userKey)
			.orElseThrow(() -> new IllegalArgumentException("유저 정보를 찾을 수 없습니다."));

		user.setLastLoginDate(localDateTime);
		user.setUpdateDate(localDateTime);
	}

	public User getUser(String identifier) {
		User user = userRepository
			.findByIdentifier(identifier)
			.orElseThrow(() -> new IllegalArgumentException("유저 정보를 찾을 수 없습니다."));

		return user;
	}

	@Transactional(readOnly = true)
	public RewardCheckDTO getRewardCheckedStatus(String userKey) {
		User user = userRepository.findByIdentifier(userKey)
				.orElseThrow(() -> new IllegalArgumentException("User not found"));

		ZoneId zoneId = ZoneId.of("Asia/Seoul");
		LocalDateTime threeDaysAgo = LocalDateTime.now(zoneId).minusDays(3);

		List<UserItem> acquiredItems = userItemRepository.findByUserOrderByAcquiredAtDesc(user);

		boolean isRewardChecked = acquiredItems.stream()
				.anyMatch(item -> item.getAcquiredAt().isAfter(threeDaysAgo));

		return RewardCheckDTO.builder().checked(isRewardChecked).build();
	}
}