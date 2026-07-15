package donmani.donmani_server.fcm.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import donmani.donmani_server.fcm.entity.FortuneProvider;

@Service
public class FortuneProviderResolver {

	@Value("${fortune.automation.default-provider:gpt}")
	private String defaultProvider;

	public FortuneProvider resolve(String provider) {
		if (provider == null || provider.isBlank()) {
			return getDefaultProvider();
		}

		return FortuneProvider.from(provider);
	}

	public FortuneProvider resolve(FortuneProvider provider) {
		return provider == null ? getDefaultProvider() : provider;
	}

	public FortuneProvider getDefaultProvider() {
		return FortuneProvider.from(defaultProvider);
	}
}
