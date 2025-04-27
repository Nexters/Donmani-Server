package donmani.donmani_server.webhook.controller;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import donmani.donmani_server.webhook.service.WebHookService;
import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
public class WebHookController {
	private final WebHookService webHookService;

	@Scheduled(cron = "0 0 9 * * ?", zone = "Asia/Seoul") // 매일 오전 9시에 호출
	@PostMapping("api/v1/test/")
	public void sendDailyUserStatsReport() {
		webHookService.sendDailyUserStatsReport();
	}
}
