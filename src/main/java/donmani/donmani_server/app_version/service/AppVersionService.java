package donmani.donmani_server.app_version.service;

import org.springframework.stereotype.Service;

import donmani.donmani_server.app_version.entity.PlatformType;
import donmani.donmani_server.app_version.repository.AppVersionRepository;
import donmani.donmani_server.app_version.dto.AppVersionDTO;
import donmani.donmani_server.app_version.entity.AppVersion;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppVersionService {

	private final AppVersionRepository appVersionRepository;

	public AppVersionDTO getLatestVersionByPlatform(PlatformType platformType) {
		AppVersion appVersion = appVersionRepository.findLatestVersionByPlatform(platformType);

		return AppVersionDTO.builder()
			.platformType(appVersion.getPlatformType())
			.latestVersion(appVersion.getLatestVersion())
			.forcedUpdateYn(appVersion.getForceUpdateYn())
			.build();
	}
}