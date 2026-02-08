package donmani.donmani_server.fcm.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import donmani.donmani_server.fcm.dto.FortuneResponseV1;
import donmani.donmani_server.fcm.entity.Fortune;
import donmani.donmani_server.fcm.entity.FortuneHistory;
import donmani.donmani_server.fcm.entity.ReadSource;
import donmani.donmani_server.fcm.repository.FortuneHistoryRepository;
import donmani.donmani_server.fcm.repository.FortuneRepository;
import donmani.donmani_server.user.entity.User;
import donmani.donmani_server.user.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FortuneService {

	private final FortuneRepository fortuneRepository;
	private final FortuneHistoryRepository fortuneHistoryRepository;
	private final FCMService fcmService;
	private final UserService userService;

	/**
	 * 유저에게 운세를 푸시 알림으로 발송합니다.
	 *
	 * <p>
	 * 1. 현재 날짜(KST)에 해당하는 운세를 조회합니다.<br>
	 * 2. 유저 정보를 조회합니다.<br>
	 * 2. 운세 이력을 저장합니다.<br>
	 * 3. FCM을 통해 푸시 알림 발송하고, 발송 이력을 저장합니다.
	 * </p>
	 *
	 * @param token 유저 식별 및 FCM 전송을 위한 디바이스/유저 토큰
	 * @return Void
	 * @throws EntityNotFoundException 운세 정보 없음
	 * @throws IllegalArgumentException 유저 정보 없음
	 *
	 */
	@Transactional
	public void sendDailyFortune(String token) {
		// 1. 금일 운세 데이터 조회
		LocalDate localDate = LocalDate.now(ZoneId.of("Asia/Seoul"));

		Fortune dailyFortune = fortuneRepository.findByTargetDate(localDate)
			.orElseThrow(() -> new EntityNotFoundException("오늘의 운세가 없습니다."));

		// 2. 유저 확인
		User user = userService.getUser(token);

		// 3. 운세 이력 저장
		FortuneHistory history = FortuneHistory.builder()
			.fortune(dailyFortune)
			.user(user)
			.build();

		fortuneHistoryRepository.save(history);

		// 4. 푸시 알림 발송 및 이력 저장
		fcmService.sendMessage(token, dailyFortune.getTitle(), dailyFortune.getContent());
	}

	@Transactional
	public void sendDailyFortuneTest(String testToken) {
		// 1. 금일 운세 데이터 조회
		LocalDate localDate = LocalDate.now(ZoneId.of("Asia/Seoul"));

		Fortune dailyFortune = fortuneRepository.findByTargetDate(localDate)
			.orElseThrow(() -> new EntityNotFoundException("오늘의 운세가 없습니다."));

		// 2. 유저 확인
		User user = userService.getUser(testToken);

		// 3. 운세 이력 저장
		FortuneHistory history = FortuneHistory.builder()
			.fortune(dailyFortune)
			.user(user)
			.build();

		fortuneHistoryRepository.save(history);

		// 4. 푸시 알림 발송 및 이력 저장
		fcmService.sendMessage(testToken, dailyFortune.getTitle(), dailyFortune.getContent());
	}

	/**
	 * 유저의 운세를 조회합니다
	 *
	 * @param userKey 유저 고유 키
	 * @param targetDate 운세 일자
	 * @return 운세 응답 DTO (V1)
	 * @throws EntityNotFoundException 운세 정보 없음
	 */
	@Transactional(readOnly = true)
	public FortuneResponseV1 getDailyFortune(
		String userKey,
		LocalDate targetDate
	) {
		FortuneHistory fortuneHistory = fortuneHistoryRepository.findFortuneByTargetDate(userKey, targetDate)
			.orElseThrow(() -> new EntityNotFoundException("오늘의 운세가 없습니다."));

		return FortuneResponseV1.from(fortuneHistory.getFortune());
	}

	/**
	 * 유저가 운세를 읽었을 때 읽음 시각과 진입 경로를 기록합니다.
	 *
	 * @param userKey 유저 고유 키
	 * @param readSource 운세 진입 경로
	 * @param localDateTime 운세 열람 시각
	 * @return void
	 * @throws EntityNotFoundException 운세 정보 없음
	 */
	@Transactional
	public void markFortuneAsRead(
		String userKey,
		ReadSource readSource,
		LocalDateTime localDateTime
	) {
		FortuneHistory fortuneHistory = fortuneHistoryRepository.findFortuneByTargetDate(userKey,
				localDateTime.toLocalDate())
			.orElseThrow(() -> new EntityNotFoundException("조회할 운세 이력이 없습니다."));

		fortuneHistory.markAsRead(readSource, localDateTime);
	}
}