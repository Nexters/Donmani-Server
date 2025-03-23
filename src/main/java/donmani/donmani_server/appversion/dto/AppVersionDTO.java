package donmani.donmani_server.appversion.dto;

import donmani.donmani_server.appversion.entity.PlatformType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppVersionDTO {
	private PlatformType platformType;
	private String latestVersion;
	private String forcedUpdateYn;
}
