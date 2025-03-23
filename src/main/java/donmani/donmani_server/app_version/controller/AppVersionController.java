package donmani.donmani_server.app_version.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import donmani.donmani_server.app_version.entity.PlatformType;
import donmani.donmani_server.app_version.dto.AppVersionDTO;
import donmani.donmani_server.app_version.service.AppVersionService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api2/appVersion")
@RequiredArgsConstructor
public class AppVersionController {
	private final AppVersionService appVersionService;

	@GetMapping("/{platformType}")
	public ResponseEntity<AppVersionDTO> getLatestVersion(@PathVariable PlatformType platformType) {
		AppVersionDTO appVersionDTO = appVersionService.getLatestVersionByPlatform((platformType));
		return ResponseEntity.ok(appVersionDTO);
	}
}
