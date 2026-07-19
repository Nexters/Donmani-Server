package donmani.donmani_server.fcm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import donmani.donmani_server.fcm.entity.FCMToken;
import donmani.donmani_server.fcm.repository.FCMLogRepository;
import donmani.donmani_server.fcm.repository.FCMTokenRepository;
import donmani.donmani_server.fcm.repository.PushExpenseRepository;
import donmani.donmani_server.user.entity.User;
import donmani.donmani_server.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class FCMServiceTest {

	@Mock
	private FCMTokenRepository fcmTokenRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private PushExpenseRepository expenseRepository;

	@Mock
	private FCMLogRepository fcmLogRepository;

	@InjectMocks
	private FCMService fcmService;

	@Test
	void saveOrUpdateTokenSavesNewTokenForNewUser() {
		User user = user(1L, "user-1");
		when(userRepository.findByIdentifier("user-1")).thenReturn(Optional.of(user));
		when(fcmTokenRepository.findByToken("new-token")).thenReturn(Optional.empty());
		when(fcmTokenRepository.findByUser(user)).thenReturn(Optional.empty());

		fcmService.saveOrUpdateToken("user-1", "\"new-token\"");

		ArgumentCaptor<FCMToken> tokenCaptor = ArgumentCaptor.forClass(FCMToken.class);
		verify(fcmTokenRepository).save(tokenCaptor.capture());
		assertThat(tokenCaptor.getValue().getUser()).isEqualTo(user);
		assertThat(tokenCaptor.getValue().getToken()).isEqualTo("new-token");
	}

	@Test
	void saveOrUpdateTokenUpdatesExistingUserToken() {
		User user = user(1L, "user-1");
		FCMToken existingToken = fcmToken(user, "old-token");
		when(userRepository.findByIdentifier("user-1")).thenReturn(Optional.of(user));
		when(fcmTokenRepository.findByToken("new-token")).thenReturn(Optional.empty());
		when(fcmTokenRepository.findByUser(user)).thenReturn(Optional.of(existingToken));

		fcmService.saveOrUpdateToken("user-1", "new-token");

		assertThat(existingToken.getToken()).isEqualTo("new-token");
		verify(fcmTokenRepository).save(existingToken);
	}

	@Test
	void saveOrUpdateTokenDoesNothingWhenCurrentUserAlreadyHasSameToken() {
		User user = user(1L, "user-1");
		FCMToken existingToken = fcmToken(user, "same-token");
		when(userRepository.findByIdentifier("user-1")).thenReturn(Optional.of(user));
		when(fcmTokenRepository.findByToken("same-token")).thenReturn(Optional.of(existingToken));

		fcmService.saveOrUpdateToken("user-1", "same-token");

		verify(fcmTokenRepository, never()).save(any(FCMToken.class));
		verify(fcmTokenRepository, never()).delete(any(FCMToken.class));
	}

	@Test
	void saveOrUpdateTokenMovesTokenOwnerToCurrentUser() {
		User currentUser = user(1L, "user-1");
		User previousUser = user(2L, "user-2");
		FCMToken existingToken = fcmToken(previousUser, "shared-token");
		when(userRepository.findByIdentifier("user-1")).thenReturn(Optional.of(currentUser));
		when(fcmTokenRepository.findByToken("shared-token")).thenReturn(Optional.of(existingToken));
		when(fcmTokenRepository.findByUser(currentUser)).thenReturn(Optional.empty());

		fcmService.saveOrUpdateToken("user-1", "shared-token");

		assertThat(existingToken.getUser()).isEqualTo(currentUser);
		verify(fcmTokenRepository).save(existingToken);
		verify(fcmTokenRepository, never()).delete(any(FCMToken.class));
	}

	@Test
	void saveOrUpdateTokenDeletesConflictingTokenBeforeUpdatingCurrentUserToken() {
		User currentUser = user(1L, "user-1");
		User previousUser = user(2L, "user-2");
		FCMToken currentUserToken = fcmToken(currentUser, "old-token");
		FCMToken conflictingToken = fcmToken(previousUser, "shared-token");
		when(userRepository.findByIdentifier("user-1")).thenReturn(Optional.of(currentUser));
		when(fcmTokenRepository.findByToken("shared-token")).thenReturn(Optional.of(conflictingToken));
		when(fcmTokenRepository.findByUser(currentUser)).thenReturn(Optional.of(currentUserToken));

		fcmService.saveOrUpdateToken("user-1", "shared-token");

		assertThat(currentUserToken.getToken()).isEqualTo("shared-token");
		InOrder inOrder = inOrder(fcmTokenRepository);
		inOrder.verify(fcmTokenRepository).delete(conflictingToken);
		inOrder.verify(fcmTokenRepository).flush();
		inOrder.verify(fcmTokenRepository).save(currentUserToken);
	}

	private User user(Long id, String userKey) {
		User user = User.builder()
			.userKey(userKey)
			.build();
		user.setId(id);
		return user;
	}

	private FCMToken fcmToken(User user, String token) {
		return FCMToken.builder()
			.user(user)
			.token(token)
			.build();
	}
}
