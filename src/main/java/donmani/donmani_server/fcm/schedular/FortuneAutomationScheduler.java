package donmani.donmani_server.fcm.schedular;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import donmani.donmani_server.fcm.entity.FortuneGenerationTriggerType;
import donmani.donmani_server.fcm.service.FortuneAutomationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FortuneAutomationScheduler {

	private final FortuneAutomationService fortuneAutomationService;

	@Value("${fortune.automation.enabled:false}")
	private boolean enabled;

	@Scheduled(cron = "0 0 9 21 * *", zone = "Asia/Seoul")
	public void runMonthlyFortuneGeneration() {
		if (!enabled) {
			return;
		}

		try {
			fortuneAutomationService.runForNextMonth(false, FortuneGenerationTriggerType.SCHEDULER);
		} catch (Exception e) {
			log.error("월별 운세 생성 작업이 실패했습니다.", e);
		}
	}

	@Scheduled(cron = "0 0 9 25-31 * *", zone = "Asia/Seoul")
	public void runMonthlyImageGeneration() {
		if (!enabled) {
			return;
		}

		try {
			fortuneAutomationService.runApprovedImagesForNextMonth(FortuneGenerationTriggerType.SCHEDULER);
		} catch (Exception e) {
			log.error("월별 운세 이미지 생성 작업이 실패했습니다.", e);
		}
	}
}
