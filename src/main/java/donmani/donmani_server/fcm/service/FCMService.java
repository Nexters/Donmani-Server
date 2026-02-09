package donmani.donmani_server.fcm.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import donmani.donmani_server.fcm.entity.FCMToken;
import donmani.donmani_server.fcm.entity.FcmLog;
import donmani.donmani_server.fcm.entity.NotificationType;
import donmani.donmani_server.fcm.entity.PushStatus;
import donmani.donmani_server.fcm.repository.FCMLogRepository;
import donmani.donmani_server.fcm.repository.FCMTokenRepository;
import donmani.donmani_server.fcm.repository.PushExpenseRepository;
import donmani.donmani_server.user.entity.User;
import donmani.donmani_server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FCMService {

	private final FCMTokenRepository fcmTokenRepository;
	private final UserRepository userRepository;
	private final PushExpenseRepository expenseRepository;
	private final FCMLogRepository fcmLogRepository;

	@Transactional
	public void saveOrUpdateToken(String userKey, String token) {
		User user = userRepository.findByIdentifier(userKey).orElseThrow(() -> new RuntimeException("USER NOT FOUND"));

		String removeQuotesToken = removeQuotes(token);
		FCMToken fcmToken = fcmTokenRepository.findByUser(user)
			.map(existingToken -> {
				existingToken.setToken(removeQuotesToken);
				return existingToken;
			}).orElseGet(() -> FCMToken.builder()
				.user(user)
				.token(removeQuotesToken)
				.build()
			);

		fcmTokenRepository.save(fcmToken);
	}

	private String removeQuotes(String input) {
		return (input.startsWith("\"") && input.endsWith("\"")) ?
			input.substring(1, input.length() - 1) : input;
	}

	@Transactional
	public void sendMessage(
		User user,
		String userToken,
		NotificationType notificationType,
		String title,
		String content
	) {
		// 1. FCM 인스턴스 세팅
		Message message = Message.builder()
			.setToken(userToken)
			.setNotification(Notification.builder()
				.setTitle(title)
				.setBody(content)
				.build())
			.putData("notificationType", notificationType.name()) // TODO : 운세 정보까지 같이 던져줄지
			.build();

		// 2. FCM 로그 저장
		FcmLog fcmlog = saveFcmLog(
			user,
			userToken,
			notificationType,
			PushStatus.PENDING,
			null,
			null
		);

		try {
			// 3. FCM 발송
			FirebaseMessaging.getInstance().send(message);

			// 4-1. FCM 로그 갱신 (성공)
			fcmlog.updateStatus(PushStatus.SUCCESS, null, null);
		} catch (FirebaseMessagingException e) {
			String errorCode = e.getMessagingErrorCode().name();
			String errorMessage = e.getMessage();

			// 4-2. FCM 로그 갱신 (실패)
			fcmlog.updateStatus(PushStatus.FAIL, errorCode, errorMessage);
		}
	}

	@Transactional
	public List<String> getTokenNoExpenseToday() {
		return expenseRepository.findTokensWithoutExpenseToday();
	}

	@Transactional
	public List<String> getTokenNoExpenseYesterday() {
		return expenseRepository.findTokensWithoutExpenseSince(LocalDateTime.now().minusDays(1));
	}

	@Transactional
	public FcmLog saveFcmLog(
		User user,
		String userToken,
		NotificationType notificationType,
		PushStatus status,
		String errorCode,
		String errorMessage
	) {
		FcmLog fcmlog = FcmLog.builder()
			.user(user)
			.fcmTokenSnapshot(userToken)
			.notificationType(notificationType)
			.status(status)
			.errorCode(errorCode)
			.errorMessage(errorMessage)
			.build();

		return fcmLogRepository.save(fcmlog);
	}
}
