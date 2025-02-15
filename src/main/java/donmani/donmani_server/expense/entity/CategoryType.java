package donmani.donmani_server.expense.entity;

public enum CategoryType {
    // 좋은 소비
	ENERGY, // 활력
	GROWTH, // 성장
	HEALING, // 힐링
	HAPPINESS, // 소확행
	FLEX, // 플렉스
	DIGNITY, // 품위 유지
	AFFECTION, // 마음 전달
	HEALTH, // 건강

	// 2.나쁜 소비
	GREED, // 욕심
	ADDICTION, // 중독
	LAZINESS, // 게으름
	IMPULSE, // 충동
	MEANINGLESSNESS, // 무의미
	BOASTFULNESS, // 과시
	HABIT, // 습관 반복
	OVERFRUGALITY, // 과한 절약

	// 좋은 소비, 나쁜 소비 공통
	NONE // 카테고리 없음
}