package donmani.donmani_server.fcm.service;

import java.time.LocalDate;
import java.time.YearMonth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import donmani.donmani_server.fcm.entity.FortuneAiCallLog;
import donmani.donmani_server.fcm.entity.FortuneAiCallType;
import donmani.donmani_server.fcm.entity.FortuneProvider;
import donmani.donmani_server.fcm.repository.FortuneAiCallLogRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FortuneAiCallLogService {

	private static final Logger log = LoggerFactory.getLogger(FortuneAiCallLogService.class);

	private final FortuneAiCallLogRepository fortuneAiCallLogRepository;

	public void recordSuccess(
		FortuneProvider provider,
		FortuneAiCallType callType,
		YearMonth targetMonth,
		LocalDate targetDate,
		String model,
		String prompt,
		String responseText
	) {
		saveLog(provider, callType, targetMonth, targetDate, model, prompt, responseText, true, null);
	}

	public void recordFailure(
		FortuneProvider provider,
		FortuneAiCallType callType,
		YearMonth targetMonth,
		LocalDate targetDate,
		String model,
		String prompt,
		String errorMessage
	) {
		saveLog(provider, callType, targetMonth, targetDate, model, prompt, null, false, errorMessage);
	}

	private void saveLog(
		FortuneProvider provider,
		FortuneAiCallType callType,
		YearMonth targetMonth,
		LocalDate targetDate,
		String model,
		String prompt,
		String responseText,
		boolean success,
		String errorMessage
	) {
		try {
			fortuneAiCallLogRepository.save(FortuneAiCallLog.builder()
				.provider(provider)
				.callType(callType)
				.targetMonth(targetMonth)
				.targetDate(targetDate)
				.model(model)
				.prompt(prompt)
				.responseText(responseText)
				.success(success)
				.errorMessage(errorMessage)
				.build());
		} catch (Exception e) {
			log.warn("AI 호출 로그 저장에 실패했습니다.", e);
		}
	}
}
