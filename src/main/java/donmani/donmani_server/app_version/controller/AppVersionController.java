package donmani.donmani_server.app_version.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import donmani.donmani_server.app_version.entity.PlatformType;
import donmani.donmani_server.app_version.dto.AppVersionDTO;
import donmani.donmani_server.app_version.service.AppVersionService;
import donmani.donmani_server.common.httpStatus.HttpStatusDTO;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AppVersionController {
	private final AppVersionService appVersionService;

	@GetMapping("api/v1/appVersion/{platformType}")
	public ResponseEntity<HttpStatusDTO<AppVersionDTO>> getLatestVersion(@PathVariable PlatformType platformType) {
		AppVersionDTO appVersion = appVersionService.getLatestVersionByPlatform((platformType));
		return ResponseEntity.ok(
			HttpStatusDTO.response(HttpStatus.OK.value(), "성공", appVersion));
	}
}
