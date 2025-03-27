package donmani.donmani_server.fcm.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
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

import java.io.FileInputStream;
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

        FCMToken fcmToken = fcmTokenRepository.findByUser(user)
                .map(existingToken -> {
                    existingToken.setToken(token);
                    return existingToken;
                }).orElseGet(() -> FCMToken.builder()
                        .user(user)
                        .token(token)
                        .build()
                );

        fcmTokenRepository.save(fcmToken);
    }

    public void sendMessage(String targetToken, String title, String body) {
        Message message = Message.builder()
                .setToken(targetToken)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();

        try {
            FirebaseMessaging.getInstance().send(message);
        } catch (FirebaseMessagingException e) {
            e.printStackTrace();
        }
    }

    @Transactional
    public List<String> getTokenNoExpenseToday() {
        return expenseRepository.findTokensWithoutExpenseToday();
    }

    @Transactional
    public List<String> getTokenNoExpenseYesterday() {
        return expenseRepository.findTokensWithoutExpenseYesterday();
    }

}
