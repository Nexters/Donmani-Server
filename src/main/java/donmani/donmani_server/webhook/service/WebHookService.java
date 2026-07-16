package donmani.donmani_server.webhook.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import donmani.donmani_server.webhook.repository.WebHookRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WebHookService {
	private final WebHookRepository webHookRepository;
	private final WebClient webClient;

	@Value("${discord.statistics.webhook.url:}")
	private String webhookUrl;
	private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");

	public void sendDailyUserStatsReport() {
		LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

		LocalDateTime yesterday      = today.atStartOfDay().minusDays(1);

		String formattedToday = today.format(formatter);
		String formattedYesterday = yesterday.format(formatter);

		// 1. 오늘 앱 설치 수 (신규 가입)
		Integer todayNewUserCount = webHookRepository.countNewUsersOnDate(yesterday);

		// 2. 누적 앱 설치 수
		Integer totalUserCountUntilYesterday = webHookRepository.countAllUsersBefore(yesterday);

		// 3. 오늘 서비스 진입 수
		Integer todayLoginUserCount = webHookRepository.countLoginUsersOnDate(yesterday);

		// 4. 오늘 기록 완료 수
		Integer todaySubmittedUserCount = webHookRepository.countExpenseSubmittersOnDate(yesterday);

		// 5. 알림 수신 동의 여부 수
		Integer notificationOptInUserCount = webHookRepository.countByNoticeEnableTrueUser();

		// 6. 2일 연속 기록 작성 수
		Integer twoDayStreakUserCount = webHookRepository.countUsersWithStreak(yesterday, 1);

		// 7. 3일 연속 기록 작성 수
		Integer threeDayStreakUserCount = webHookRepository.countUsersWithStreak(yesterday, 2);

		sendToDiscord(formattedToday
			, formattedYesterday
			, todayNewUserCount
			, totalUserCountUntilYesterday
			, todayLoginUserCount
			, todaySubmittedUserCount
			, notificationOptInUserCount
			, twoDayStreakUserCount
			, threeDayStreakUserCount);
	}

	private void sendToDiscord(String today
		, String yesterday
		, Integer newUsers
		, Integer allUsers
		, Integer todayLoginUsers
		, Integer todaySubmitExpenseUsers
		, Integer noticeEnableTrueUsers
		, Integer twoDayStreakUserCount
		, Integer threeDayStreakUserCount) {
		String message = "기준일 : " + yesterday + "\n\n"
			           + "1️⃣  오늘 앱 설치 수 -> [" + newUsers + "]\n\n"
			           + "2️⃣  누적 앱 설치 수 -> [" + allUsers + "]\n\n"
			           + "3️⃣  오늘 서비스 진입 수 -> [" + todayLoginUsers + "]\n\n"
			           + "4️⃣  오늘 기록 완료 수 -> [" + todaySubmitExpenseUsers + "]\n\n"
			           + "5️⃣  알림 받기 설정 수 -> [" + noticeEnableTrueUsers + "]\n\n"
			           + "6️⃣  2일 연속 작성 수 -> [" + twoDayStreakUserCount + "]\n\n"
			           + "7️⃣  3일 연속 작성 수 -> [" + threeDayStreakUserCount + "]\n\n";

		Map<String, Object> embed = Map.of(
			"title", "📊 [" + today + "] 별별소 통계",
			"description", message,
			"color", 0x00BFFF
		);

		Map<String, Object> payload = Map.of(
			"embeds", List.of(embed)
		);

		assertWebhookConfigured();
		webClient.post()
			.uri(webhookUrl)
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(payload)
			.retrieve()
			.toBodilessEntity()
			.subscribe();
	}

	private void assertWebhookConfigured() {
		if (!StringUtils.hasText(webhookUrl) || "default-value".equals(webhookUrl)) {
			throw new IllegalStateException("discord.statistics.webhook.url 설정이 필요합니다.");
		}
	}
}
