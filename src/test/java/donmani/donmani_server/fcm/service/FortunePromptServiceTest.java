package donmani.donmani_server.fcm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import donmani.donmani_server.fcm.entity.Fortune;

class FortunePromptServiceTest {

	@Test
	void buildMonthlyPromptUsesConfiguredPromptOnlyWithVariables() {
		FortunePromptService promptService = new FortunePromptService();
		ReflectionTestUtils.setField(promptService, "textPromptTemplate", "private prompt for {{TARGET_MONTH_LABEL}}\\nsecond line");

		String prompt = promptService.buildMonthlyPrompt(YearMonth.of(2026, 8));

		assertThat(prompt).contains("private prompt for 2026년 8월");
		assertThat(prompt).contains("2026년 8월" + System.lineSeparator() + "second line");
		assertThat(prompt).doesNotContain("fortune DB 스키마와 컬럼 매핑");
		assertThat(prompt).doesNotContain("최종 출력 규칙");
	}

	@Test
	void buildMonthlyPromptAppliesTargetMonthVariables() {
		FortunePromptService promptService = new FortunePromptService();
		ReflectionTestUtils.setField(
			promptService,
			"textPromptTemplate",
			"대상 월 {{TARGET_MONTH_LABEL}} {{TARGET_MONTH}} {{START_DATE}} {{END_DATE}} {{DAY_COUNT}}"
		);

		String prompt = promptService.buildMonthlyPrompt(YearMonth.of(2026, 12));

		assertThat(prompt).contains("2026년 12월");
		assertThat(prompt).contains("2026-12");
		assertThat(prompt).contains("2026-12-01");
		assertThat(prompt).contains("2026-12-31");
		assertThat(prompt).contains("31");
		assertThat(prompt).doesNotContain("{{TARGET_MONTH}}");
	}

	@Test
	void testPromptPropertiesContainTargetRulesWithoutFixedJulyDate() throws Exception {
		String properties = Files.readString(Path.of("src/test/resources/application-test.properties"));

		assertThat(properties).contains("{{TARGET_MONTH}}");
		assertThat(properties).contains("{{START_DATE}}");
		assertThat(properties).contains("{{END_DATE}}");
		assertThat(properties).contains("{{DAY_COUNT}}");
		assertThat(properties).doesNotContain("\"targetDate\":\"2026-07-01\"");
		assertThat(properties).doesNotContain("\"targetDate\": \"2026-07-01\"");
	}

	@Test
	void buildMonthlyPromptRequiresConfiguredPrompt() {
		FortunePromptService promptService = new FortunePromptService();

		assertThatThrownBy(() -> promptService.buildMonthlyPrompt(YearMonth.of(2026, 8)))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("fortune.automation.prompt.text-template");
	}

	@Test
	void buildImagePromptUsesConfiguredPromptOnlyWithVariables() {
		FortunePromptService promptService = new FortunePromptService();
		ReflectionTestUtils.setField(
			promptService,
			"imagePromptTemplate",
			"image prompt {{TARGET_DATE}} {{TITLE}} {{SUBTITLE}} {{CONTENT}} {{ITEM}}"
		);
		Fortune fortune = Fortune.builder()
			.targetDate(LocalDate.of(2026, 8, 1))
			.title("8월 1일, 오늘의 운세가 도착했어요!")
			.subtitle("새로운 기회가 가볍게 다가와요")
			.content("작은 시도가 하루의 흐름을 바꿀 수 있어요.")
			.item("행운의 물건 : 작은 열쇠")
			.build();

		String prompt = promptService.buildImagePrompt(fortune);

		assertThat(prompt).contains("image prompt 2026-08-01");
		assertThat(prompt).contains("8월 1일, 오늘의 운세가 도착했어요!");
		assertThat(prompt).contains("새로운 기회가 가볍게 다가와요");
		assertThat(prompt).contains("행운의 물건 : 작은 열쇠");
		assertThat(prompt).doesNotContain("exact character identity");
		assertThat(prompt).doesNotContain("Use the fortune context below to infer");
	}

	@Test
	void buildImagePromptRequiresConfiguredPrompt() {
		FortunePromptService promptService = new FortunePromptService();

		assertThatThrownBy(() -> promptService.buildImagePrompt(fortune()))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("fortune.automation.prompt.image-template");
	}

	private Fortune fortune() {
		return Fortune.builder()
			.targetDate(LocalDate.of(2026, 8, 1))
			.title("8월 1일, 오늘의 운세가 도착했어요!")
			.subtitle("새로운 기회가 가볍게 다가와요")
			.content("작은 시도가 하루의 흐름을 바꿀 수 있어요.")
			.item("행운의 물건 : 작은 열쇠")
			.build();
	}
}
