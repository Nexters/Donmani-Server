package donmani.donmani_server.user.controller;

import donmani.donmani_server.common.httpStatus.HttpStatusDTO;
import donmani.donmani_server.user.dto.UpdateUsernameRequestDTO;
import donmani.donmani_server.user.dto.UpdateUsernameResponseDTO;
import donmani.donmani_server.user.dto.UserRegisterRequestDTO;
import donmani.donmani_server.user.dto.UserRegisterResponseDTO;
import donmani.donmani_server.user.dto.UserRegisterResponseDTOV2;
import donmani.donmani_server.user.service.UserService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class UserController {
	private final UserService userService;

	@PostMapping("users/register")
	public ResponseEntity<UserRegisterResponseDTO> registerUserV1(
		@Valid @RequestBody UserRegisterRequestDTO request) {

		UserRegisterResponseDTO response = userService.registerUser(request.getUserKey());

		return ResponseEntity.ok(response);
	}

	@PostMapping("api/v1/user/register")
	public ResponseEntity<HttpStatusDTO<UserRegisterResponseDTOV2>> registerUserV2(
		@Valid @RequestBody UserRegisterRequestDTO request) {

		ResponseEntity<HttpStatusDTO<UserRegisterResponseDTOV2>> response;

		// 1. userId
		long userId = userService.getUserIdByUserKeyV2(request.getUserKey());

		// 2.1 신규 유저 -> 201
		if (userId == -1L) {
			UserRegisterResponseDTOV2 user = userService.registerUserV2(request.getUserKey());
			response = ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.CREATED.value(), "신규 유저", user));
		}

		// 2,2 기존 유저 -> 200
		else {
			UserRegisterResponseDTOV2 user = userService.registerUserV2(request.getUserKey());
			response = ResponseEntity.ok(HttpStatusDTO.response(HttpStatus.OK.value(), "기존 유저", user));
		}

		return response;
	}

	@PutMapping("users/update")
	public ResponseEntity<UpdateUsernameResponseDTO> updateNicknameV1(
		@Valid @RequestBody UpdateUsernameRequestDTO request) {

		UpdateUsernameResponseDTO response = userService.updateUsername(request.getUserKey(), request.getNewUserName());

		return ResponseEntity.ok(response);
	}

	@PostMapping("api/v1/user/update")
	public ResponseEntity<HttpStatusDTO<UpdateUsernameResponseDTO>> updateNicknameV2(
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
}