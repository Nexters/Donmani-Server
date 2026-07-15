package donmani.donmani_server.fcm.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
	private static final String DEFAULT_TEXT_PROMPT_TEMPLATE = """
		너는 매일 아침 모바일 앱 사용자에게 운세를 전하는 한국어 운세 콘텐츠 에디터다.
		{{TARGET_MONTH_LABEL}} 한 달치 일일 운세를 만들어줘.

		톤 가이드:
		- 20~30대 사용자가 매일 아침 가볍게 읽을 수 있는 한국어
		- 진지한 점술보다 하루의 분위기를 다정하게 안내하는 톤
		- 단정 짓지 않고 가능성의 언어를 사용
		- 해요체를 사용
		""";
	private static final String MONTHLY_TEXT_OUTPUT_CONTRACT_TEMPLATE = """

		월별 생성 조건:
		- 대상 월: {{TARGET_MONTH_LABEL}} ({{TARGET_MONTH}})
		- 날짜 범위: {{START_DATE}}부터 {{END_DATE}}까지
		- 반드시 {{DAY_COUNT}}개의 결과를 생성
		- 날짜는 하루도 빠짐없이 모두 포함

		fortune DB 스키마와 컬럼 매핑:
		- target_date: 운세 대상 날짜, yyyy-MM-dd 형식
		- title: "{M월 D일}, 오늘의 운세가 도착했어요!" 형식의 고정 제목
		- subtitle: 푸시 알림처럼 호기심을 주는 한 문장, 문장 끝 이모지 1개 포함
		- content: 운세 본문, 분위기 환기 + 핵심 메시지 + 여운의 흐름을 가진 한글 3문장
		- item: "행운의 {행운 요소 카테고리} : {행운 요소 값}" 형식

		기존 저장 형식 예시:
		- target_date=2026-07-27, title=7월 27일, 오늘의 운세가 도착했어요!, subtitle=순리대로 흐르는 편안함을 느껴보세요 🌊, item=행운의 사람 : 이어폰을 끼고 걷는 사람
		- target_date=2026-07-28, title=7월 28일, 오늘의 운세가 도착했어요!, subtitle=뜻밖의 행운이 당신의 하루를 빛내요 ✨, item=행운의 금액 : 7,700원
		- target_date=2026-07-29, title=7월 29일, 오늘의 운세가 도착했어요!, subtitle=지나친 열정보다는 적당한 조율이 필요해요 ⚖️, item=행운의 행동 : 창문을 열어 환기를 시켜보세요.
		- target_date=2026-07-30, title=7월 30일, 오늘의 운세가 도착했어요!, subtitle=소소한 성취감이 나를 채우는 하루예요 🥎, item=행운의 초성 : ㅅㄹ
		- target_date=2026-07-31, title=7월 31일, 오늘의 운세가 도착했어요!, subtitle=한 달의 마무리가 완벽하게 빛나요 🏆, item=행운의 색 : 하늘색

		최종 출력 규칙:
		- JSON만 반환하고 설명 문장은 절대 포함하지 않음
		- fortunes 배열 길이는 반드시 {{DAY_COUNT}}
		- targetDate는 yyyy-MM-dd 형식이며 {{START_DATE}}부터 {{END_DATE}}까지 오름차순
		- title/subtitle/item은 255자 이내
		- content는 한글 3문장
		- item은 반드시 "행운의 {카테고리} : {값}" 형식

		JSON 스키마:
		{
		  "fortunes": [
		    {
		      "targetDate": "yyyy-MM-dd",
		      "title": "string",
		      "subtitle": "string",
		      "content": "string",
		      "item": "string"
		    }
		  ]
		}
		""";
	private static final String DEFAULT_IMAGE_PROMPT_TEMPLATE = """
		Use the attached reference image as the exact character identity. Preserve the same small white bunny's appearance: round face and body, smooth white 3D material, two rounded upright ears, simple glossy black oval eyes with tiny white highlights, small black w-shaped mouth, soft pink cheek tint, short rounded arms and legs, and the same cute minimal toy-like proportions. Do not redesign the bunny into another animal, mascot, or realistic rabbit. Do not change the face shape, eye style, mouth style, body proportions, or overall character silhouette.

		Create a high-quality, cute 3D render of this same bunny interacting with a {Symbolic_Element} (e.g., a stylized four-leaf clover, a compass, or a small crystal formation) that visually represents the core message of the fortune. Its pose is {Daily_Theme_Pose} (e.g., contemplative, energetic, relaxed) to align with the theme while keeping the bunny's original identity recognizable. The background elements are adjusted to include subtle, decorative symbols related to the fortune, such as {Background_Theme_Icons} (e.g., a tiny key for 'new opportunity' or a delicate cloud for 'peace'). The composition remains wide, providing ample negative space for flexibility to crop to a 4:3 aspect ratio without losing key elements. There is no text in image. The lighting is warm, enhancing textures. High detail, 3D render, Pixar style, aesthetic, pastel tones, 8k resolution.
		""";

	@Value("${fortune.automation.prompt.text-template:}")
	private String textPromptTemplate;

	@Value("${fortune.automation.prompt.image-template:}")
	private String imagePromptTemplate;

	@Value("${fortune.automation.prompt.image-template-path:}")
	private String imagePromptTemplatePath;

	public String buildMonthlyPrompt(YearMonth targetMonth) {
		String template = resolveInlineTemplate(textPromptTemplate, DEFAULT_TEXT_PROMPT_TEMPLATE);
		return applyMonthlyVariables(template + MONTHLY_TEXT_OUTPUT_CONTRACT_TEMPLATE, targetMonth);
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
		String template = resolvePromptTemplate(imagePromptTemplatePath, imagePromptTemplate, DEFAULT_IMAGE_PROMPT_TEMPLATE);
		return applyImageVariables(template, fortune) + buildImageFortuneContext(fortune);
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

	private String buildImageFortuneContext(Fortune fortune) {
		return """

			Use the fortune context below to infer {Symbolic_Element}, {Daily_Theme_Pose}, and {Background_Theme_Icons}. Do not render any letters, words, numbers, captions, or UI text in the image.

			Fortune context:
			- Target date: %s
			- Title: %s
			- Subtitle: %s
			- Content: %s
			- Lucky point: %s
			""".formatted(
			fortune.getTargetDate(),
			fortune.getTitle(),
			fortune.getSubtitle(),
			fortune.getContent(),
			fortune.getItem()
		);
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

	private String resolvePromptTemplate(
		String templatePath,
		String inlineTemplate,
		String defaultTemplate
	) {
		if (hasText(templatePath)) {
			try {
				return Files.readString(Path.of(templatePath));
			} catch (IOException e) {
				throw new IllegalStateException("프롬프트 파일을 읽지 못했습니다: " + templatePath, e);
			}
		}

		if (hasText(inlineTemplate)) {
			return normalizeInlineTemplate(inlineTemplate);
		}

		return defaultTemplate;
	}

	private String resolveInlineTemplate(
		String inlineTemplate,
		String defaultTemplate
	) {
		if (hasText(inlineTemplate)) {
			return normalizeInlineTemplate(inlineTemplate);
		}

		return defaultTemplate;
	}

	private String normalizeInlineTemplate(String inlineTemplate) {
		return inlineTemplate.replace("\\n", System.lineSeparator());
	}

	private boolean hasText(String value) {
		return StringUtils.hasText(value);
	}
}
