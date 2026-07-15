package donmani.donmani_server.fcm.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import donmani.donmani_server.fcm.entity.Fortune;

class FortunePromptServiceTest {

	@Test
	void buildMonthlyPromptAppendsDbOutputContractToConfiguredPrompt() {
		FortunePromptService promptService = new FortunePromptService();
		ReflectionTestUtils.setField(promptService, "textPromptTemplate", "private prompt for {{TARGET_MONTH_LABEL}}\\nsecond line");

		String prompt = promptService.buildMonthlyPrompt(YearMonth.of(2026, 8));

		assertThat(prompt).contains("private prompt for 2026년 8월");
		assertThat(prompt).contains("2026년 8월" + System.lineSeparator() + "second line");
		assertThat(prompt).contains("fortune DB 스키마와 컬럼 매핑");
		assertThat(prompt).contains("target_date: 운세 대상 날짜, yyyy-MM-dd 형식");
		assertThat(prompt).contains("title=7월 27일, 오늘의 운세가 도착했어요!");
		assertThat(prompt).contains("반드시 31개의 결과를 생성");
		assertThat(prompt).contains("2026-08-01부터 2026-08-31까지");
		assertThat(prompt).contains("\"fortunes\"");
	}

	@Test
	void buildImagePromptUsesRabbitTemplateAndFortuneContext() {
		FortunePromptService promptService = new FortunePromptService();
		Fortune fortune = Fortune.builder()
			.targetDate(LocalDate.of(2026, 8, 1))
			.title("8월 1일, 오늘의 운세가 도착했어요!")
			.subtitle("새로운 기회가 가볍게 다가와요")
			.content("작은 시도가 하루의 흐름을 바꿀 수 있어요.")
			.item("행운의 물건 : 작은 열쇠")
			.build();

		String prompt = promptService.buildImagePrompt(fortune);

		assertThat(prompt).contains("exact character identity");
		assertThat(prompt).contains("same small white bunny's appearance");
		assertThat(prompt).contains("Do not redesign the bunny");
		assertThat(prompt).contains("{Symbolic_Element}");
		assertThat(prompt).contains("Use the fortune context below to infer");
		assertThat(prompt).contains("Target date: 2026-08-01");
		assertThat(prompt).contains("새로운 기회가 가볍게 다가와요");
		assertThat(prompt).contains("행운의 물건 : 작은 열쇠");
	}
}
