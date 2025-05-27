package donmani.donmani_server.feedback.provider;

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
		// 활력
		templateMap.put(CategoryType.ENERGY, List.of(
			new FeedbackTemplate("오늘 텐션 충전 완료!", "이 소비로 오늘 텐션\n바뀌었겠는데! 😊"),
			new FeedbackTemplate("기분 전환 성공", "기운 빠질 땐 이런 소비\n하나쯤 괜찮지! 😆"),
			new FeedbackTemplate("분위기 메이커 등판", "잘했어, 오늘 기분전환\n제대로 했겠다 🥳"),
			new FeedbackTemplate("한숨 돌릴 타이밍", "덕분에 오늘 텐션 좀\n올라갔을 듯?"),
			new FeedbackTemplate("온도 1도 상승 중", "작은 소비 하나가\n오늘의 쉼표가 됐어!")
		));

		// 성장
		templateMap.put(CategoryType.GROWTH, List.of(
			new FeedbackTemplate("작지만 좋은 선택", "'도전'이라는 단어가\n잘 어울리는 선택이었어"),
			new FeedbackTemplate("내안의 지분", "망설이지 않고 나를 위한\n선택이라니, 잘했어 😌"),
			new FeedbackTemplate("시간이 모여 나를 만든다", "이게 스스로한테\n투자인거지 😆"),
			new FeedbackTemplate("천천히 쌓아가는 나", "하나하나 쌓다 보면,\n그게 또 보람이지 ☺️"),
			new FeedbackTemplate("속도가 다는 아니니까", "오늘 한 걸음도 꽤 괜찮았어.\n계속 이렇게만 가자 ☺️")
		));

		// 힐링
		templateMap.put(CategoryType.HEALING, List.of(
			new FeedbackTemplate("지친 나를 향한 손길", "지친 스스로를 다독이려 했던\n마음이 느껴졌어 😊"),
			new FeedbackTemplate("고요한 용기", "이건 나를 위한 선물이자\n회복이었을지도 몰라 😆"),
			new FeedbackTemplate("아무 이유 없어도 돼", "그냥 하고 싶어서 했던거,\n그걸로 충분해"),
			new FeedbackTemplate("잠깐 멈춘 덕분이야", "이거 덕분에 숨 좀 돌렸겠지?\n잘했어 ☺️"),
			new FeedbackTemplate("괜찮아, 쉬어도 돼", "별거 아니어도 잠깐 쉬어가는\n순건이 필요해! ☺️")
		));

		// 소확행
		templateMap.put(CategoryType.HAPPINESS, List.of(
			new FeedbackTemplate("이 순간이 오늘을 살렸어", "나만 아는 행복이었던 것 같아,\n그래서 더 소중해 💛"),
			new FeedbackTemplate("작지만 마음은 꽉 찼어", "크지 않아도 충분히\n가치 있는 소비였어! 😆"),
			new FeedbackTemplate("이거 하나로 웃었잖아", "정말 별거 아니었지만,\n나를 웃게 했지 😆"),
			new FeedbackTemplate("오늘, 괜히 기분 좋았어", "이런 거 하나로 기분 괜히\n좋아지잖아. 행복 했으면 됐어 ☺️"),
			new FeedbackTemplate("작아도 충분한 위로", "오늘 하루를 포근하게\n만든 소비였어 ☺️")
		));

		// 픞렉스
		templateMap.put(CategoryType.FLEX, List.of(
			new FeedbackTemplate("나를 위해 질렀다!", "이런 날도 있어야지.\n행복했으면 된 거야~ ☺️"),
			new FeedbackTemplate("후회 없는 멋진 소비", "오늘 좀 멋있었다.\n잘 샀다! 😆"),
			new FeedbackTemplate("기준 있는 선택, 칭찬해!", "고민 없이 질렀다면, 그만큼\n가치 있었던 거지 뭐 😆"),
			new FeedbackTemplate("내 마음을 위한 투자", "망설임 없이 선택했던 이유가\n무엇이었을까? ☺️"),
			new FeedbackTemplate("이 순간을 위해", "이 순간을 위해 참아온 거잖아\n잘했어! 😉")
		));

		// 품위유지
		templateMap.put(CategoryType.DIGNITY, List.of(
			new FeedbackTemplate("오늘은 나를 돌본 날", "오늘은 스스로를 다듬는\n멋진 시간이었지"),
			new FeedbackTemplate("기준 있는 선택, 칭찬해!", "선택 하나에도 너다운\n기준이 보이네 😊"),
			new FeedbackTemplate("아껴둔 나만의 센스", "지나치지 않고 딱 맞게 썼잖아\n그 센스, 누구보다 너 자신을\n위한 거였어 ✨"),
			new FeedbackTemplate("내가 나를 다듬는 시간", "‘아깝다’보다 ‘잘 챙겼다’가 먼저\n생각이 드는 날이네! ☺️"),
			new FeedbackTemplate("딱 나에게 맞게", "꼭 나에게 맞는 걸 골랐잖아\n오늘의 중심엔 내가 있었네 😊")
		));

		// 마음전달
		templateMap.put(CategoryType.AFFECTION, List.of(
			new FeedbackTemplate("이건 진심이었어", "선물보다 중요한 건,\n전달하고 싶었던 감정이었어 😊"),
			new FeedbackTemplate("소중한 누군가를 위해", "그 사람의 얼굴을 떠올리며\n고른 순간이 기억나 😆"),
			new FeedbackTemplate("기억에 남을 선택", "마음 쓰인 김에 바로 챙긴 거,\n그게 진짜 마음이지 😆"),
			new FeedbackTemplate("마음을 건넨 날", "이런 따뜻한 소비는 기억에\n오래 남아. 잘했어 ☺️"),
			new FeedbackTemplate("내 마음도 함께 담았어", "소중한 사람을 위한 선택,\n멋졌어.☺️")
		));

		// 건강
		templateMap.put(CategoryType.HEALTH, List.of(
			new FeedbackTemplate("스스로에게 준 선물", "단순한 구매가 아니라,\n나를 돌보는 결정이었어 😊"),
			new FeedbackTemplate("오늘도 나를 챙긴 하루", "스스로를 소중히 여기는\n마음이 담겨 있지 😊"),
			new FeedbackTemplate("미래의 나를 위해", "오늘 선택, 미래의 내가\n고마워할지도 😆"),
			new FeedbackTemplate("나를 아껴주는 결심", "몸도 마음도, 오늘은 진짜\n챙길 수 있었던 날! ☺️"),
			new FeedbackTemplate("나를 위한 다정한 투자", "꾸준히 스스로를 챙기는 너,\n진짜 멋져! ☺️")
		));

		// 절약
		templateMap.put(CategoryType.SAVING, List.of(
			new FeedbackTemplate("잘 아낌도 멋진 선택", "내가 나를 조율한 선택이었지,\n충분히 잘했어 😊"),
			new FeedbackTemplate("똑똑하게 소비했다", "쓸 땐 쓰고, 아낄 땐 아끼는 게\n제일 어려운 건데 잘했어! 😆"),
			new FeedbackTemplate("절약에도 기준이 있다면", "생각보다 똑똑하게 잘 고른\n소비였던 거 같지? 😆"),
			new FeedbackTemplate("이건 절제 속 만족이야", "절약에 대해 고민한 오늘,\n분명 더 단단해졌을 거야. ☺️"),
			new FeedbackTemplate("덜 쓰고도 충분했던 하루", "절약도 나를 위한 하나의 습관이\n되어가고 있어 ☺️")
		));

		// 욕심
		templateMap.put(CategoryType.GREED, List.of(
			new FeedbackTemplate("채워지지 않는 이유", "채워도 채워도 허전한 마음,\n어쩌면 다른 걸 원했던 건 아닐까?"),
			new FeedbackTemplate("채워도 공허한 이유", "진짜 필요한 건 물건이 아니라,\n감정일 수 있어"),
			new FeedbackTemplate("비워야 보이는 것도 있어", "그 순간의 ‘조금 더’는\n어떤 기분을 채우고 싶었던 걸까?"),
			new FeedbackTemplate("욕심 속에 숨은 감정", "소소한 욕심 안에도 나름의 이유가 있었을지도 몰라 🥹"),
			new FeedbackTemplate("다 채워지진 않더라", "무리하게 붙잡았던 것들, 나를\n위한 게 아니었을 수도 있어")
		));

		// 중독
		templateMap.put(CategoryType.ADDICTION, List.of(
			new FeedbackTemplate("반복하는 이유 마주하기", "같은 행동을 반복하게 만드는\n감정, 그게 뭘까?"),
			new FeedbackTemplate("익숙함에 속지말기", "무언가를 계속 채우려는 마음,\n그 안에 진짜 필요는 뭐였을까?"),
			new FeedbackTemplate("물건이 아냐", "이게 정말 필요한 건지, 익숙해서 그런 건지 돌아봐도 좋겠다 🥹"),
			new FeedbackTemplate("벗어나고 싶다면", "무의식적으로 반복된 행동,\n의식하면 다르게 보이기도 해"),
			new FeedbackTemplate("내가 왜 그랬을까", "무심히 지나친 선택들,\n그 이유를 마주해봐도 좋아")
		));

		// 게으름
		templateMap.put(CategoryType.LAZINESS, List.of(
			new FeedbackTemplate("하루를 놓쳤던 이유", "조금만 일찍 움직였더라면,\n이 소비는 달라졌을까? 🥹"),
			new FeedbackTemplate("조금만 움직였더라면", "다음엔 작은 것이라도\n바꿔보면 결과가 달라질 수\n있을거야! "),
			new FeedbackTemplate("미룸이 남긴 마음", "미룬 선택이 결국 스스로를 더\n피곤하게 만들었을지도 몰라"),
			new FeedbackTemplate("선택을 놓친 순간", "그때 왜 이런 선택을 했을까,\n하루의 선택을 떠올려봐도 좋겠다"),
			new FeedbackTemplate("잠깐의 멈춤", "게으름이라고 부르기 전에,\n왜 그랬는지 한 번 돌아봐도 좋아")
		));

		// 충동
		templateMap.put(CategoryType.IMPULSE, List.of(
			new FeedbackTemplate("그땐 왜 그랬을까", "그 순간 뭐가 날 그렇게 움직이게 했을까? 생각해보자 🤔"),
			new FeedbackTemplate("급했을지도 몰라", "즉흥적인 선택 뒤엔 종종 생각하지 못한 감정이 따라와 🥲"),
			new FeedbackTemplate("멈춤도 선택할 수 있지", "다음엔 그 감정이 올라올 때 한\n박자만 쉬어봐도 좋을 것 같아!"),
			new FeedbackTemplate("마음은 알고 있어", "잠깐 멈췄다면, 마음은 뭐라고\n말했을까?"),
			new FeedbackTemplate("감정 앞에선 흐려져", "빠르게 결정한 만큼,\n금방 후회한 건 아니었을까?")
		));

		// 무의미
		templateMap.put(CategoryType.MEANINGLESSNESS, List.of(
			new FeedbackTemplate("그떄를 떠올려봐", "그 시간을 통해 나는 무엇을\n느꼈을까, 가만히 떠올려봐"),
			new FeedbackTemplate("의미가 있었을까", "무의미함 뒤에 감춰진 마음,\n혹시 놓치고 있던 건 아닐까? 🤔"),
			new FeedbackTemplate("의미가 있었던 걸까", "기대했던 감정은 왜 오지\n않았을까, 그 이유를 생각해보면 도움 될지도 몰라"),
			new FeedbackTemplate("습관처럼 했던 선택", "이유 없이 고른 것 같은 소비,\n정말 이유가 없었을까 🤔"),
			new FeedbackTemplate("무슨 감정이었을까", "반복된 일상 속, 뭔가를 채우고\n싶었던 마음이었을지도 몰라")
		));

		// 과시
		templateMap.put(CategoryType.BOASTFULNESS, List.of(
			new FeedbackTemplate("누구를 위한 거였지", "이건 정말 나를 위한 소비였을까,\n아니면 누군가의 시선을 위한\n선택이었을까?"),
			new FeedbackTemplate("날 위한 게 맞을까", "갖고 싶었던 건 물건일까, 아니면\n그걸 가진 ‘내 모습’이었을까? 🧐"),
			new FeedbackTemplate("불안이었을까", "결국 중요한 건, 남이 아니라\n내 눈에 괜찮은 게 중요해"),
			new FeedbackTemplate("비교는 나를 흐려", "비교의 눈에 맞추다 보면,\n나다운 건 점점 사라질 수 있어"),
			new FeedbackTemplate("결국 난 어디 있었지", "나를 드러내고 싶었던 건지,\n감추고 싶었던 건지 곰곰히\n생각해봐")
		));

		// 습관반복
		templateMap.put(CategoryType.HABIT, List.of(
			new FeedbackTemplate("지금은 달라도 돼", "지금의 나는 예전과 같은 패턴이\n필요하지 않을 수도 있어"),
			new FeedbackTemplate("익숙해서 놓쳤을지도", "익숙해서 해버린 것들,\n익숙하다고 괜찮은 건 아니야 🥲"),
			new FeedbackTemplate("반복은 늘 조용하지", "습관처럼 이어진 소비, 어쩌면\n무뎌진 감각의 신호일 수 있어"),
			new FeedbackTemplate("괜찮아서 계속했을까", "아무 생각 없이 또 했던 행동,\n어쩌면 그게 제일 위험할 수 있어"),
			new FeedbackTemplate("지금 멈춰도 괜찮아", "나도 모르게 같은 걸 택했다면,\n지금이 멈출 타이밍일지도 몰라")
		));

		// 과한절약
		templateMap.put(CategoryType.OVERFRUGALITY, List.of(
			new FeedbackTemplate("덜 쓴다고 다 좋진 않아", "지혜로운 소비는 나를 존중하는\n방법일 수 있어! 🥲"),
			new FeedbackTemplate("이건 절약이었을까", "과하게 절약하다 마음까지 조인 건\n아니었는지 생각해봐"),
			new FeedbackTemplate("절약은 균형에서부터", "절약도 과하지 않도록\n조율해보는 거야! 🧐"),
			new FeedbackTemplate("잘 참은 건 맞을까", "오늘 참은 이유,\n너를 위한 거였으면 좋겠다! 🥺"),
			new FeedbackTemplate("참고도 후회했어", "너무 아끼다가 나까지\n아껴버리진 않았는지 돌아봐")
		));

		// 선택미스
		templateMap.put(CategoryType.MISS, List.of(
			new FeedbackTemplate("선택도 배움이었어", "선택에서 또 배울 수 있는게\n있을거야"),
			new FeedbackTemplate("그때는 몰랐던 거야", "실수였다고 느껴진다면, 그만큼\n내 기준이 생겼다는 뜻이야"),
			new FeedbackTemplate("덕분에 더 알게 됐어", "미스였다고 자책하지 말자\n정답은 늘 뒤에 오는 법이니까 🧐"),
			new FeedbackTemplate("실수에서 찾은 기준", "이 경험 덕분에 다음 선택은\n조금 더 선명해질 거야"),
			new FeedbackTemplate("이건 나를 위한 과정", "실수한 날 자세히 기록해두자\n그래야 배운 과정이 보여\n")
		));
	}

	public List<FeedbackTemplate> getTemplates(CategoryType category) {
		return templateMap.getOrDefault(category, List.of());
	}
}
