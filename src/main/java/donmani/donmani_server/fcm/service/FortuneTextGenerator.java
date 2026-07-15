package donmani.donmani_server.fcm.service;

import java.time.YearMonth;
import java.util.List;

import donmani.donmani_server.fcm.entity.FortuneProvider;

public interface FortuneTextGenerator {
	FortuneProvider supports();

	List<GeneratedFortunePayload> generateMonthlyFortunes(YearMonth targetMonth);
}
