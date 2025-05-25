package donmani.donmani_server.feedback.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import donmani.donmani_server.expense.entity.CategoryType;

@Component
public class FeedbackTemplateProvider {
	private Map<CategoryType, List<FeedbackTemplate>> templateMap = new HashMap<>();

	@PostConstruct
	public void init() {
		templateMap.put(CategoryType.ENERGY, List.of(
			new FeedbackTemplate("에너지 넘치는 하루!", "당신의 소비는 오늘도 활력을 줍니다."),
			new FeedbackTemplate("좋은 시작", "에너지 충전 완료!"),
			new FeedbackTemplate("활기찬 소비", "활력이 느껴지는 소비네요."),
			new FeedbackTemplate("에너지 UP", "기분 좋은 에너지가 전달돼요."),
			new FeedbackTemplate("힘이 나는 하루", "당신의 선택이 빛납니다.")
		));
	}

	public List<FeedbackTemplate> getTemplates(CategoryType category) {
		return templateMap.getOrDefault(category, List.of());
	}
}
