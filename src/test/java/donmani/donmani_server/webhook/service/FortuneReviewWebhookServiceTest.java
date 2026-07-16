package donmani.donmani_server.webhook.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import donmani.donmani_server.fcm.entity.Fortune;
import donmani.donmani_server.fcm.entity.FortuneProvider;

class FortuneReviewWebhookServiceTest {

	private final FortuneReviewWebhookService webhookService = new FortuneReviewWebhookService(
		WebClient.builder().build(),
		new ObjectMapper()
	);

	@Test
	void sendMonthlyFortuneImagesRequiresFortuneWebhookUrl() {
		assertThatThrownBy(() -> webhookService.sendMonthlyFortuneImages(List.of()))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("discord.fortune.webhook.url 설정이 필요합니다.");
	}

	@Test
	void buildMonthlyFortuneImageMessagesUsesDateAndEmbedImageOnly() {
		Fortune fortune = fortune(1);

		List<Map<String, Object>> messages = webhookService.buildMonthlyFortuneImageMessages(List.of(fortune));

		assertThat(messages).hasSize(1);
		List<?> embeds = (List<?>)messages.get(0).get("embeds");
		assertThat(embeds).hasSize(1);

		@SuppressWarnings("unchecked")
		Map<String, Object> embed = (Map<String, Object>)embeds.get(0);
		assertThat(embed.get("title")).isEqualTo("2026-08-01");
		assertThat(embed).containsKey("image");
		assertThat(embed).doesNotContainKeys("description", "fields", "thumbnail");
		assertThat(embed.toString())
			.doesNotContain("subtitle-1")
			.doesNotContain("content-1")
			.doesNotContain("item-1");

		@SuppressWarnings("unchecked")
		Map<String, Object> image = (Map<String, Object>)embed.get("image");
		assertThat(image.get("url")).isEqualTo("https://example.com/2026-08-01.png");
	}

	@Test
	void buildMonthlyFortuneImageMessagesSplitsEveryTenEmbeds() {
		List<Fortune> fortunes = IntStream.rangeClosed(1, 11)
			.mapToObj(this::fortune)
			.toList();

		List<Map<String, Object>> messages = webhookService.buildMonthlyFortuneImageMessages(fortunes);

		assertThat(messages).hasSize(2);
		assertThat((List<?>)messages.get(0).get("embeds")).hasSize(10);
		assertThat((List<?>)messages.get(1).get("embeds")).hasSize(1);
	}

	@Test
	void buildMonthlyFortuneReviewWithImageMessagesIncludesTextAndImage() {
		Fortune fortune = fortune(1);

		List<Map<String, Object>> messages = webhookService.buildMonthlyFortuneReviewWithImageMessages(List.of(fortune));

		assertThat(messages).hasSize(1);
		List<?> embeds = (List<?>)messages.get(0).get("embeds");
		assertThat(embeds).hasSize(1);

		@SuppressWarnings("unchecked")
		Map<String, Object> embed = (Map<String, Object>)embeds.get(0);
		assertThat(embed.get("title")).isEqualTo("2026-08-01");
		assertThat((String)embed.get("description"))
			.contains("subtitle-1")
			.contains("content-1")
			.contains("item-1")
			.doesNotContain("부제")
			.doesNotContain("본문")
			.doesNotContain("아이템");

		@SuppressWarnings("unchecked")
		Map<String, Object> image = (Map<String, Object>)embed.get("image");
		assertThat(image.get("url")).isEqualTo("https://example.com/2026-08-01.png");
	}

	@Test
	void buildMonthlyFortuneReviewWithImageMessagesIncludesTextWhenImageIsMissing() {
		Fortune fortune = fortuneWithoutImage(1);

		List<Map<String, Object>> messages = webhookService.buildMonthlyFortuneReviewWithImageMessages(List.of(fortune));

		@SuppressWarnings("unchecked")
		Map<String, Object> embed = (Map<String, Object>)((List<?>)messages.get(0).get("embeds")).get(0);
		assertThat(embed.get("title")).isEqualTo("2026-08-01");
		assertThat((String)embed.get("description"))
			.contains("subtitle-1")
			.contains("content-1")
			.contains("item-1")
			.doesNotContain("부제")
			.doesNotContain("본문")
			.doesNotContain("아이템");
		assertThat(embed).doesNotContainKey("image");
	}

	@Test
	void buildMonthlyFortuneReviewWithImageMessagesSplitsEveryTenEmbeds() {
		List<Fortune> fortunes = IntStream.rangeClosed(1, 11)
			.mapToObj(this::fortune)
			.toList();

		List<Map<String, Object>> messages = webhookService.buildMonthlyFortuneReviewWithImageMessages(fortunes);

		assertThat(messages).hasSize(2);
		assertThat((List<?>)messages.get(0).get("embeds")).hasSize(10);
		assertThat((List<?>)messages.get(1).get("embeds")).hasSize(1);
	}

	private Fortune fortune(int day) {
		Fortune fortune = fortuneWithoutImage(day);
		LocalDate targetDate = fortune.getTargetDate();
		fortune.updateImage(
			"fortune_content/2026-08/%s.png".formatted(targetDate),
			"https://example.com/%s.png".formatted(targetDate),
			"prompt-" + day,
			FortuneProvider.GEMINI
		);
		return fortune;
	}

	private Fortune fortuneWithoutImage(int day) {
		LocalDate targetDate = LocalDate.of(2026, 8, day);
		return Fortune.builder()
			.targetDate(targetDate)
			.title("title-" + day)
			.subtitle("subtitle-" + day)
			.content("content-" + day)
			.item("item-" + day)
			.build();
	}
}
