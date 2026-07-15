package donmani.donmani_server.fcm.entity;

import java.util.Locale;

public enum FortuneProvider {
	GPT,
	GEMINI;

	public static FortuneProvider from(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("provider는 gpt 또는 gemini 여야 합니다.");
		}

		try {
			return FortuneProvider.valueOf(value.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("provider는 gpt 또는 gemini 여야 합니다.");
		}
	}
}
