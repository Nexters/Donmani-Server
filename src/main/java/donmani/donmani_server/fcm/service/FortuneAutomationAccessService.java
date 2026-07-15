package donmani.donmani_server.fcm.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FortuneAutomationAccessService {

	@Value("${fortune.automation.admin-token:}")
	private String adminToken;

	public boolean isAuthorized(String requestToken) {
		if (!StringUtils.hasText(adminToken)) {
			return false;
		}

		return StringUtils.hasText(requestToken) && adminToken.equals(requestToken);
	}
}
