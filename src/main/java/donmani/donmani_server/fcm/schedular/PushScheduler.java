package donmani.donmani_server.fcm.schedular;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import donmani.donmani_server.fcm.entity.NotificationType;
import donmani.donmani_server.fcm.service.FCMService;
import donmani.donmani_server.fcm.service.FortuneService;
import donmani.donmani_server.user.entity.User;
import donmani.donmani_server.user.service.UserService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PushScheduler {

	private final FCMService fcmService;
	private final FortuneService fortuneService;
	private final UserService userService;

	@Scheduled(cron = "0 31 8 * * *", zone = "Asia/Seoul")
	public void sendDailyFortunesInMorning() {
		List<String> tokens = fcmService.getTokensToSendFortune();

		for (String token : tokens) {
			fortuneService.sendDailyFortune(token);
		}
	}

	@Scheduled(cron = "0 31 12 * * *", zone = "Asia/Seoul")
	public void resendUnreadDailyFortuneInAfternoon() {
		List<String> tokens = fcmService.getTokensToSendFortuneInAfternoon();

		for (String token : tokens) {
			fortuneService.resendUnreadDailyFortune(token);
		}
	}

	@Scheduled(cron = "0 1 22 * * *", zone = "Asia/Seoul")
	public void sendDailyPushInEvening() {
		List<String> fortuneReadTokens = fcmService.getTokensReadFortuneToday();
		List<String> fortuneUnreadAndNoExpenseTokens = fcmService.getTokensNoExpenseTodayAndUnreadFortune();

		for (String token : fortuneReadTokens) {
			sendFortuneReviewPushInEvening(token);
		}

		for (String token : fortuneUnreadAndNoExpenseTokens) {
			sendDefaultPushInEvening(token);
		}
	}

	private void sendFortuneReviewPushInEvening(String token) {
		User user = userService.getUser(token);

		fcmService.sendMessage(
			user,
			token,
			NotificationType.FORTUNE_REMIND,
			"오늘 운세, 실제로 맞았나요?",
			"오늘 운세로 마무리 해보세요."
		);
	}

	private void sendDefaultPushInEvening(String token) {
		User user = userService.getUser(token);

		fcmService.sendMessage(
			user,
			token,
			NotificationType.DEFAULT,
			"오늘 소비 일기 기록해 볼까? ☺️",
			"별사탕 받고 기분 좋게 하루 마무리하자!"
		);
	}
}
