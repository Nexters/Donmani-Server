package donmani.donmani_server.webhook.service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import donmani.donmani_server.fcm.entity.Fortune;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FortuneReviewWebhookService {

	private static final int REVIEW_COLOR = 0x52C41A;
	private static final int IMAGE_COLOR = 0x00BFFF;
	private static final int REVIEW_WITH_IMAGE_COLOR = 0x9B59B6;
	private static final int EMBED_DESCRIPTION_LIMIT = 3500;
	private static final int IMAGE_EMBEDS_PER_MESSAGE = 10;

	private final WebClient webClient;
	private final ObjectMapper objectMapper;

	@Value("${discord.fortune.webhook.url:}")
	private String webhookUrl;

	// 월간 운세 텍스트 검수본을 한두 개의 긴 description embed로 묶어 보낸다.
	public void sendMonthlyFortuneReview(
		YearMonth targetMonth,
		List<Fortune> fortunes
	) {
		assertWebhookConfigured();
		for (Map<String, Object> message : buildMonthlyFortunes(
			"\uD83C\uDF40 " + targetMonth + " 운세 생성 완료",
			"",
			fortunes,
			REVIEW_COLOR
		)) {
			post(message);
		}
	}

	// 이미지 검수용 웹훅: imageUrl이 있는 운세만 날짜별 이미지 embed로 보낸다.
	public void sendMonthlyFortuneImages(List<Fortune> fortunes) {
		assertWebhookConfigured();
		for (Map<String, Object> message : buildMonthlyFortuneImageMessages(fortunes)) {
			post(message);
		}
	}

	// 합본 검수용 웹훅: 날짜별 텍스트와 이미지가 같은 embed에 보이도록 보낸다.
	public void sendMonthlyFortuneReviewWithImages(List<Fortune> fortunes) {
		assertWebhookConfigured();
		for (Map<String, Object> message : buildMonthlyFortuneReviewWithImageMessages(fortunes)) {
			post(message);
		}
	}

	List<Map<String, Object>> buildMonthlyFortuneReviewWithImageMessages(List<Fortune> fortunes) {
		List<Map<String, Object>> messages = new ArrayList<>();
		List<Map<String, Object>> embeds = new ArrayList<>();

		for (Fortune fortune : fortunes) {
			embeds.add(buildFortuneReviewWithImageEmbed(fortune));
			if (embeds.size() == IMAGE_EMBEDS_PER_MESSAGE) {
				// Discord는 한 메시지에 embed를 최대 10개까지만 허용한다.
				messages.add(Map.of("embeds", embeds));
				embeds = new ArrayList<>();
			}
		}

		if (!embeds.isEmpty()) {
			messages.add(Map.of("embeds", embeds));
		}
		return messages;
	}

	List<Map<String, Object>> buildMonthlyFortuneImageMessages(List<Fortune> fortunes) {
		List<Map<String, Object>> messages = new ArrayList<>();
		List<Map<String, Object>> embeds = new ArrayList<>();

		for (Fortune fortune : fortunes) {
			if (!StringUtils.hasText(fortune.getImageUrl())) {
				continue;
			}
			embeds.add(buildFortuneImageEmbed(fortune));
			if (embeds.size() == IMAGE_EMBEDS_PER_MESSAGE) {
				// Discord는 한 메시지에 embed를 최대 10개까지만 허용한다.
				messages.add(Map.of("embeds", embeds));
				embeds = new ArrayList<>();
			}
		}

		if (!embeds.isEmpty()) {
			messages.add(Map.of("embeds", embeds));
		}
		return messages;
	}

	private Map<String, Object> buildFortuneImageEmbed(Fortune fortune) {
		Map<String, Object> embed = new LinkedHashMap<>();
		embed.put("title", fortune.getTargetDate().toString());
		embed.put("color", IMAGE_COLOR);
		embed.put("image", Map.of("url", fortune.getImageUrl()));
		return embed;
	}

	private Map<String, Object> buildFortuneReviewWithImageEmbed(Fortune fortune) {
		Map<String, Object> embed = new LinkedHashMap<>();
		embed.put("title", fortune.getTargetDate().toString());
		embed.put("description", buildFortuneReviewWithImageDescription(fortune));
		embed.put("color", REVIEW_WITH_IMAGE_COLOR);
		if (StringUtils.hasText(fortune.getImageUrl())) {
			embed.put("image", Map.of("url", fortune.getImageUrl()));
		}
		return embed;
	}

	private String buildFortuneReviewWithImageDescription(Fortune fortune) {
		return truncate("""
			%s
			%s
			%s
			""".formatted(
			truncate(fortune.getSubtitle(), 400),
			truncate(fortune.getContent(), 1800),
			truncate(fortune.getItem(), 400)
		).strip(), EMBED_DESCRIPTION_LIMIT);
	}

	private List<Map<String, Object>> buildMonthlyFortunes(
		String title,
		String description,
		List<Fortune> fortunes,
		int color
	) {
		List<String> chunks = buildFortuneChunks(description, fortunes);
		List<Map<String, Object>> messages = new ArrayList<>();
		for (int i = 0; i < chunks.size(); i++) {
			Map<String, Object> embed = new LinkedHashMap<>();
			embed.put("title", buildChunkTitle(title, i, chunks.size()));
			embed.put("description", chunks.get(i));
			embed.put("color", color);

			messages.add(Map.of("embeds", List.of(embed)));
		}
		return messages;
	}

	private List<String> buildFortuneChunks(
		String header,
		List<Fortune> fortunes
	) {
		List<String> chunks = new ArrayList<>();
		StringBuilder chunk = new StringBuilder(truncate(header, EMBED_DESCRIPTION_LIMIT));

		for (Fortune fortune : fortunes) {
			String block = buildFortuneBlock(fortune);
			if (chunk.length() > 0 && chunk.length() + block.length() + 4 > EMBED_DESCRIPTION_LIMIT) {
				chunks.add(chunk.toString());
				chunk = new StringBuilder();
			}
			if (chunk.length() > 0) {
				chunk.append("\n\n---\n\n");
			}
			chunk.append(truncate(block, EMBED_DESCRIPTION_LIMIT));
		}

		if (chunk.length() > 0) {
			chunks.add(chunk.toString());
		}
		return chunks;
	}

	private String buildFortuneBlock(Fortune fortune) {
		return """
			**%s**
			%s
			%s
			%s
			""".formatted(
			fortune.getTargetDate(),
			truncate(fortune.getSubtitle(), 400),
			truncate(fortune.getContent(), 1800),
			truncate(fortune.getItem(), 400)
		).strip();
	}

	private String buildChunkTitle(
		String title,
		int chunkIndex,
		int chunkCount
	) {
		if (chunkCount <= 1) {
			return title;
		}
		return title + " (" + (chunkIndex + 1) + "/" + chunkCount + ")";
	}

	private void post(Map<String, Object> payload) {
		ResponseEntity<String> response = webClient.post()
			.uri(webhookUrlWithWait())
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(payload)
			.retrieve()
			.toEntity(String.class)
			.block();

		logDelivery(response);
	}

	private void assertWebhookConfigured() {
		if (!StringUtils.hasText(webhookUrl) || "default-value".equals(webhookUrl)) {
			throw new IllegalStateException("discord.fortune.webhook.url 설정이 필요합니다.");
		}
	}

	private String webhookUrlWithWait() {
		return UriComponentsBuilder.fromUriString(webhookUrl)
			.replaceQueryParam("wait", "true")
			.build(true)
			.toUriString();
	}

	private void logDelivery(ResponseEntity<String> response) {
		if (response == null) {
			log.warn("Discord webhook response is empty.");
			return;
		}

		String messageId = readJsonText(response.getBody(), "id");
		String channelId = readJsonText(response.getBody(), "channel_id");
		if (StringUtils.hasText(messageId) && StringUtils.hasText(channelId)) {
			log.info(
				"Discord webhook message delivered. status={}, channelId={}, messageId={}",
				response.getStatusCode().value(),
				channelId,
				messageId
			);
			return;
		}

		log.info("Discord webhook request completed. status={}", response.getStatusCode().value());
	}

	private String readJsonText(
		String body,
		String fieldName
	) {
		if (!StringUtils.hasText(body)) {
			return null;
		}
		try {
			JsonNode value = objectMapper.readTree(body).get(fieldName);
			return value == null ? null : value.asText();
		} catch (Exception e) {
			return null;
		}
	}

	private String truncate(
		String value,
		int maxLength
	) {
		if (!StringUtils.hasText(value) || value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength - 3) + "...";
	}

}
