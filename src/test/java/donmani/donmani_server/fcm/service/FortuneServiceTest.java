package donmani.donmani_server.fcm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import donmani.donmani_server.fcm.dto.FortuneHistoryResponseV1;
import donmani.donmani_server.fcm.entity.Fortune;
import donmani.donmani_server.fcm.entity.FortuneHistory;
import donmani.donmani_server.fcm.entity.ReadSource;
import donmani.donmani_server.fcm.repository.FortuneHistoryRepository;
import donmani.donmani_server.fcm.repository.FortuneRepository;
import donmani.donmani_server.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class FortuneServiceTest {

	@Mock
	private FortuneRepository fortuneRepository;

	@Mock
	private FortuneHistoryRepository fortuneHistoryRepository;

	@Mock
	private FCMService fcmService;

	@Mock
	private UserService userService;

	@Test
	void getFortuneHistoriesReturnsReadFortunesInRepositoryOrder() {
		FortuneService fortuneService = new FortuneService(
			fortuneRepository,
			fortuneHistoryRepository,
			fcmService,
			userService
		);
		LocalDate startDate = LocalDate.of(2026, 7, 1);
		LocalDate endDate = LocalDate.of(2026, 7, 31);
		Fortune firstFortune = fortune(LocalDate.of(2026, 7, 1), "image-1", "subtitle-1", "content-1", "item-1");
		Fortune secondFortune = fortune(LocalDate.of(2026, 7, 3), "image-3", "subtitle-3", "content-3", "item-3");
		FortuneHistory firstHistory = readHistory(firstFortune);
		FortuneHistory secondHistory = readHistory(secondFortune);

		when(fortuneHistoryRepository.findReadFortunesByTargetDateBetween("user-1234", startDate, endDate))
			.thenReturn(List.of(firstHistory, secondHistory));

		List<FortuneHistoryResponseV1> response = fortuneService.getFortuneHistories(
			"user-1234",
			startDate,
			endDate
		);

		assertThat(response).hasSize(2);
		assertThat(response)
			.extracting(
				FortuneHistoryResponseV1::getTargetDate,
				FortuneHistoryResponseV1::getImageUrl,
				FortuneHistoryResponseV1::getSubtitle,
				FortuneHistoryResponseV1::getContent,
				FortuneHistoryResponseV1::getItem
			)
			.containsExactly(
				org.assertj.core.groups.Tuple.tuple(LocalDate.of(2026, 7, 1), "image-1", "subtitle-1", "content-1", "item-1"),
				org.assertj.core.groups.Tuple.tuple(LocalDate.of(2026, 7, 3), "image-3", "subtitle-3", "content-3", "item-3")
			);
	}

	private Fortune fortune(
		LocalDate targetDate,
		String imageUrl,
		String subtitle,
		String content,
		String item
	) {
		Fortune fortune = Fortune.builder()
			.targetDate(targetDate)
			.title("title")
			.subtitle(subtitle)
			.content(content)
			.item(item)
			.build();
		fortune.updateImage(null, imageUrl, null, null);
		return fortune;
	}

	private FortuneHistory readHistory(Fortune fortune) {
		FortuneHistory fortuneHistory = FortuneHistory.builder()
			.fortune(fortune)
			.user(null)
			.build();
		fortuneHistory.markAsRead(ReadSource.NOTIFICATION, LocalDateTime.of(2026, 7, 1, 9, 0));
		return fortuneHistory;
	}
}
