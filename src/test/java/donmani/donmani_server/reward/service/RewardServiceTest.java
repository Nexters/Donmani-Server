package donmani.donmani_server.reward.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import donmani.donmani_server.feedback.entity.Feedback;
import donmani.donmani_server.feedback.repository.FeedbackRepository;
import donmani.donmani_server.reward.dto.HiddenUpdateRequestDTO;
import donmani.donmani_server.reward.entity.RewardItem;
import donmani.donmani_server.reward.entity.UserItem;
import donmani.donmani_server.reward.repository.RewardItemRepository;
import donmani.donmani_server.reward.repository.UserEquippedItemRepository;
import donmani.donmani_server.reward.repository.UserItemRepository;
import donmani.donmani_server.user.entity.User;
import donmani.donmani_server.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class RewardServiceTest {

    @Mock
    private RewardItemRepository rewardItemRepository;

    @Mock
    private UserItemRepository userItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private UserEquippedItemRepository userEquippedItemRepository;

    @InjectMocks
    private RewardService rewardService;

    @Test
    void acquireRandomItemsUsesLockedUserAndDoesNotGrantVisibleItemWhenCountIsAtMax() {
        User user = user(1L, "user-1");
        when(userRepository.findByIdentifierForUpdate("user-1")).thenReturn(Optional.of(user));
        when(userItemRepository.countVisibleItemsByUser(user)).thenReturn(12L);

        rewardService.acquireRandomItems("user-1", LocalDate.of(2026, 7, 23));

        verify(userRepository).findByIdentifierForUpdate("user-1");
        verify(userItemRepository, never()).save(any(UserItem.class));
        verify(rewardItemRepository, never()).findAllVisibleItemsExcludingDefaults();
    }

    @Test
    void acquireRandomItemsKeepsUserNotFoundBehaviorWithLockedLookup() {
        when(userRepository.findByIdentifierForUpdate("missing-user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rewardService.acquireRandomItems("missing-user", LocalDate.of(2026, 7, 23)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("USER NOT FOUND");
    }

    @Test
    void openItemsGrantsHiddenItemWhenVisibleItemCountIsExactlyMaxAndUserHasNoHiddenItem() {
        User user = user(1L, "user-1");
        UserItem unopenedItem = userItem(user, rewardItem(1L, false), false);
        RewardItem hiddenItem = rewardItem(23L, true);
        stubOpenItemsBase(user, List.of(unopenedItem));
        when(userItemRepository.countVisibleItemsByUser(user)).thenReturn(12L);
        when(userItemRepository.existsHiddenItemByUser(user)).thenReturn(false);
        when(rewardItemRepository.findFirstByHiddenTrue()).thenReturn(Optional.of(hiddenItem));

        rewardService.openItems("user-1");

        ArgumentCaptor<UserItem> userItemCaptor = ArgumentCaptor.forClass(UserItem.class);
        verify(userItemRepository).save(userItemCaptor.capture());
        assertThat(userItemCaptor.getValue().getUser()).isEqualTo(user);
        assertThat(userItemCaptor.getValue().getItem()).isEqualTo(hiddenItem);
        assertThat(userItemCaptor.getValue().isOpened()).isFalse();
    }

    @Test
    void openItemsDoesNotGrantHiddenItemWhenVisibleItemCountIsUnderMax() {
        User user = user(1L, "user-1");
        UserItem unopenedItem = userItem(user, rewardItem(1L, false), false);
        stubOpenItemsBase(user, List.of(unopenedItem));
        when(userItemRepository.countVisibleItemsByUser(user)).thenReturn(11L);

        rewardService.openItems("user-1");

        verify(userItemRepository, never()).save(any(UserItem.class));
        verify(userItemRepository, never()).existsHiddenItemByUser(user);
        verify(rewardItemRepository, never()).findFirstByHiddenTrue();
    }

    @Test
    void openItemsDoesNotGrantHiddenItemWhenUserAlreadyHasHiddenItem() {
        User user = user(1L, "user-1");
        UserItem unopenedItem = userItem(user, rewardItem(1L, false), false);
        stubOpenItemsBase(user, List.of(unopenedItem));
        when(userItemRepository.countVisibleItemsByUser(user)).thenReturn(12L);
        when(userItemRepository.existsHiddenItemByUser(user)).thenReturn(true);

        rewardService.openItems("user-1");

        verify(userItemRepository, never()).save(any(UserItem.class));
        verify(rewardItemRepository, never()).findFirstByHiddenTrue();
    }

    @Test
    void openItemsKeepsUserNotFoundBehaviorWithLockedLookup() {
        when(userRepository.findByIdentifierForUpdate("missing-user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rewardService.openItems("missing-user"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("USER NOT FOUND");
    }

    @Test
    void updateHiddenReadOpensUnopenedHiddenItem() {
        User user = user("user-1");
        UserItem hiddenItem = UserItem.builder()
                .user(user)
                .isOpened(false)
                .build();
        when(userRepository.findByIdentifier("user-1")).thenReturn(Optional.of(user));
        when(userItemRepository.findOneUnopenedHiddenItem(user)).thenReturn(Optional.of(hiddenItem));

        rewardService.updateHiddenRead(hiddenUpdateRequest("user-1"));

        assertThat(hiddenItem.isOpened()).isTrue();
        verify(userItemRepository).save(hiddenItem);
    }

    @Test
    void updateHiddenReadDoesNothingWhenNoUnopenedHiddenItemExists() {
        User user = user("user-1");
        when(userRepository.findByIdentifier("user-1")).thenReturn(Optional.of(user));
        when(userItemRepository.findOneUnopenedHiddenItem(user)).thenReturn(Optional.empty());

        assertThatCode(() -> rewardService.updateHiddenRead(hiddenUpdateRequest("user-1")))
                .doesNotThrowAnyException();

        verify(userItemRepository, never()).save(any(UserItem.class));
    }

    @Test
    void updateHiddenReadKeepsUserNotFoundBehavior() {
        when(userRepository.findByIdentifier("missing-user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rewardService.updateHiddenRead(hiddenUpdateRequest("missing-user")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("USER NOT FOUND");
    }

    private HiddenUpdateRequestDTO hiddenUpdateRequest(String userKey) {
        return HiddenUpdateRequestDTO.builder()
                .userKey(userKey)
                .year(2026)
                .month(7)
                .build();
    }

    private User user(String userKey) {
        return user(null, userKey);
    }

    private User user(Long id, String userKey) {
        User user = User.builder()
                .userKey(userKey)
                .build();
        user.setId(id);
        return user;
    }

    private void stubOpenItemsBase(User user, List<UserItem> notOpenedItems) {
        when(userRepository.findByIdentifierForUpdate(user.getUserKey())).thenReturn(Optional.of(user));
        when(feedbackRepository.findFeedbackByIsOpenedOrderByCreatedDateDesc(user.getId()))
                .thenReturn(List.of(feedback(user)));
        when(userItemRepository.findByUserNotOpened(user)).thenReturn(notOpenedItems);
    }

    private Feedback feedback(User user) {
        return Feedback.builder()
                .user(user)
                .createdDate(LocalDateTime.of(2026, 7, 23, 9, 0))
                .isOpened(false)
                .build();
    }

    private UserItem userItem(User user, RewardItem rewardItem, boolean isOpened) {
        return UserItem.builder()
                .user(user)
                .item(rewardItem)
                .acquiredAt(LocalDateTime.of(2026, 7, 23, 9, 0))
                .isOpened(isOpened)
                .build();
    }

    private RewardItem rewardItem(Long id, boolean isHidden) {
        return RewardItem.builder()
                .id(id)
                .name("reward-" + id)
                .isHidden(isHidden)
                .build();
    }

}
