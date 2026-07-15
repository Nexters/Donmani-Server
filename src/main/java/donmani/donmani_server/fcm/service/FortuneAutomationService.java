package donmani.donmani_server.fcm.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import donmani.donmani_server.fcm.dto.FortuneAutomationResponse;
import donmani.donmani_server.fcm.entity.Fortune;
import donmani.donmani_server.fcm.entity.FortuneGenerationJob;
import donmani.donmani_server.fcm.entity.FortuneGenerationJobStatus;
import donmani.donmani_server.fcm.entity.FortuneGenerationTriggerType;
import donmani.donmani_server.fcm.entity.FortuneProvider;
import donmani.donmani_server.fcm.repository.FortuneGenerationJobRepository;
import donmani.donmani_server.fcm.repository.FortuneRepository;
import donmani.donmani_server.webhook.service.FortuneReviewWebhookService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FortuneAutomationService {
	/*
	 * 월간 운세 자동화 흐름:
	 * 1. generateFortunes: API 파라미터를 해석하고 지정 월의 운세 텍스트 생성을 시작한다.
	 * 2. sendFortuneReviewWebhook: 저장된 운세 텍스트를 Discord 검수용 웹훅으로 보낸다.
	 * 3. approve: 검수 완료 후 해당 월 작업을 APPROVED 상태로 바꾼다.
	 * 4. generateImages/runApprovedImagesForNextMonth: 승인된 월의 운세 이미지를 생성하고 스토리지에 업로드한다.
	 * 5. sendImageWebhook/sendFortuneWithImageWebhook: 이미지 전용 또는 텍스트+이미지 합본 웹훅을 보낸다.
	 * 6. sendImageWebhook: 월 전체 운세에 imageUrl이 있어야 작업을 COMPLETED로 마감한다.
	 */

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final DateTimeFormatter TARGET_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

	private final FortuneRepository fortuneRepository;
	private final FortuneGenerationJobRepository fortuneGenerationJobRepository;
	private final FortuneProviderRouter fortuneProviderRouter;
	private final FortuneProviderResolver fortuneProviderResolver;
	private final FortuneImageStorageService fortuneImageStorageService;
	private final FortuneReviewWebhookService fortuneReviewWebhookService;

	public FortuneAutomationResponse runForNextMonth(
		boolean force,
		FortuneGenerationTriggerType triggerType
	) {
		YearMonth nextMonth = YearMonth.from(LocalDate.now(KST).plusMonths(1));
		return generateFortunesForMonth(nextMonth, force, triggerType, fortuneProviderResolver.getDefaultProvider());
	}

	// 수동 API 요청값을 내부 타입으로 변환하고, 실제 월간 운세 생성 로직으로 넘긴다.
	public FortuneAutomationResponse generateFortunes(
		String targetMonth,
		boolean force,
		String provider
	) {
		FortuneProvider resolvedProvider = fortuneProviderResolver.resolve(provider);
		if (!StringUtils.hasText(targetMonth)) {
			return generateFortunesForMonth(
				YearMonth.from(LocalDate.now(KST).plusMonths(1)),
				force,
				FortuneGenerationTriggerType.MANUAL,
				resolvedProvider
			);
		}
		return generateFortunesForMonth(parseTargetMonth(targetMonth), force, FortuneGenerationTriggerType.MANUAL, resolvedProvider);
	}

	// 검수자가 확인한 월간 운세를 승인 상태로 전환한다. 승인된 월만 이미지 생성 대상이 된다.
	public FortuneAutomationResponse approve(String targetMonth) {
		YearMonth yearMonth = parseTargetMonth(targetMonth);
		FortuneGenerationJob job = getJob(yearMonth);

		if (job.getStatus() != FortuneGenerationJobStatus.WAITING_FOR_APPROVAL) {
			throw new IllegalStateException("승인 대기 상태의 운세만 승인할 수 있습니다.");
		}

		job.markApproved(LocalDateTime.now(KST));
		fortuneGenerationJobRepository.save(job);
		return FortuneAutomationResponse.from(job, true);
	}

	public FortuneAutomationResponse runApprovedImagesForNextMonth(FortuneGenerationTriggerType triggerType) {
		YearMonth nextMonth = YearMonth.from(LocalDate.now(KST).plusMonths(1));
		FortuneGenerationJob nextMonthJob = fortuneGenerationJobRepository.findByTargetMonth(nextMonth.atDay(1))
			.orElse(null);

		if (nextMonthJob != null && nextMonthJob.getStatus() == FortuneGenerationJobStatus.APPROVED) {
			generateImagesForApprovedMonth(nextMonth, triggerType, false, null);
			return sendImageWebhook(nextMonth);
		}

		FortuneGenerationJob approvedJob = fortuneGenerationJobRepository
			.findFirstByApprovedAtIsNotNullAndStatusInOrderByTargetMonthAsc(
				EnumSet.of(FortuneGenerationJobStatus.APPROVED, FortuneGenerationJobStatus.FAILED)
			)
			.orElse(null);

		if (approvedJob == null) {
			if (nextMonthJob != null) {
				return FortuneAutomationResponse.from(nextMonthJob, false);
			}
			return FortuneAutomationResponse.builder()
				.targetMonth(nextMonth.toString())
				.executed(false)
				.build();
		}

		YearMonth approvedMonth = YearMonth.from(approvedJob.getTargetMonth());
		generateImagesForApprovedMonth(approvedMonth, triggerType, false, null);
		return sendImageWebhook(approvedMonth);
	}

	// 저장된 운세 텍스트만 Discord 검수본으로 보낸다. 생성기나 스토리지는 호출하지 않는다.
	public FortuneAutomationResponse sendFortuneReviewWebhook(String targetMonth) {
		return sendFortuneReviewWebhook(parseTargetMonth(targetMonth));
	}

	// 저장된 운세 텍스트와 imageUrl을 한 embed에 묶어 보낸다. 생성/업로드 없이 DB 값만 사용한다.
	public FortuneAutomationResponse sendFortuneWithImageWebhook(String targetMonth) {
		YearMonth yearMonth = parseTargetMonth(targetMonth);
		FortuneGenerationJob job = getJob(yearMonth);
		List<Fortune> fortunes = getMonthlyFortunes(yearMonth);
		if (fortunes.isEmpty()) {
			throw new IllegalArgumentException("해당 월의 운세 데이터가 없습니다.");
		}

		LocalDateTime now = LocalDateTime.now(KST);
		// 합본 웹훅은 검수 편의를 위해 DB에 저장된 텍스트와 imageUrl만 전송한다.
		fortuneReviewWebhookService.sendMonthlyFortuneReviewWithImages(fortunes);
		if (isReviewWebhookStatus(job.getStatus())) {
			job.markReviewWebhookSent(fortunes.size(), now);
		} else {
			job.markWebhookSent(now);
		}
		fortuneGenerationJobRepository.save(job);

		return FortuneAutomationResponse.from(job, true);
	}

	private FortuneAutomationResponse sendFortuneReviewWebhook(YearMonth yearMonth) {
		FortuneGenerationJob job = getJob(yearMonth);
		List<Fortune> fortunes = getMonthlyFortunes(yearMonth);
		if (fortunes.isEmpty()) {
			throw new IllegalArgumentException("해당 월의 운세 데이터가 없습니다.");
		}

		LocalDateTime now = LocalDateTime.now(KST);
		fortuneReviewWebhookService.sendMonthlyFortuneReview(yearMonth, fortunes);
		if (isReviewWebhookStatus(job.getStatus())) {
			job.markReviewWebhookSent(fortunes.size(), now);
		} else {
			job.markWebhookSent(now);
		}
		fortuneGenerationJobRepository.save(job);

		return FortuneAutomationResponse.from(job, true);
	}

	public FortuneAutomationResponse getStatus(String targetMonth) {
		YearMonth yearMonth = parseTargetMonth(targetMonth);
		FortuneGenerationJob job = getJob(yearMonth);

		return FortuneAutomationResponse.from(job, false);
	}

	// 승인된 운세의 이미지를 생성해 스토리지에 업로드한다. 웹훅 발송은 별도 API에서 처리한다.
	public FortuneAutomationResponse generateImages(
		String targetMonth,
		String targetDate,
		String provider
	) {
		assertSingleImageTarget(targetMonth, targetDate);

		if (StringUtils.hasText(targetDate)) {
			LocalDate parsedTargetDate = parseTargetDate(targetDate);
			YearMonth yearMonth = YearMonth.from(parsedTargetDate);
			FortuneGenerationJob job = fortuneGenerationJobRepository.findByTargetMonth(yearMonth.atDay(1))
				.orElse(null);
			if (job == null) {
				return FortuneAutomationResponse.builder()
					.targetMonth(yearMonth.toString())
					.targetDate(parsedTargetDate)
					.executed(false)
					.build();
			}
			if (!isImageGenerationRetryable(job, false)) {
				return FortuneAutomationResponse.from(job, false, parsedTargetDate, null);
			}

			FortuneProvider providerOverride = StringUtils.hasText(provider)
				? fortuneProviderResolver.resolve(provider)
				: null;
			Fortune fortune = getDailyFortune(parsedTargetDate);
			FortuneProvider imageProvider = providerOverride == null
				? fortuneProviderResolver.resolve(fortune.getImageProvider())
				: providerOverride;

			generateAndUploadImage(yearMonth, fortune, imageProvider);
			return FortuneAutomationResponse.from(job, true, parsedTargetDate, fortune.getImageUrl());
		}

		FortuneProvider providerOverride = StringUtils.hasText(provider)
			? fortuneProviderResolver.resolve(provider)
			: null;

		return generateImagesForApprovedMonth(
			parseTargetMonth(targetMonth),
			FortuneGenerationTriggerType.MANUAL,
			false,
			providerOverride
		);
	}

	// 수동/스케줄러가 공통으로 사용하는 실제 월간 운세 생성 로직이다.
	// 이 단계에서는 텍스트 생성과 DB 저장만 수행하고, 웹훅과 이미지 생성은 실행하지 않는다.
	private FortuneAutomationResponse generateFortunesForMonth(
		YearMonth targetMonth,
		boolean force,
		FortuneGenerationTriggerType triggerType,
		FortuneProvider provider
	) {
		FortuneGenerationJob existingJob = fortuneGenerationJobRepository.findByTargetMonth(targetMonth.atDay(1))
			.orElse(null);

		if (existingJob != null && isFortuneGenerationInProgress(existingJob.getStatus())) {
			throw new IllegalStateException("해당 월의 운세 생성 작업이 이미 실행 중입니다.");
		}

		if (existingJob != null && isFortuneGenerationAlreadyPrepared(existingJob.getStatus()) && !force) {
			return FortuneAutomationResponse.from(existingJob, false);
		}

		FortuneGenerationJob job = existingJob == null
			? FortuneGenerationJob.start(targetMonth.atDay(1), triggerType)
			: existingJob;

		if (existingJob != null) {
			job.markGeneratingFortunes(triggerType);
		}

		fortuneGenerationJobRepository.save(job);

		try {
			List<GeneratedFortunePayload> generatedFortunes = fortuneProviderRouter
				.textGenerator(provider)
				.generateMonthlyFortunes(targetMonth);
			List<Fortune> fortunes = upsertFortunes(generatedFortunes, provider);
			LocalDateTime now = LocalDateTime.now(KST);
			job.markWaitingForApproval(fortunes.size(), now, null);
			fortuneGenerationJobRepository.save(job);

			return FortuneAutomationResponse.from(job, true);
		} catch (Exception e) {
			job.markFailed(truncateFailureMessage(e.getMessage()));
			fortuneGenerationJobRepository.save(job);
			throw e;
		}
	}

	private FortuneAutomationResponse generateImagesForApprovedMonth(
		YearMonth targetMonth,
		FortuneGenerationTriggerType triggerType,
		boolean force,
		FortuneProvider providerOverride
	) {
		FortuneGenerationJob job = getJob(targetMonth);

		if (job.getStatus() == FortuneGenerationJobStatus.GENERATING_IMAGES) {
			throw new IllegalStateException("해당 월의 이미지 생성 작업이 이미 실행 중입니다.");
		}

		if (job.getStatus() == FortuneGenerationJobStatus.COMPLETED && !force) {
			return FortuneAutomationResponse.from(job, false);
		}

		if (!isImageGenerationRetryable(job, force)) {
			return FortuneAutomationResponse.from(job, false);
		}

		job.markGeneratingImages(triggerType);
		fortuneGenerationJobRepository.save(job);

		try {
			List<Fortune> fortunes = getMonthlyFortunes(targetMonth);
			if (fortunes.isEmpty()) {
				throw new IllegalArgumentException("해당 월의 운세 데이터가 없습니다.");
			}

			generateAndUploadImages(targetMonth, fortunes, providerOverride);
			fortuneGenerationJobRepository.save(job);

			return FortuneAutomationResponse.from(job, true);
		} catch (Exception e) {
			job.markFailed(truncateFailureMessage(e.getMessage()));
			fortuneGenerationJobRepository.save(job);
			throw e;
		}
	}

	private List<Fortune> upsertFortunes(
		List<GeneratedFortunePayload> generatedFortunes,
		FortuneProvider provider
	) {
		return generatedFortunes.stream()
			.map(generatedFortune -> {
				Fortune fortune = fortuneRepository.findByTargetDate(generatedFortune.targetDate())
					.orElseGet(() -> Fortune.builder()
						.targetDate(generatedFortune.targetDate())
						.title(generatedFortune.title())
						.subtitle(generatedFortune.subtitle())
						.content(generatedFortune.content())
						.item(generatedFortune.item())
						.build());

				fortune.updateText(
					generatedFortune.title(),
					generatedFortune.subtitle(),
					generatedFortune.content(),
					generatedFortune.item(),
					provider,
					provider
				);
				// 텍스트가 바뀌면 기존 이미지는 더 이상 같은 운세를 표현하지 않을 수 있다.
				fortune.clearImage();
				return fortuneRepository.save(fortune);
			})
			.toList();
	}

	private void generateAndUploadImages(
		YearMonth targetMonth,
		List<Fortune> fortunes,
		FortuneProvider providerOverride
	) {
		for (Fortune fortune : fortunes) {
			if (StringUtils.hasText(fortune.getImageUrl())) {
				// 이미 업로드된 이미지는 재생성 비용을 들이지 않는다.
				continue;
			}
			if (restoreStoredImageIfExists(targetMonth, fortune)) {
				// DB만 비어 있고 스토리지 파일이 남아 있으면 기존 URL을 복구한다.
				continue;
			}
			FortuneProvider imageProvider = providerOverride == null
				? fortuneProviderResolver.resolve(fortune.getImageProvider())
				: providerOverride;
			generateAndUploadImage(targetMonth, fortune, imageProvider);
		}
	}

	private FortuneAutomationResponse sendImageWebhook(YearMonth yearMonth) {
		FortuneGenerationJob job = getJob(yearMonth);
		List<Fortune> fortunes = getMonthlyFortunes(yearMonth).stream()
			.map(fortune -> restoreStoredImageIfMissing(yearMonth, fortune))
			.toList();
		if (fortunes.isEmpty()) {
			throw new IllegalArgumentException("해당 월의 운세 데이터가 없습니다.");
		}

		List<Fortune> imageFortunes = fortunes.stream()
			.filter(fortune -> StringUtils.hasText(fortune.getImageUrl()))
			.toList();
		if (imageFortunes.isEmpty()) {
			throw new IllegalArgumentException("이미지 생성된 운세 데이터가 없습니다.");
		}

		LocalDateTime now = LocalDateTime.now(KST);
		fortuneReviewWebhookService.sendMonthlyFortuneImages(imageFortunes);
		if (hasGeneratedImages(fortunes)) {
			// 월 전체 이미지가 준비된 경우에만 자동화 작업을 완료 처리한다.
			job.markCompleted(fortunes.size(), now, now);
		} else {
			job.markWebhookSent(now);
		}
		fortuneGenerationJobRepository.save(job);

		return FortuneAutomationResponse.from(job, true);
	}

	private void generateAndUploadImage(
		YearMonth targetMonth,
		Fortune fortune,
		FortuneProvider imageProvider
	) {
		GeneratedImagePayload generatedImage = fortuneProviderRouter.imageGenerator(imageProvider).generateImage(fortune);
		FortuneImageStorageService.StoredImage storedImage = fortuneImageStorageService.upload(
			targetMonth,
			fortune.getTargetDate(),
			generatedImage.bytes(),
			generatedImage.mimeType()
		);
		fortune.updateImage(storedImage.key(), storedImage.url(), generatedImage.prompt(), imageProvider);
		fortuneRepository.save(fortune);
	}

	private Fortune restoreStoredImageIfMissing(
		YearMonth targetMonth,
		Fortune fortune
	) {
		if (!StringUtils.hasText(fortune.getImageUrl())) {
			restoreStoredImageIfExists(targetMonth, fortune);
		}
		return fortune;
	}

	private boolean restoreStoredImageIfExists(
		YearMonth targetMonth,
		Fortune fortune
	) {
		return fortuneImageStorageService.findStoredImage(targetMonth, fortune.getTargetDate())
			.map(storedImage -> {
				fortune.updateImage(
					storedImage.key(),
					storedImage.url(),
					fortune.getImagePrompt(),
					fortune.getImageProvider()
				);
				fortuneRepository.save(fortune);
				return true;
			})
			.orElse(false);
	}

	private List<Fortune> getMonthlyFortunes(YearMonth targetMonth) {
		return fortuneRepository.findAllByTargetDateBetweenOrderByTargetDateAsc(
			targetMonth.atDay(1),
			targetMonth.atEndOfMonth()
		);
	}

	private Fortune getDailyFortune(LocalDate targetDate) {
		return fortuneRepository.findByTargetDate(targetDate)
			.orElseThrow(() -> new IllegalArgumentException("해당 일자의 운세 데이터가 없습니다."));
	}

	private boolean hasGeneratedImages(List<Fortune> fortunes) {
		return !fortunes.isEmpty()
			&& fortunes.stream().allMatch(fortune -> StringUtils.hasText(fortune.getImageUrl()));
	}

	private FortuneGenerationJob getJob(YearMonth yearMonth) {
		return fortuneGenerationJobRepository.findByTargetMonth(yearMonth.atDay(1))
			.orElseThrow(() -> new IllegalArgumentException("해당 월의 운세 자동 생성 작업 이력이 없습니다."));
	}

	private boolean isFortuneGenerationInProgress(FortuneGenerationJobStatus status) {
		return status == FortuneGenerationJobStatus.GENERATING_FORTUNES;
	}

	private boolean isFortuneGenerationAlreadyPrepared(FortuneGenerationJobStatus status) {
		return status == FortuneGenerationJobStatus.WAITING_FOR_APPROVAL
			|| status == FortuneGenerationJobStatus.APPROVED
			|| status == FortuneGenerationJobStatus.GENERATING_IMAGES
			|| status == FortuneGenerationJobStatus.COMPLETED;
	}

	private boolean isImageGenerationRetryable(
		FortuneGenerationJob job,
		boolean force
	) {
		if (job.getStatus() == FortuneGenerationJobStatus.APPROVED) {
			return true;
		}

		if (job.getStatus() == FortuneGenerationJobStatus.FAILED && job.getApprovedAt() != null) {
			return true;
		}

		return force && job.getStatus() == FortuneGenerationJobStatus.COMPLETED;
	}

	private boolean isReviewWebhookStatus(FortuneGenerationJobStatus status) {
		return status == FortuneGenerationJobStatus.WAITING_FOR_APPROVAL
			|| status == FortuneGenerationJobStatus.FAILED
			|| status == FortuneGenerationJobStatus.GENERATING_FORTUNES;
	}

	private YearMonth parseTargetMonth(String targetMonth) {
		if (!StringUtils.hasText(targetMonth)) {
			throw new IllegalArgumentException("targetMonth는 yyyy-MM 형식이어야 합니다.");
		}
		try {
			return YearMonth.parse(targetMonth, TARGET_MONTH_FORMATTER);
		} catch (DateTimeParseException e) {
			throw new IllegalArgumentException("targetMonth는 yyyy-MM 형식이어야 합니다.");
		}
	}

	private LocalDate parseTargetDate(String targetDate) {
		if (!StringUtils.hasText(targetDate)) {
			throw new IllegalArgumentException("targetDate는 yyyy-MM-dd 형식이어야 합니다.");
		}
		try {
			return LocalDate.parse(targetDate);
		} catch (DateTimeParseException e) {
			throw new IllegalArgumentException("targetDate는 yyyy-MM-dd 형식이어야 합니다.");
		}
	}

	private void assertSingleImageTarget(
		String targetMonth,
		String targetDate
	) {
		if (StringUtils.hasText(targetMonth) && StringUtils.hasText(targetDate)) {
			throw new IllegalArgumentException("targetMonth와 targetDate는 동시에 사용할 수 없습니다.");
		}
		if (!StringUtils.hasText(targetMonth) && !StringUtils.hasText(targetDate)) {
			throw new IllegalArgumentException("targetMonth 또는 targetDate가 필요합니다.");
		}
	}

	private String truncateFailureMessage(String message) {
		if (!StringUtils.hasText(message)) {
			return "운세 자동 생성 작업이 실패했습니다.";
		}
		if (message.length() <= 1000) {
			return message;
		}
		return message.substring(0, 997) + "...";
	}
}
