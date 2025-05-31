package donmani.donmani_server.user.controller;

import donmani.donmani_server.common.httpStatus.HttpStatusDTO;
import donmani.donmani_server.expense.dto.NoticeReadDTO;
import donmani.donmani_server.reward.dto.RewardCheckDTO;
import donmani.donmani_server.user.dto.UpdateUserNoticeEnableRequestDTO;
import donmani.donmani_server.user.dto.UpdateUsernameRequestDTO;
import donmani.donmani_server.user.dto.UpdateUsernameResponseDTO;
import donmani.donmani_server.user.dto.UserRegisterRequestDTO;
import donmani.donmani_server.user.dto.UserRegisterResponseDTO;
import donmani.donmani_server.user.dto.UserRegisterResponseDTOV1;
import donmani.donmani_server.user.service.UserService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class UserController {
	private final UserService userService;

	@PostMapping("users/register")
	public ResponseEntity<UserRegisterResponseDTO> registerUser(
		@Valid @RequestBody UserRegisterRequestDTO request) {

		UserRegisterResponseDTO response = userService.registerUser(request.getUserKey());

		return ResponseEntity.ok(response);
	}

	@PostMapping("api/v1/user/register")
	public ResponseEntity<HttpStatusDTO<UserRegisterResponseDTOV1>> registerUserV1(
		@Valid @RequestBody UserRegisterRequestDTO request) {

		ResponseEntity<HttpStatusDTO<UserRegisterResponseDTOV1>> response;

		// 1. userId
		long userId = userService.getUserIdByUserKeyV1(request.getUserKey());

		// 2.1 신규 유저 -> 201
		if (userId == -1L) {
			UserRegisterResponseDTOV1 user = userService.registerUserV1(request.getUserKey());
			response = ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.CREATED.value(), "신규 유저", user));
		}

		// 2,2 기존 유저 -> 200
		else {
			UserRegisterResponseDTOV1 user = userService.registerUserV1(request.getUserKey());
			response = ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.OK.value(), "기존 유저", user));
		}

		return response;
	}

	@PutMapping("users/update")
	public ResponseEntity<UpdateUsernameResponseDTO> updateNickname(
		@Valid @RequestBody UpdateUsernameRequestDTO request) {

		UpdateUsernameResponseDTO response = userService.updateUsername(request.getUserKey(), request.getNewUserName());

		return ResponseEntity.ok(response);
	}

	@PostMapping("api/v1/user/update")
	public ResponseEntity<HttpStatusDTO<UpdateUsernameResponseDTO>> updateNicknameV1(
		@Valid @RequestBody UpdateUsernameRequestDTO request) {
		try {
			UpdateUsernameResponseDTO user = userService.updateUsername(request.getUserKey(), request.getNewUserName());

			// 1. 닉네임 변경 성공 -> 201
			return ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.CREATED.value(), "성공", user));
		} catch (IllegalArgumentException e) {
			// 2. 닉네임 변경 실패 -> 500
			return ResponseEntity.ok(
				HttpStatusDTO.response(HttpStatus.INTERNAL_SERVER_ERROR.value(), "유저 정보 없음", null));
		}
	}

	@GetMapping("api/v1/notice/status/{userKey}")
	public ResponseEntity<NoticeReadDTO> getNoticeReadStatus(@PathVariable String userKey) {
		NoticeReadDTO noticeStatus = userService.getNoticeReadStatus(userKey);
		return ResponseEntity.ok(noticeStatus);
	}

	@PutMapping("api/v1/notice/status/{userKey}")
	public ResponseEntity<Void> markNoticeAsRead(@PathVariable String userKey) {
		userService.markNoticeAsRead(userKey);
		return ResponseEntity.ok().build();
	}

	@PutMapping("api/v1/notice/enable/{userKey}")
	public ResponseEntity<HttpStatusDTO<UpdateUserNoticeEnableRequestDTO>> updateUserNoticeEnableV1(
		@Valid @RequestBody UpdateUserNoticeEnableRequestDTO request) {
		try {
			userService.updateUserNoticeEnable(request.getUserKey(), request.isNoticeEnable());

			// 1. 알림수신동의여부 변경 성공 -> 201
			return ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.CREATED.value(), "성공", null));
		} catch (IllegalArgumentException e) {
			// 2. 알림수신동의여부 변경 실패 -> 500
			return ResponseEntity.ok(
				HttpStatusDTO.response(HttpStatus.INTERNAL_SERVER_ERROR.value(), "유저 정보 없음", null));
		}
	}

	@PutMapping("api/v1/user/last-login/{userKey}")
	public ResponseEntity<HttpStatusDTO<Void>> updateUserLastLoginDateV1(
		@PathVariable String userKey) {
		try {
			userService.updateUserLastLoginDate(userKey);

			// 1. 최종접속일자 변경 성공 -> 201
			return ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.CREATED.value(), "성공", null));
		} catch (IllegalArgumentException e) {
			// 2. 최종접속일자 변경 실패 -> 500
			return ResponseEntity.ok(
				HttpStatusDTO.response(HttpStatus.INTERNAL_SERVER_ERROR.value(), "유저 정보 없음", null));
		}
	}

	@GetMapping("api/v1/reward/status/{userKey}")
	public ResponseEntity<RewardCheckDTO> getRewardCheckedStatus(@PathVariable String userKey) {
		RewardCheckDTO rewardStatus = userService.getRewardCheckedStatus(userKey);
		return ResponseEntity.ok(rewardStatus);
	}

	@PutMapping("api/v1/reward/status/{userKey}")
	public ResponseEntity<Void> markRewardAsChecked(@PathVariable String userKey) {
		userService.markRewardAsChecked(userKey);
		return ResponseEntity.ok().build();
	}
}