package donmani.donmani_server.fcm.schedular;

import donmani.donmani_server.fcm.service.FCMService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PushScheduler {

    private final FCMService tokenService;

    @Scheduled(cron = "0 0 21 * * *", zone = "Asia/Seoul")
    public void sendPushToToday() {
        // 9시에는 오늘 기록 없는 유저
        List<String> tokens = tokenService.getTokenNoExpenseToday();

        for (String token : tokens) {
            tokenService.sendMessage(token, "오늘 소비 일기 기록해 볼까? ☺️", "별사탕 받고 기분 좋게 하루 마무리하자!");
        }
    }

    @Scheduled(cron = "0 0 22 * * *", zone = "Asia/Seoul")
    public void sendPushToYesterday() {
        // 10시에는 어제 기록 없는 유저
        List<String> tokens = tokenService.getTokenNoExpenseYesterday();

        for (String token : tokens) {
            tokenService.sendMessage(token, "앗, 어제 소비 일기 아직 안 썼는데.. \uD83D\uDE2E️", "어제 기록은 오늘까지 쓸 수 있어. 지금 기록해 볼까?");
        }
    }
}
