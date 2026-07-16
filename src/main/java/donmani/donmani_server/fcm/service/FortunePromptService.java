package donmani.donmani_server.fcm.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import donmani.donmani_server.fcm.entity.Fortune;

@Service
public class FortunePromptService {

	private static final DateTimeFormatter TARGET_MONTH_LABEL = DateTimeFormatter.ofPattern("yyyy년 M월");

	@Value("${fortune.automation.prompt.text-template:}")
	private String textPromptTemplate;

	@Value("${fortune.automation.prompt.image-template:}")
	private String imagePromptTemplate;

	public String buildMonthlyPrompt(YearMonth targetMonth) {
		String template = resolveRequiredInlineTemplate(
			textPromptTemplate,
			"fortune.automation.prompt.text-template 설정이 필요합니다."
		);
		return applyMonthlyVariables(template, targetMonth);
	}

	private String applyMonthlyVariables(
		String template,
		YearMonth targetMonth
	) {
		return template
			.replace("{{TARGET_MONTH}}", targetMonth.toString())
			.replace("{{TARGET_MONTH_LABEL}}", targetMonth.atDay(1).format(TARGET_MONTH_LABEL))
			.replace("{{DAY_COUNT}}", String.valueOf(targetMonth.lengthOfMonth()))
			.replace("{{START_DATE}}", targetMonth.atDay(1).toString())
			.replace("{{END_DATE}}", targetMonth.atEndOfMonth().toString());
	}

	public String buildImagePrompt(Fortune fortune) {
		String template = resolveRequiredInlineTemplate(
			imagePromptTemplate,
			"fortune.automation.prompt.image-template 설정이 필요합니다."
		);
		return applyImageVariables(template, fortune);
	}

	private String applyImageVariables(
		String template,
		Fortune fortune
	) {
		return template
			.replace("{{TARGET_DATE}}", fortune.getTargetDate().toString())
			.replace("{{TITLE}}", fortune.getTitle())
			.replace("{{SUBTITLE}}", fortune.getSubtitle())
			.replace("{{CONTENT}}", fortune.getContent())
			.replace("{{ITEM}}", fortune.getItem());
	}

	public List<GeneratedFortunePayload> validateFortunes(
		YearMonth targetMonth,
		List<GeneratedFortunePayload> fortunes
	) {
		if (fortunes == null || fortunes.size() != targetMonth.lengthOfMonth()) {
			throw new IllegalStateException("생성된 운세 개수가 대상 월 일수와 맞지 않습니다.");
		}

		Set<LocalDate> dates = new HashSet<>();
		List<GeneratedFortunePayload> validated = new ArrayList<>();

		for (GeneratedFortunePayload fortune : fortunes) {
			if (fortune.targetDate() == null || !YearMonth.from(fortune.targetDate()).equals(targetMonth)) {
				throw new IllegalStateException("응답에 대상 월과 맞지 않는 날짜가 포함되어 있습니다.");
			}
			if (!dates.add(fortune.targetDate())) {
				throw new IllegalStateException("응답에 중복 날짜가 포함되어 있습니다.");
			}
			if (!hasText(fortune.title()) || !hasText(fortune.subtitle()) || !hasText(fortune.content()) || !hasText(fortune.item())) {
				throw new IllegalStateException("응답에 비어 있는 운세 필드가 포함되어 있습니다.");
			}
			validated.add(fortune);
		}

		validated.sort((left, right) -> left.targetDate().compareTo(right.targetDate()));
		return validated;
	}

	private String resolveRequiredInlineTemplate(
		String inlineTemplate,
		String errorMessage
	) {
		if (hasText(inlineTemplate)) {
			return normalizeInlineTemplate(inlineTemplate);
		}

		throw new IllegalStateException(errorMessage);
	}

	private String normalizeInlineTemplate(String inlineTemplate) {
		return inlineTemplate.replace("\\n", System.lineSeparator());
	}

	private boolean hasText(String value) {
		return StringUtils.hasText(value);
	}
}
