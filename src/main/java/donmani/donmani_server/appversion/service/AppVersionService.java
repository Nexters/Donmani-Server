package donmani.donmani_server.appversion.service;

import org.springframework.stereotype.Service;

import donmani.donmani_server.appversion.entity.PlatformType;
import donmani.donmani_server.appversion.repository.AppVersionRepository;
import donmani.donmani_server.appversion.dto.AppVersionDTO;
import donmani.donmani_server.appversion.entity.AppVersion;
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