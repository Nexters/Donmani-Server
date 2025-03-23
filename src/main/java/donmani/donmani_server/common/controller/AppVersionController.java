package donmani.donmani_server.common.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import donmani.donmani_server.common.dto.AppVersionDTO;
import donmani.donmani_server.common.entity.PlatformType;
import donmani.donmani_server.common.service.AppVersionService;
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
