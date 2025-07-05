package donmani.donmani_server.reward.service;

import donmani.donmani_server.common.exception.HiddenItemAlreadyOpenedException;
import donmani.donmani_server.feedback.entity.Feedback;
import donmani.donmani_server.feedback.repository.FeedbackRepository;
import donmani.donmani_server.reward.dto.HiddenUpdateRequestDTO;
import donmani.donmani_server.reward.dto.RewardItemResponseDTO;
import donmani.donmani_server.reward.dto.RewardItemSaveRequestDTO;
import donmani.donmani_server.reward.entity.RewardCategory;
import donmani.donmani_server.reward.entity.RewardItem;
import donmani.donmani_server.reward.entity.UserEquippedItem;
import donmani.donmani_server.reward.entity.UserItem;
import donmani.donmani_server.reward.repository.RewardItemRepository;
import donmani.donmani_server.reward.repository.UserEquippedItemRepository;
import donmani.donmani_server.reward.repository.UserItemRepository;
import donmani.donmani_server.user.entity.User;
import donmani.donmani_server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RewardService {
    private final RewardItemRepository rewardItemRepository;
    private final UserItemRepository userItemRepository;
    private final UserRepository userRepository;
    private final FeedbackRepository feedbackRepository;
    private final UserEquippedItemRepository userEquippedItemRepository;

    private final static int MAX_REWARD = 12;

    /**
     * 기록과 동시에 선물 획득
     * @param userKey
     */
    @Transactional
    public void acquireRandomItems(String userKey, LocalDate reqDate) {
        User user = userRepository.findByUserKey(userKey).orElseThrow(() -> new RuntimeException("USER NOT FOUND"));

        List<UserItem> acquiredItems = userItemRepository.findByUserNotOpened(user);
        Set<Long> acquiredItemIds = acquiredItems.stream()
                .map(userItem -> userItem.getItem().getId())
                .collect(Collectors.toSet());

        if(acquiredItems.size() == MAX_REWARD) {
            // 인당 최대 12개의 선물 생성 가능
            return;
        }

        // user가 가지고 있지 않은 아이템 중 랜덤으로 획득 (히든, 디폴트 제외)
        List<RewardItem> allItems = rewardItemRepository.findAllVisibleItemsExcludingDefaults();

        List<RewardItem> availableRewards = allItems.stream()
                .filter(item -> !acquiredItemIds.contains(item.getId()))
                .collect(Collectors.toList());

        if (!availableRewards.isEmpty()) {
            RewardItem selected = availableRewards.get(new Random().nextInt(availableRewards.size()));

            UserItem newUserItem = UserItem.builder()
                .user(user)
                .item(selected)
                .acquiredAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
                .isOpened(false)
                .build();

            userItemRepository.save(newUserItem);
        }
    }

    /**
     * 열지 않은 선물의 개수
     * @param userKey
     * @return
     */
    @Transactional(readOnly = true)
    public int getNotOpenedItemSize(String userKey) {
        User user = userRepository.findByUserKey(userKey).orElseThrow(() -> new RuntimeException("USER NOT FOUND"));

        List<UserItem> notOpenedItems = userItemRepository.findByUserNotOpened(user);

        return notOpenedItems.size();
    }

    /***
     * 선물 열기
     * @param userKey
     */
    @Transactional
    public List<RewardItemResponseDTO> openItems(String userKey) {
        User user = userRepository.findByUserKey(userKey).orElseThrow(() -> new RuntimeException("USER NOT FOUND"));

        // 피드백 열기 (중간 이탈해도 피드백+선물 한 set으로 열기)
        Feedback notOpenedFeedback = feedbackRepository.findFeedbackByIsOpenedOrderByCreatedDateDesc(user.getId()).get(0);
        notOpenedFeedback.setOpened(true);

        // 선물 열기
        List<UserItem> notOpenedItems = userItemRepository.findByUserNotOpened(user);
        for (UserItem item : notOpenedItems) {
            item.setOpened(true);
        }

        feedbackRepository.save(notOpenedFeedback);
        userItemRepository.saveAll(notOpenedItems);

        // 히든 아이템 획득
        acquireHiddenItems(user);;

        List<RewardItemResponseDTO> response = new ArrayList<>();
        for (UserItem item : notOpenedItems) {
            response.add(RewardItemResponseDTO.of(item.getItem()));
        }

        return response;
    }

    private void acquireHiddenItems(User user) {
        List<UserItem> acquiredItems = userItemRepository.findByUserOrderByAcquiredAtDesc(user);
        if(acquiredItems.size() == MAX_REWARD) {
            RewardItem hiddenItem = rewardItemRepository.findFirstByHiddenTrue().orElseThrow();
            UserItem newUserItem = UserItem.builder()
                    .user(user)
                    .item(hiddenItem)
                    .acquiredAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
                    .isOpened(true)
                    .build();
            userItemRepository.save(newUserItem);
        }
    }

    /**
     * 꾸미기 탭 접속하여 아이템 리스트 조회 (해당월)
     * 전체 아이템 및 획득 아이템 표시
     * @param userKey
     * @return
     */
    @Transactional(readOnly = true)
    public Map<RewardCategory, List<RewardItemResponseDTO>> getAcquiredItem(String userKey) {
        User user = userRepository.findByUserKey(userKey)
                .orElseThrow(() -> new RuntimeException("USER NOT FOUND"));

        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        LocalDateTime threeDaysAgo = LocalDateTime.now(zoneId).minusDays(3);

        // 유저가 획득한 아이템
        List<UserItem> acquiredItems = userItemRepository.findByUserOrderByAcquiredAtDesc(user);

        // 기본 아이템 ID 목록
        List<Long> defaultItemIds = List.of(1L, 2L, 3L, 4L);

        // 기본 아이템들을 DTO로 변환
        List<RewardItemResponseDTO> response = defaultItemIds.stream()
                .map(id -> rewardItemRepository.findById(id)
                        .map(item -> RewardItemResponseDTO.of(item, false))
                        .orElseThrow(() -> new RuntimeException("Default item ID " + id + " not found")))
                .collect(Collectors.toList());

        response.addAll(
                acquiredItems.stream()
                .map(item -> {
                    // 3일 이내 획득한 Item
                    boolean newAcquired = item.getAcquiredAt().isAfter(threeDaysAgo);
                    // Hidden 아이템 처음 받았는지 여부
                    if(item.getItem().isHidden() && !item.isOpened()) {
                        newAcquired = true;
                    }

                    return RewardItemResponseDTO.of(item.getItem(), newAcquired);
                }).collect(Collectors.toList())
        );

        return response.stream()
                .collect(Collectors.groupingBy(RewardItemResponseDTO::getCategory));
    }

    @Transactional
    public void updateHiddenRead(HiddenUpdateRequestDTO request) {
        User user = userRepository.findByUserKey(request.getUserKey()).orElseThrow(() -> new RuntimeException("USER NOT FOUND"));

        UserItem findItem = userItemRepository.findOneUnopenedHiddenItem(user, request.getYear(), request.getMonth())
                .orElseThrow(HiddenItemAlreadyOpenedException::new);

        findItem.setOpened(true);

        userItemRepository.save(findItem);
    }

    /**
     * 꾸미기 상태 저장
     */
    @Transactional
    public void saveItem(RewardItemSaveRequestDTO request) {
        User user = userRepository.findByUserKey(request.getUserKey()).orElseThrow(() -> new RuntimeException("USER NOT FOUND"));

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        int year = now.getYear();
        int month = now.getMonthValue();

        if(year != request.getYear() || month != request.getMonth()) {
            /**
             * Client 단으로부터 요청 연도/월을 함께 받아 실제 저장 시각과 비교
             * edge case : 5월 31일 23:59에 꾸미기 탭 들어가 6월 1일 00:00시 저장 시
             *              5월 아이템으로 6월에 꾸미기가 저장되는 상황 방지
             */
            throw new IllegalArgumentException("유효하지 않은 요청입니다.");
        }

        RewardItem updateBackground = rewardItemRepository.findById(request.getBackgroundId()).orElseThrow();
        RewardItem updateEffect = rewardItemRepository.findById(request.getEffectId()).orElseThrow();
        RewardItem updateDecoration = rewardItemRepository.findById(request.getDecorationId()).orElseThrow();
        RewardItem updateByeoltongCase = rewardItemRepository.findById(request.getByeoltongCaseId()).orElseThrow();
        // RewardItem updateBgm = rewardItemRepository.findById(request.getBgmId()).orElseThrow();

        Optional<UserEquippedItem> equippedItem = userEquippedItemRepository.findTopByUserAndSavedAtInCurrentMonth(user.getId(), year, month);
        if(equippedItem.isPresent()) {
            UserEquippedItem presentEquippedItem = equippedItem.get();
            presentEquippedItem.updateEquippedStatus(updateBackground, updateEffect, updateDecoration, updateByeoltongCase, now);
            userEquippedItemRepository.save(presentEquippedItem);
        } else {
            UserEquippedItem savedEquippedItem = UserEquippedItem.builder()
                    .user(user)
                    .savedAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
                    .background(updateBackground)
                    .decoration(updateDecoration)
                    .effect(updateEffect)
                    .byeoltongCase(updateByeoltongCase)
                    .build();
            userEquippedItemRepository.save(savedEquippedItem);
        }

    }

    /**
     * 꾸미기 상태 불러오기
     */
    @Transactional
    public List<RewardItemResponseDTO> getSavedItem(String userKey, int year, int month) {
        List<RewardItemResponseDTO> savedItems = new ArrayList<>();
        RewardItem background = null, effect = null, decoration = null, byeoltongCase = null;

        User user = userRepository.findByUserKey(userKey).orElseThrow(() -> new RuntimeException("USER NOT FOUND"));

        Optional<UserEquippedItem> savedItem = userEquippedItemRepository.findTopByUserAndSavedAtInCurrentMonth(user.getId(), year, month);

        if(savedItem.isPresent()) {
            UserEquippedItem presentSavedItem = savedItem.get();

            background = presentSavedItem.getBackground();
            effect = presentSavedItem.getEffect();
            decoration = presentSavedItem.getDecoration();
            byeoltongCase = presentSavedItem.getByeoltongCase();
            // bgm = presentSavedItem.getBgm();
        } else {
            // 기준 날짜보다 이전 날짜 중 가장 최근에 저장된 꾸미기 상태 불러오기
            LocalDateTime beforeDate = LocalDateTime.of(year, month, 1, 0, 0);
            Optional<UserEquippedItem> lastEquippedItem = userEquippedItemRepository.findLeastBeforeDate(user.getId(), beforeDate);

            // 있다면 불러오고 해당 상태로 해당 월에 저장
            if(lastEquippedItem.isPresent()) {
                UserEquippedItem presentSavedItem = savedItem.get();

                background = presentSavedItem.getBackground();
                effect = presentSavedItem.getEffect();
                decoration = presentSavedItem.getDecoration();
                byeoltongCase = presentSavedItem.getByeoltongCase();

                UserEquippedItem savedEquippedItem = UserEquippedItem.builder()
                        .user(user)
                        .savedAt(beforeDate)
                        .background(background)
                        .decoration(decoration)
                        .effect(effect)
                        .byeoltongCase(byeoltongCase)
                        .build();
                userEquippedItemRepository.save(savedEquippedItem);
            } else {
                // 꾸미기 저장 데이터 없으면 default
                background = rewardItemRepository.findById(1L).orElseThrow();
                effect = rewardItemRepository.findById(2L).orElseThrow();
                decoration = rewardItemRepository.findById(3L).orElseThrow();
                byeoltongCase = rewardItemRepository.findById(4L).orElseThrow();
                // bgm = rewardItemRepository.findById(5L).orElseThrow();
            }
        }

        savedItems.add(RewardItemResponseDTO.of(background));
        savedItems.add(RewardItemResponseDTO.of(effect));
        savedItems.add(RewardItemResponseDTO.of(decoration));
        savedItems.add(RewardItemResponseDTO.of(byeoltongCase));
        // savedItems.add(RewardItemResponseDTO.of(bgm));

        return savedItems;
    }

    /**
     * 오픈하지 않은 선물 여부 확인
     * @param userKey
     * @return boolean
     */
    @Transactional(readOnly = true)
    public boolean hasNotOpenedRewards(String userKey) {
        User user = userRepository.findByUserKey(userKey)
            .orElseThrow(() -> new RuntimeException("USER NOT FOUND"));

        List<UserItem> notOpenedItems = userItemRepository.findByUserNotOpened(user);

        return notOpenedItems == null || notOpenedItems.isEmpty() ? false : true;
    }
}
