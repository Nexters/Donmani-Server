package donmani.donmani_server.common.service;

import org.springframework.stereotype.Service;

import donmani.donmani_server.common.dto.AppVersionDTO;
import donmani.donmani_server.common.entity.AppVersion;
import donmani.donmani_server.common.entity.PlatformType;
import donmani.donmani_server.common.repository.AppVersionRepository;
import donmani.donmani_server.expense.dto.ExpenseResponseDTO;
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