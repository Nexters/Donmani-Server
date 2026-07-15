package donmani.donmani_server.fcm.service;

import java.time.LocalDate;

public record GeneratedFortunePayload(
	LocalDate targetDate,
	String title,
	String subtitle,
	String content,
	String item
) {
}
