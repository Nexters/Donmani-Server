package donmani.donmani_server.fcm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import donmani.donmani_server.fcm.dto.FortuneAutomationResponse;
import donmani.donmani_server.fcm.entity.Fortune;
import donmani.donmani_server.fcm.entity.FortuneGenerationJob;
import donmani.donmani_server.fcm.entity.FortuneGenerationJobStatus;
import donmani.donmani_server.fcm.entity.FortuneGenerationTriggerType;
import donmani.donmani_server.fcm.entity.FortuneProvider;
import donmani.donmani_server.fcm.repository.FortuneGenerationJobRepository;
import donmani.donmani_server.fcm.repository.FortuneRepository;
import donmani.donmani_server.webhook.service.FortuneReviewWebhookService;

@ExtendWith(MockitoExtension.class)
class FortuneAutomationServiceTest {

	@Mock
	private FortuneRepository fortuneRepository;

	@Mock
	private FortuneGenerationJobRepository fortuneGenerationJobRepository;

	@Mock
	private FortuneProviderRouter fortuneProviderRouter;

	@Mock
	private FortuneProviderResolver fortuneProviderResolver;

	@Mock
	private FortuneTextGenerator gptTextGenerator;

	@Mock
	private FortuneTextGenerator geminiTextGenerator;

	@Mock
	private FortuneImageGenerator gptImageGenerator;

	@Mock
	private FortuneImageGenerator geminiImageGenerator;

	@Mock
	private FortuneImageStorageService fortuneImageStorageService;

	@Mock
	private FortuneReviewWebhookService fortuneReviewWebhookService;

	private FortuneAutomationService fortuneAutomationService;

	@BeforeEach
	void setUp() {
		fortuneAutomationService = new FortuneAutomationService(
			fortuneRepository,
			fortuneGenerationJobRepository,
			fortuneProviderRouter,
			fortuneProviderResolver,
			fortuneImageStorageService,
			fortuneReviewWebhookService
		);
		lenient().when(fortuneProviderResolver.resolve(any(FortuneProvider.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void generateFortunesWithGptUsesGptTextGeneratorAndStoresProviders() {
		LocalDate firstDate = LocalDate.of(2026, 7, 1);
		LocalDate secondDate = LocalDate.of(2026, 7, 2);

		when(fortuneProviderResolver.resolve("gpt")).thenReturn(FortuneProvider.GPT);
		when(fortuneProviderRouter.textGenerator(FortuneProvider.GPT)).thenReturn(gptTextGenerator);
		when(fortuneGenerationJobRepository.findByTargetMonth(LocalDate.of(2026, 7, 1)))
			.thenReturn(Optional.empty());
		when(fortuneGenerationJobRepository.save(any(FortuneGenerationJob.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));
		when(fortuneRepository.save(any(Fortune.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));
		when(gptTextGenerator.generateMonthlyFortunes(YearMonth.of(2026, 7)))
			.thenReturn(List.of(
				new GeneratedFortunePayload(firstDate, "title-1", "subtitle-1", "content-1", "item-1"),
				new GeneratedFortunePayload(secondDate, "title-2", "subtitle-2", "content-2", "item-2")
			));
		when(fortuneRepository.findByTargetDate(firstDate)).thenReturn(Optional.empty());
		when(fortuneRepository.findByTargetDate(secondDate)).thenReturn(Optional.empty());

		FortuneAutomationResponse response = fortuneAutomationService.generateFortunes("2026-07", false, "gpt");

		assertThat(response.isExecuted()).isTrue();
		assertThat(response.getStatus()).isEqualTo(FortuneGenerationJobStatus.WAITING_FOR_APPROVAL);
		assertThat(response.getApprovalRequestedAt()).isNull();
		assertThat(response.getWebhookSentAt()).isNull();

		ArgumentCaptor<Fortune> savedFortunes = ArgumentCaptor.forClass(Fortune.class);
		verify(gptTextGenerator).generateMonthlyFortunes(YearMonth.of(2026, 7));
		verify(geminiTextGenerator, never()).generateMonthlyFortunes(any());
		verify(fortuneReviewWebhookService, never()).sendMonthlyFortuneReview(any(), any());
		verify(fortuneRepository, times(2)).save(savedFortunes.capture());
		assertThat(savedFortunes.getAllValues())
			.extracting(Fortune::getTextProvider, Fortune::getImageProvider)
			.containsExactly(
				org.assertj.core.groups.Tuple.tuple(FortuneProvider.GPT, FortuneProvider.GPT),
				org.assertj.core.groups.Tuple.tuple(FortuneProvider.GPT, FortuneProvider.GPT)
			);
	}

	@Test
	void generateFortunesWithGeminiUsesGeminiTextGenerator() {
		LocalDate targetDate = LocalDate.of(2026, 8, 1);

		when(fortuneProviderResolver.resolve("gemini")).thenReturn(FortuneProvider.GEMINI);
		when(fortuneProviderRouter.textGenerator(FortuneProvider.GEMINI)).thenReturn(geminiTextGenerator);
		when(fortuneGenerationJobRepository.findByTargetMonth(LocalDate.of(2026, 8, 1)))
			.thenReturn(Optional.empty());
		when(fortuneGenerationJobRepository.save(any(FortuneGenerationJob.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));
		when(fortuneRepository.save(any(Fortune.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));
		when(geminiTextGenerator.generateMonthlyFortunes(YearMonth.of(2026, 8)))
			.thenReturn(List.of(
				new GeneratedFortunePayload(targetDate, "title-1", "subtitle-1", "content-1", "item-1")
			));
		when(fortuneRepository.findByTargetDate(targetDate)).thenReturn(Optional.empty());

		FortuneAutomationResponse response = fortuneAutomationService.generateFortunes("2026-08", false, "gemini");

		assertThat(response.isExecuted()).isTrue();
		assertThat(response.getStatus()).isEqualTo(FortuneGenerationJobStatus.WAITING_FOR_APPROVAL);

		ArgumentCaptor<Fortune> savedFortune = ArgumentCaptor.forClass(Fortune.class);
		verify(geminiTextGenerator).generateMonthlyFortunes(YearMonth.of(2026, 8));
		verify(gptTextGenerator, never()).generateMonthlyFortunes(any());
		verify(fortuneRepository).save(savedFortune.capture());
		assertThat(savedFortune.getValue().getTextProvider()).isEqualTo(FortuneProvider.GEMINI);
		assertThat(savedFortune.getValue().getImageProvider()).isEqualTo(FortuneProvider.GEMINI);
	}

	@Test
	void generateFortunesReturnsExistingPreparedJobWhenForceIsFalse() {
		FortuneGenerationJob existingJob = FortuneGenerationJob.start(
			LocalDate.of(2026, 7, 1),
			FortuneGenerationTriggerType.SCHEDULER
		);
		existingJob.markWaitingForApproval(
			31,
			LocalDateTime.of(2026, 6, 21, 9, 0),
			LocalDateTime.of(2026, 6, 21, 9, 1)
		);

		when(fortuneProviderResolver.resolve("gpt")).thenReturn(FortuneProvider.GPT);
		when(fortuneGenerationJobRepository.findByTargetMonth(LocalDate.of(2026, 7, 1)))
			.thenReturn(Optional.of(existingJob));

		FortuneAutomationResponse response = fortuneAutomationService.generateFortunes("2026-07", false, "gpt");

		assertThat(response.isExecuted()).isFalse();
		assertThat(response.getStatus()).isEqualTo(FortuneGenerationJobStatus.WAITING_FOR_APPROVAL);
		verify(gptTextGenerator, never()).generateMonthlyFortunes(any());
		verify(fortuneReviewWebhookService, never()).sendMonthlyFortuneReview(any(), any());
	}

	@Test
	void runForNextMonthUsesDefaultProvider() {
		YearMonth nextMonth = YearMonth.now(ZoneId.of("Asia/Seoul")).plusMonths(1);
		LocalDate targetDate = nextMonth.atDay(1);

		when(fortuneProviderResolver.getDefaultProvider()).thenReturn(FortuneProvider.GPT);
		when(fortuneProviderRouter.textGenerator(FortuneProvider.GPT)).thenReturn(gptTextGenerator);
		when(fortuneGenerationJobRepository.findByTargetMonth(nextMonth.atDay(1)))
			.thenReturn(Optional.empty());
		when(fortuneGenerationJobRepository.save(any(FortuneGenerationJob.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));
		when(fortuneRepository.save(any(Fortune.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));
		when(gptTextGenerator.generateMonthlyFortunes(nextMonth))
			.thenReturn(List.of(new GeneratedFortunePayload(targetDate, "title", "subtitle", "content", "item")));
		when(fortuneRepository.findByTargetDate(targetDate)).thenReturn(Optional.empty());

		FortuneAutomationResponse response = fortuneAutomationService.runForNextMonth(false, FortuneGenerationTriggerType.SCHEDULER);

		assertThat(response.isExecuted()).isTrue();
		verify(gptTextGenerator).generateMonthlyFortunes(nextMonth);
		verify(geminiTextGenerator, never()).generateMonthlyFortunes(any());
	}

	@Test
	void approveMovesWaitingJobToApproved() {
		FortuneGenerationJob existingJob = FortuneGenerationJob.start(
			LocalDate.of(2026, 7, 1),
			FortuneGenerationTriggerType.SCHEDULER
		);
		existingJob.markWaitingForApproval(
			31,
			LocalDateTime.of(2026, 6, 21, 9, 0),
			LocalDateTime.of(2026, 6, 21, 9, 1)
		);

		when(fortuneGenerationJobRepository.findByTargetMonth(LocalDate.of(2026, 7, 1)))
			.thenReturn(Optional.of(existingJob));
		when(fortuneGenerationJobRepository.save(any(FortuneGenerationJob.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		FortuneAutomationResponse response = fortuneAutomationService.approve("2026-07");

		assertThat(response.isExecuted()).isTrue();
		assertThat(response.getStatus()).isEqualTo(FortuneGenerationJobStatus.APPROVED);
		assertThat(response.getApprovedAt()).isNotNull();
	}

	@Test
	void runApprovedImagesForNextMonthUsesSavedImageProvider() {
		YearMonth nextMonth = YearMonth.now(ZoneId.of("Asia/Seoul")).plusMonths(1);
		LocalDate targetDate = nextMonth.atDay(1);

		FortuneGenerationJob existingJob = approvedJob(nextMonth, 1);
		Fortune fortune = Fortune.builder()
			.targetDate(targetDate)
			.title("title-1")
			.subtitle("subtitle-1")
			.content("content-1")
			.item("item-1")
			.build();
		fortune.updateText("title-1", "subtitle-1", "content-1", "item-1", FortuneProvider.GPT, FortuneProvider.GEMINI);

		when(fortuneGenerationJobRepository.findByTargetMonth(nextMonth.atDay(1)))
			.thenReturn(Optional.of(existingJob));
		when(fortuneGenerationJobRepository.save(any(FortuneGenerationJob.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));
		when(fortuneRepository.findAllByTargetDateBetweenOrderByTargetDateAsc(nextMonth.atDay(1), nextMonth.atEndOfMonth()))
			.thenReturn(List.of(fortune));
		when(fortuneProviderRouter.imageGenerator(FortuneProvider.GEMINI)).thenReturn(geminiImageGenerator);
		when(geminiImageGenerator.generateImage(fortune))
			.thenReturn(new GeneratedImagePayload(new byte[] {1, 2, 3}, "image/png", "prompt-1"));
		when(fortuneImageStorageService.upload(any(), eq(targetDate), any(), eq("image/png")))
			.thenReturn(new FortuneImageStorageService.StoredImage("fortune/%s/%s.png".formatted(nextMonth, targetDate), "https://example.com/1.png"));
		when(fortuneRepository.save(any(Fortune.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		FortuneAutomationResponse response = fortuneAutomationService.runApprovedImagesForNextMonth(FortuneGenerationTriggerType.SCHEDULER);

		assertThat(response.isExecuted()).isTrue();
		assertThat(response.getStatus()).isEqualTo(FortuneGenerationJobStatus.COMPLETED);
		verify(geminiImageGenerator).generateImage(fortune);
		verify(gptImageGenerator, never()).generateImage(any());
		verify(fortuneReviewWebhookService).sendMonthlyFortuneImages(any());
		assertThat(fortune.getImageProvider()).isEqualTo(FortuneProvider.GEMINI);
		assertThat(fortune.getImageUrl()).isEqualTo("https://example.com/1.png");
	}

	@Test
	void runApprovedImagesForNextMonthRetriesFailedApprovedJobWithSameProvider() {
		YearMonth nextMonth = YearMonth.now(ZoneId.of("Asia/Seoul")).plusMonths(1);
		LocalDate targetDate = nextMonth.atDay(1);

		FortuneGenerationJob failedApprovedJob = approvedJob(nextMonth, 1);
		failedApprovedJob.markFailed("temporary error");

		Fortune fortune = Fortune.builder()
			.targetDate(targetDate)
			.title("title-1")
			.subtitle("subtitle-1")
			.content("content-1")
			.item("item-1")
			.build();
		fortune.updateText("title-1", "subtitle-1", "content-1", "item-1", FortuneProvider.GPT, FortuneProvider.GPT);

		when(fortuneGenerationJobRepository.findByTargetMonth(nextMonth.atDay(1)))
			.thenReturn(Optional.of(failedApprovedJob));
		when(fortuneGenerationJobRepository.findFirstByApprovedAtIsNotNullAndStatusInOrderByTargetMonthAsc(any()))
			.thenReturn(Optional.of(failedApprovedJob));
		when(fortuneGenerationJobRepository.save(any(FortuneGenerationJob.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));
		when(fortuneRepository.findAllByTargetDateBetweenOrderByTargetDateAsc(nextMonth.atDay(1), nextMonth.atEndOfMonth()))
			.thenReturn(List.of(fortune));
		when(fortuneProviderRouter.imageGenerator(FortuneProvider.GPT)).thenReturn(gptImageGenerator);
		when(gptImageGenerator.generateImage(fortune))
			.thenReturn(new GeneratedImagePayload(new byte[] {1, 2, 3}, "image/png", "prompt-1"));
		when(fortuneImageStorageService.upload(any(), eq(targetDate), any(), eq("image/png")))
			.thenReturn(new FortuneImageStorageService.StoredImage("fortune/%s/%s.png".formatted(nextMonth, targetDate), "https://example.com/1.png"));
		when(fortuneRepository.save(any(Fortune.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		FortuneAutomationResponse response = fortuneAutomationService.runApprovedImagesForNextMonth(FortuneGenerationTriggerType.SCHEDULER);

		assertThat(response.isExecuted()).isTrue();
		assertThat(response.getStatus()).isEqualTo(FortuneGenerationJobStatus.COMPLETED);
		verify(gptImageGenerator).generateImage(fortune);
		assertThat(fortune.getImageProvider()).isEqualTo(FortuneProvider.GPT);
	}

	@Test
	void runApprovedImagesForNextMonthSkipsAlreadyUploadedImages() {
		YearMonth nextMonth = YearMonth.now(ZoneId.of("Asia/Seoul")).plusMonths(1);
		LocalDate uploadedDate = nextMonth.atDay(1);
		LocalDate missingDate = nextMonth.atDay(2);

		FortuneGenerationJob existingJob = approvedJob(nextMonth, 2);
		Fortune uploadedFortune = Fortune.builder()
			.targetDate(uploadedDate)
			.title("title-1")
			.subtitle("subtitle-1")
			.content("content-1")
			.item("item-1")
			.build();
		uploadedFortune.updateText("title-1", "subtitle-1", "content-1", "item-1", FortuneProvider.GPT, FortuneProvider.GPT);
		uploadedFortune.updateImage("fortune/%s/%s.png".formatted(nextMonth, uploadedDate), "https://example.com/1.png", "prompt-1", FortuneProvider.GPT);

		Fortune missingFortune = Fortune.builder()
			.targetDate(missingDate)
			.title("title-2")
			.subtitle("subtitle-2")
			.content("content-2")
			.item("item-2")
			.build();
		missingFortune.updateText("title-2", "subtitle-2", "content-2", "item-2", FortuneProvider.GPT, FortuneProvider.GPT);

		when(fortuneGenerationJobRepository.findByTargetMonth(nextMonth.atDay(1)))
			.thenReturn(Optional.of(existingJob));
		when(fortuneGenerationJobRepository.save(any(FortuneGenerationJob.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));
		when(fortuneRepository.findAllByTargetDateBetweenOrderByTargetDateAsc(nextMonth.atDay(1), nextMonth.atEndOfMonth()))
			.thenReturn(List.of(uploadedFortune, missingFortune));
		when(fortuneProviderRouter.imageGenerator(FortuneProvider.GPT)).thenReturn(gptImageGenerator);
		when(gptImageGenerator.generateImage(missingFortune))
			.thenReturn(new GeneratedImagePayload(new byte[] {1, 2, 3}, "image/png", "prompt-2"));
		when(fortuneImageStorageService.upload(any(), eq(missingDate), any(), eq("image/png")))
			.thenReturn(new FortuneImageStorageService.StoredImage("fortune/%s/%s.png".formatted(nextMonth, missingDate), "https://example.com/2.png"));
		when(fortuneRepository.save(any(Fortune.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		FortuneAutomationResponse response = fortuneAutomationService.runApprovedImagesForNextMonth(FortuneGenerationTriggerType.SCHEDULER);

		assertThat(response.isExecuted()).isTrue();
		assertThat(response.getStatus()).isEqualTo(FortuneGenerationJobStatus.COMPLETED);
		verify(gptImageGenerator, never()).generateImage(uploadedFortune);
		verify(gptImageGenerator).generateImage(missingFortune);
		verify(fortuneImageStorageService, never()).upload(any(), eq(uploadedDate), any(), any());
		verify(fortuneReviewWebhookService).sendMonthlyFortuneImages(eq(List.of(uploadedFortune, missingFortune)));
	}

	@Test
	void generateImagesForTargetDateDoesNotSendWebhook() {
		YearMonth targetMonth = YearMonth.of(2026, 7);
		LocalDate targetDate = LocalDate.of(2026, 7, 15);
		FortuneGenerationJob existingJob = approvedJob(targetMonth, 31);
		Fortune fortune = Fortune.builder()
			.targetDate(targetDate)
			.title("title-15")
			.subtitle("subtitle-15")
			.content("content-15")
			.item("item-15")
			.build();
		fortune.updateText("title-15", "subtitle-15", "content-15", "item-15", FortuneProvider.GPT, FortuneProvider.GPT);

		when(fortuneRepository.findByTargetDate(targetDate)).thenReturn(Optional.of(fortune));
		when(fortuneProviderResolver.resolve("gemini")).thenReturn(FortuneProvider.GEMINI);
		when(fortuneProviderRouter.imageGenerator(FortuneProvider.GEMINI)).thenReturn(geminiImageGenerator);
		when(geminiImageGenerator.generateImage(fortune))
			.thenReturn(new GeneratedImagePayload(new byte[] {1, 2, 3}, "image/png", "prompt-15"));
		when(fortuneImageStorageService.upload(eq(targetMonth), eq(targetDate), any(), eq("image/png")))
			.thenReturn(new FortuneImageStorageService.StoredImage("fortune/2026-07/2026-07-15.png", "https://example.com/15.png"));
		when(fortuneRepository.save(any(Fortune.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));
		when(fortuneGenerationJobRepository.findByTargetMonth(targetMonth.atDay(1)))
			.thenReturn(Optional.of(existingJob));

		FortuneAutomationResponse response = fortuneAutomationService.generateImages(null, "2026-07-15", "gemini");

		assertThat(response.isExecuted()).isTrue();
		assertThat(response.getStatus()).isEqualTo(FortuneGenerationJobStatus.APPROVED);
		assertThat(response.getTargetDate()).isEqualTo(targetDate);
		assertThat(response.getImageUrl()).isEqualTo("https://example.com/15.png");
		verify(geminiImageGenerator).generateImage(fortune);
		verify(fortuneImageStorageService).upload(eq(targetMonth), eq(targetDate), any(), eq("image/png"));
		verify(fortuneReviewWebhookService, never()).sendMonthlyFortuneImages(any());
	}

	@Test
	void generateImagesForTargetDateDoesNotGenerateWhenJobIsWaitingForApproval() {
		YearMonth targetMonth = YearMonth.of(2026, 7);
		LocalDate targetDate = LocalDate.of(2026, 7, 15);
		FortuneGenerationJob waitingJob = FortuneGenerationJob.start(
			targetMonth.atDay(1),
			FortuneGenerationTriggerType.MANUAL
		);
		waitingJob.markWaitingForApproval(
			31,
			LocalDateTime.of(2026, 6, 21, 9, 0),
			LocalDateTime.of(2026, 6, 21, 9, 1)
		);

		when(fortuneGenerationJobRepository.findByTargetMonth(targetMonth.atDay(1)))
			.thenReturn(Optional.of(waitingJob));

		FortuneAutomationResponse response = fortuneAutomationService.generateImages(null, "2026-07-15", "gemini");

		assertThat(response.isExecuted()).isFalse();
		assertThat(response.getStatus()).isEqualTo(FortuneGenerationJobStatus.WAITING_FOR_APPROVAL);
		assertThat(response.getTargetDate()).isEqualTo(targetDate);
		assertThat(response.getImageUrl()).isNull();
		verify(fortuneRepository, never()).findByTargetDate(any());
		verify(geminiImageGenerator, never()).generateImage(any());
		verify(fortuneImageStorageService, never()).upload(any(), any(), any(), any());
	}

	@Test
	void generateImagesForTargetDateDoesNotGenerateWhenJobIsMissing() {
		YearMonth targetMonth = YearMonth.of(2026, 7);
		LocalDate targetDate = LocalDate.of(2026, 7, 15);

		when(fortuneGenerationJobRepository.findByTargetMonth(targetMonth.atDay(1)))
			.thenReturn(Optional.empty());

		FortuneAutomationResponse response = fortuneAutomationService.generateImages(null, "2026-07-15", "gemini");

		assertThat(response.isExecuted()).isFalse();
		assertThat(response.getTargetMonth()).isEqualTo("2026-07");
		assertThat(response.getTargetDate()).isEqualTo(targetDate);
		assertThat(response.getStatus()).isNull();
		assertThat(response.getImageUrl()).isNull();
		verify(fortuneRepository, never()).findByTargetDate(any());
		verify(geminiImageGenerator, never()).generateImage(any());
		verify(fortuneImageStorageService, never()).upload(any(), any(), any(), any());
	}

	@Test
	void generateImagesForMonthDoesNotSendWebhook() {
		YearMonth targetMonth = YearMonth.of(2026, 7);
		LocalDate targetDate = LocalDate.of(2026, 7, 1);

		FortuneGenerationJob existingJob = approvedJob(targetMonth, 1);
		Fortune fortune = Fortune.builder()
			.targetDate(targetDate)
			.title("title-1")
			.subtitle("subtitle-1")
			.content("content-1")
			.item("item-1")
			.build();
		fortune.updateText("title-1", "subtitle-1", "content-1", "item-1", FortuneProvider.GPT, FortuneProvider.GPT);

		when(fortuneGenerationJobRepository.findByTargetMonth(targetMonth.atDay(1)))
			.thenReturn(Optional.of(existingJob));
		when(fortuneGenerationJobRepository.save(any(FortuneGenerationJob.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));
		when(fortuneRepository.findAllByTargetDateBetweenOrderByTargetDateAsc(targetMonth.atDay(1), targetMonth.atEndOfMonth()))
			.thenReturn(List.of(fortune));
		when(fortuneProviderRouter.imageGenerator(FortuneProvider.GPT)).thenReturn(gptImageGenerator);
		when(gptImageGenerator.generateImage(fortune))
			.thenReturn(new GeneratedImagePayload(new byte[] {1, 2, 3}, "image/png", "prompt-1"));
		when(fortuneImageStorageService.upload(eq(targetMonth), eq(targetDate), any(), eq("image/png")))
			.thenReturn(new FortuneImageStorageService.StoredImage("fortune/2026-07/2026-07-01.png", "https://example.com/1.png"));
		when(fortuneRepository.save(any(Fortune.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		FortuneAutomationResponse response = fortuneAutomationService.generateImages("2026-07", null, null);

		assertThat(response.isExecuted()).isTrue();
		assertThat(response.getStatus()).isEqualTo(FortuneGenerationJobStatus.GENERATING_IMAGES);
		assertThat(fortune.getImageUrl()).isEqualTo("https://example.com/1.png");
		verify(gptImageGenerator).generateImage(fortune);
		verify(fortuneReviewWebhookService, never()).sendMonthlyFortuneImages(any());
	}

	@Test
	void sendFortuneWithImageWebhookUsesSavedMonthlyFortunesAndMarksReviewWebhookSent() {
		YearMonth targetMonth = YearMonth.of(2026, 8);
		FortuneGenerationJob existingJob = FortuneGenerationJob.start(
			targetMonth.atDay(1),
			FortuneGenerationTriggerType.MANUAL
		);
		existingJob.markWaitingForApproval(
			2,
			LocalDateTime.of(2026, 7, 21, 9, 0),
			LocalDateTime.of(2026, 7, 21, 9, 1)
		);

		Fortune firstFortune = Fortune.builder()
			.targetDate(LocalDate.of(2026, 8, 1))
			.title("title-1")
			.subtitle("subtitle-1")
			.content("content-1")
			.item("item-1")
			.build();
		firstFortune.updateImage("key-1", "https://example.com/1.png", "prompt-1", FortuneProvider.GPT);
		Fortune secondFortune = Fortune.builder()
			.targetDate(LocalDate.of(2026, 8, 2))
			.title("title-2")
			.subtitle("subtitle-2")
			.content("content-2")
			.item("item-2")
			.build();
		List<Fortune> savedFortunes = List.of(firstFortune, secondFortune);

		when(fortuneGenerationJobRepository.findByTargetMonth(targetMonth.atDay(1)))
			.thenReturn(Optional.of(existingJob));
		when(fortuneRepository.findAllByTargetDateBetweenOrderByTargetDateAsc(targetMonth.atDay(1), targetMonth.atEndOfMonth()))
			.thenReturn(savedFortunes);
		when(fortuneGenerationJobRepository.save(any(FortuneGenerationJob.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		FortuneAutomationResponse response = fortuneAutomationService.sendFortuneWithImageWebhook("2026-08");

		assertThat(response.isExecuted()).isTrue();
		assertThat(response.getStatus()).isEqualTo(FortuneGenerationJobStatus.WAITING_FOR_APPROVAL);
		assertThat(response.getWebhookSentAt()).isNotNull();
		assertThat(response.getApprovalRequestedAt()).isNotNull();
		verify(fortuneReviewWebhookService).sendMonthlyFortuneReviewWithImages(eq(savedFortunes));
		verify(gptImageGenerator, never()).generateImage(any());
		verify(geminiImageGenerator, never()).generateImage(any());
		verify(fortuneImageStorageService, never()).upload(any(), any(), any(), any());
	}

	@Test
	void sendFortuneWithImageWebhookForApprovedJobKeepsStatusAndMarksWebhookSent() {
		YearMonth targetMonth = YearMonth.of(2026, 8);
		FortuneGenerationJob existingJob = approvedJob(targetMonth, 1);
		Fortune fortune = Fortune.builder()
			.targetDate(LocalDate.of(2026, 8, 1))
			.title("title-1")
			.subtitle("subtitle-1")
			.content("content-1")
			.item("item-1")
			.build();

		when(fortuneGenerationJobRepository.findByTargetMonth(targetMonth.atDay(1)))
			.thenReturn(Optional.of(existingJob));
		when(fortuneRepository.findAllByTargetDateBetweenOrderByTargetDateAsc(targetMonth.atDay(1), targetMonth.atEndOfMonth()))
			.thenReturn(List.of(fortune));
		when(fortuneGenerationJobRepository.save(any(FortuneGenerationJob.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		FortuneAutomationResponse response = fortuneAutomationService.sendFortuneWithImageWebhook("2026-08");

		assertThat(response.isExecuted()).isTrue();
		assertThat(response.getStatus()).isEqualTo(FortuneGenerationJobStatus.APPROVED);
		assertThat(response.getWebhookSentAt()).isNotNull();
		verify(fortuneReviewWebhookService).sendMonthlyFortuneReviewWithImages(eq(List.of(fortune)));
	}

	@Test
	void sendFortuneWithImageWebhookThrowsWhenMonthlyFortunesAreMissing() {
		YearMonth targetMonth = YearMonth.of(2026, 8);
		FortuneGenerationJob existingJob = approvedJob(targetMonth, 0);

		when(fortuneGenerationJobRepository.findByTargetMonth(targetMonth.atDay(1)))
			.thenReturn(Optional.of(existingJob));
		when(fortuneRepository.findAllByTargetDateBetweenOrderByTargetDateAsc(targetMonth.atDay(1), targetMonth.atEndOfMonth()))
			.thenReturn(List.of());

		assertThatThrownBy(() -> fortuneAutomationService.sendFortuneWithImageWebhook("2026-08"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("해당 월의 운세 데이터가 없습니다.");

		verify(fortuneReviewWebhookService, never()).sendMonthlyFortuneReviewWithImages(any());
		verify(fortuneGenerationJobRepository, never()).save(any());
	}

	private FortuneGenerationJob approvedJob(
		YearMonth targetMonth,
		int fortuneCount
	) {
		FortuneGenerationJob job = FortuneGenerationJob.start(
			targetMonth.atDay(1),
			FortuneGenerationTriggerType.SCHEDULER
		);
		job.markWaitingForApproval(
			fortuneCount,
			LocalDateTime.of(2026, 6, 21, 9, 0),
			LocalDateTime.of(2026, 6, 21, 9, 1)
		);
		job.markApproved(LocalDateTime.of(2026, 6, 22, 10, 0));
		return job;
	}

}
