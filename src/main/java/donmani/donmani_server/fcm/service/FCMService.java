package donmani.donmani_server.fcm.service;


import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import donmani.donmani_server.fcm.entity.FCMToken;
import donmani.donmani_server.fcm.reposiory.FCMTokenRepository;
import donmani.donmani_server.fcm.reposiory.PushExpenseRepository;
import donmani.donmani_server.user.entity.User;
import donmani.donmani_server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FCMService {

    private final FCMTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;
    private final PushExpenseRepository expenseRepository;

    @Transactional
    public void saveOrUpdateToken(String userKey, String token) {
        User user = userRepository.findByUserKey(userKey).orElseThrow(() -> new RuntimeException("USER NOT FOUND"));

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

    public void sendMessage(String targetToken, String title, String body) {
        Message message = Message.builder()
                .setToken(targetToken)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();
        System.out.println();
        try {
            FirebaseMessaging.getInstance().send(message);
        } catch (FirebaseMessagingException e) {
            e.printStackTrace();
        }
    }

    public void sendMessageTest(String userKey) {
        User user = userRepository.findByUserKey(userKey).orElseThrow(() -> new RuntimeException("USER NOT FOUND"));
        FCMToken token = fcmTokenRepository.findByUser(user).orElseThrow();

        String title = "Test Title";
        String message = "Test Message, User : " + user.getName();
        sendMessage(token.getToken(), title, message);
    }
    @Transactional
    public List<String> getTokenNoExpenseToday() {
        return expenseRepository.findTokensWithoutExpenseToday();
    }

    @Transactional
    public List<String> getTokenNoExpenseYesterday() {
        return expenseRepository.findTokensWithoutExpenseSince(LocalDateTime.now().minusDays(1));
    }

}
