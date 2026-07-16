package donmani.donmani_server.webhook.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import donmani.donmani_server.webhook.repository.WebHookRepository;

@ExtendWith(MockitoExtension.class)
class WebHookServiceTest {

	@Mock
	private WebHookRepository webHookRepository;

	@Test
	void sendDailyUserStatsReportRequiresStatisticsWebhookUrl() {
		when(webHookRepository.countNewUsersOnDate(any(LocalDateTime.class))).thenReturn(0);
		when(webHookRepository.countAllUsersBefore(any(LocalDateTime.class))).thenReturn(0);
		when(webHookRepository.countLoginUsersOnDate(any(LocalDateTime.class))).thenReturn(0);
		when(webHookRepository.countExpenseSubmittersOnDate(any(LocalDateTime.class))).thenReturn(0);
		when(webHookRepository.countByNoticeEnableTrueUser()).thenReturn(0);
		when(webHookRepository.countUsersWithStreak(any(LocalDateTime.class), eq(1))).thenReturn(0);
		when(webHookRepository.countUsersWithStreak(any(LocalDateTime.class), eq(2))).thenReturn(0);

		WebHookService webHookService = new WebHookService(webHookRepository, WebClient.builder().build());

		assertThatThrownBy(webHookService::sendDailyUserStatsReport)
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("discord.statistics.webhook.url 설정이 필요합니다.");
	}
}
