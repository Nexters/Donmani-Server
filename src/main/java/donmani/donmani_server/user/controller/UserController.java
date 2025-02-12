package donmani.donmani_server.user.controller;

import donmani.donmani_server.user.dto.UpdateUsernameRequestDTO;
import donmani.donmani_server.user.dto.UpdateUsernameResponseDTO;
import donmani.donmani_server.user.dto.UserRegisterRequestDTO;
import donmani.donmani_server.user.dto.UserRegisterResponseDTO;
import donmani.donmani_server.user.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
	private final UserService userService;


	/*
	 - 유저 등록 메서드
	 - INPUT  : <UserRegisterRequestDTO> request
	 - OUTPUT : <UserRegisterResponseDTO> response
	 */
	@PostMapping("/register")
	public ResponseEntity<UserRegisterResponseDTO> registerUser(
		@Valid @RequestBody UserRegisterRequestDTO request) {

		UserRegisterResponseDTO response = userService.registerUser(request.getUserKey());

		return ResponseEntity.ok(response);
	}


	/*
	 - 유저명 변경 메서드
	 - INPUT  : <UpdateUsernameRequestDTO> request
	 - OUTPUT : <UpdateUsernameResponseDTO> response
	 */
	@PutMapping("/update")
	public ResponseEntity<UpdateUsernameResponseDTO> updateNickname(
		@Valid @RequestBody UpdateUsernameRequestDTO request) {

		UpdateUsernameResponseDTO response = userService.updateUsername(request.getUserKey(), request.getNewUserName());

		return ResponseEntity.ok(response);
	}
}