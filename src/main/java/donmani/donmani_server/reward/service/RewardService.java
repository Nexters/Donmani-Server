package donmani.donmani_server.reward.service;

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

    private final static int MAX_REWARD = 19; // default item 5개는 카운트에 불포함

    /**
     * 기록과 동시에 선물 획득
     * @param userKey
     */
    @Transactional
    public void acquireRandomItems(String userKey, LocalDate reqDate) {
        ZonedDateTime nowInSeoul = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
        LocalDate todayInSeoul = nowInSeoul.toLocalDate();

        if(reqDate.getYear() == todayInSeoul.getYear() &&
                reqDate.getMonthValue() == todayInSeoul.getMonthValue()) {
            // 어제 기록이 지난달이면 선물 받으면 안됨.
            return;
        }

        User user = userRepository.findByUserKey(userKey).orElseThrow(() -> new RuntimeException("USER NOT FOUND"));

        LocalDateTime start = YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime end = start.plusMonths(1).minusNanos(1); // 23:59:59.999999999

        List<UserItem> acquiredItems = userItemRepository.findByUserAndAcquiredAtBetween(user, start, end);

        if(acquiredItems.size() == MAX_REWARD) {
            // 인당 월 최대 14개의 선물 생성 가능
            // throw new IllegalStateException("이번 달 받을 수 있는 선물 개수를 초과하였습니다.");
            return;
        }

        // user가 가지고 있지 않은 아이템 중 랜덤으로 획득 (히든 제외)
        List<RewardItem> allItems = rewardItemRepository.findAllByHiddenFalse();
        List<RewardItem> availableRewards = allItems.stream()
                .filter(item -> !acquiredItems.contains(item))
                .collect(Collectors.toList());

        RewardItem selected = availableRewards.get(new Random().nextInt(availableRewards.size()));

        UserItem newUserItem = UserItem.builder()
                .user(user)
                .item(selected)
                .acquiredAt(LocalDateTime.now())
                .isOpened(false)
                .build();

        userItemRepository.save(newUserItem);
    }

    /**
     * 열지 않은 선물의 개수
     * @param userKey
     * @return
     */
    @Transactional(readOnly = true)
    public int getNotOpenedItemSize(String userKey) {
        User user = userRepository.findByUserKey(userKey).orElseThrow(() -> new RuntimeException("USER NOT FOUND"));

        LocalDateTime start = YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime end = start.plusMonths(1).minusNanos(1); // 23:59:59.999999999

        List<UserItem> notOpenedItems = userItemRepository.findByUserAndAcquiredAtBetweenAndNotOpened(user, start, end);

        return notOpenedItems.size();
    }

    /***
     * 선물 열기
     * @param userKey
     */
    @Transactional
    public List<RewardItemResponseDTO> openItems(String userKey) {
        User user = userRepository.findByUserKey(userKey).orElseThrow(() -> new RuntimeException("USER NOT FOUND"));

        LocalDateTime start = YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime end = start.plusMonths(1).minusNanos(1); // 23:59:59.999999999

        // 피드백 열기 (중간 이탈해도 피드백+선물 한 set으로 열기)
        Feedback notOpenedFeedback = feedbackRepository.findFeedbackByIsOpenedOrderByCreatedDateDesc(user.getId()).get(0);
        notOpenedFeedback.setOpened(true);

        // 선물 열기
        List<UserItem> notOpenedItems = userItemRepository.findByUserAndAcquiredAtBetweenAndNotOpened(user, start, end);
        for (UserItem item : notOpenedItems) {
            item.setOpened(true);
        }

        feedbackRepository.save(notOpenedFeedback);
        userItemRepository.saveAll(notOpenedItems);

        // 히든 아이템 획득
        openHiddenItems(user, start, end);;

        List<RewardItemResponseDTO> response = new ArrayList<>();
        for (UserItem item : notOpenedItems) {
            response.add(RewardItemResponseDTO.of(item.getItem()));
        }

        return response;
    }

    private void openHiddenItems(User user, LocalDateTime start, LocalDateTime end) {
        List<UserItem> acquiredItems = userItemRepository.findByUserAndAcquiredAtBetween(user, start, end);
        if(acquiredItems.size() == MAX_REWARD) {
            RewardItem hiddenItem = rewardItemRepository.findFirstByHiddenTrue().orElseThrow();
            UserItem newUserItem = UserItem.builder()
                    .user(user)
                    .item(hiddenItem)
                    .acquiredAt(LocalDateTime.now())
                    .isOpened(false)
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
        User user = userRepository.findByUserKey(userKey).orElseThrow(() -> new RuntimeException("USER NOT FOUND"));

        LocalDateTime start = YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime end = start.plusMonths(1).minusNanos(1); // 23:59:59.999999999

        List<UserItem> acquiredItems = userItemRepository.findByUserAndAcquiredAtBetween(user, start, end);
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);

        // 유저가 소유한 RewardItem
        Set<Long> ownedItemIds = new HashSet<>();
        // 최근 3일 이내 획득한 RewardItem
        Set<Long> recentlyAcquiredItemIds = new HashSet<>();
        // 히든 아이템 처음 받았는지 여부 flag
        boolean newAcquiredHiddenItem = false;

        for (UserItem userItem : acquiredItems) {
            Long itemId = userItem.getItem().getId();
            ownedItemIds.add(itemId);
            if (userItem.getAcquiredAt().isAfter(threeDaysAgo)) {
                recentlyAcquiredItemIds.add(itemId);
            }

            // 히든 아이템을 갖고 있는데 isOpened가 fasle라면, 최초 1회도 꾸미기 탭 내에서 확인하지 않은 것으로 간주
            if(userItem.getItem().isHidden()) {
                if(!userItem.isOpened()) newAcquiredHiddenItem = true;
            }
        }

        // stream 내부 사용을 위해 얕은 복사
        boolean copyFlag = newAcquiredHiddenItem;

        List<RewardItemResponseDTO> dtos = rewardItemRepository.findAll().stream()
                .map(item -> {
                    boolean owned = ownedItemIds.contains(item.getId());
                    boolean newAcquired = recentlyAcquiredItemIds.contains(item.getId());
                    if(owned && item.isHidden()) {
                        newAcquired = copyFlag;
                    }
                    return RewardItemResponseDTO.of(item, owned, newAcquired);
                })
                .collect(Collectors.toList());

        return dtos.stream()
                .collect(Collectors.groupingBy(RewardItemResponseDTO::getCategory));
    }

    @Transactional
    public void updateHiddenRead(HiddenUpdateRequestDTO request) {
        User user = userRepository.findByUserKey(request.getUserKey()).orElseThrow(() -> new RuntimeException("USER NOT FOUND"));

        UserItem findItem = userItemRepository.findOneUnopenedHiddenItem(user, request.getYear(), request.getMonth()).orElseThrow();

        findItem.setOpened(true);

        userItemRepository.save(findItem);
    }

    /**
     * 꾸미기 상태 저장
     */
    @Transactional
    public void saveItem(RewardItemSaveRequestDTO request) {
        User user = userRepository.findByUserKey(request.getUserKey()).orElseThrow(() -> new RuntimeException("USER NOT FOUND"));

        LocalDateTime now = LocalDateTime.now();
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

        Optional<UserEquippedItem> equippedItem = userEquippedItemRepository.findByUserAndSavedAtInCurrentMonth(user, year, month);

        RewardItem updateBackground = rewardItemRepository.findById(request.getBackgroundId()).orElseThrow();
        RewardItem updateEffect = rewardItemRepository.findById(request.getEffectId()).orElseThrow();
        RewardItem updateDecoration = rewardItemRepository.findById(request.getDecorationId()).orElseThrow();
        RewardItem updateByeoltongCase = rewardItemRepository.findById(request.getByeoltongCaseId()).orElseThrow();
        RewardItem updateBgm = rewardItemRepository.findById(request.getBgmId()).orElseThrow();

        if(equippedItem.isPresent()) {
            UserEquippedItem presentEquippedItem = equippedItem.get();
            presentEquippedItem.updateEquippedStatus(updateBackground, updateEffect, updateDecoration, updateByeoltongCase, updateBgm, now);

            userEquippedItemRepository.save(presentEquippedItem);
        }
        else {
            UserEquippedItem newEquippedItem = UserEquippedItem.builder()
                    .user(user)
                    .background(updateBackground)
                    .effect(updateEffect)
                    .decoration(updateDecoration)
                    .byeoltongCase(updateByeoltongCase)
                    .bgm(updateBgm)
                    .savedAt(now)
                    .build();

            userEquippedItemRepository.save(newEquippedItem);
        }

    }

    /**
     * 꾸미기 상태 불러오기
     */
    @Transactional
    public List<RewardItemResponseDTO> getSavedItem(String userKey, int year, int month) {
        List<RewardItemResponseDTO> savedItems = new ArrayList<>();
        RewardItem background = null, effect = null, decoration = null, byeoltongCase = null, bgm = null;

        User user = userRepository.findByUserKey(userKey).orElseThrow(() -> new RuntimeException("USER NOT FOUND"));

        Optional<UserEquippedItem> savedItem = userEquippedItemRepository.findByUserAndSavedAtInCurrentMonth(user, year, month);

        if(savedItem.isPresent()) {
            UserEquippedItem presentSavedItem = savedItem.get();

            background = presentSavedItem.getBackground();
            effect = presentSavedItem.getEffect();
            decoration = presentSavedItem.getDecoration();
            byeoltongCase = presentSavedItem.getByeoltongCase();
            bgm = presentSavedItem.getBgm();
        } else {
            // 꾸미기 저장 데이터 없으면 default 세팅
            background = rewardItemRepository.findById(1L).orElseThrow();
            effect = rewardItemRepository.findById(2L).orElseThrow();
            decoration = rewardItemRepository.findById(3L).orElseThrow();
            byeoltongCase = rewardItemRepository.findById(4L).orElseThrow();
            bgm = rewardItemRepository.findById(5L).orElseThrow();

            // default 세팅으로 저장
            UserEquippedItem newEquippedItem = UserEquippedItem.builder()
                    .user(user)
                    .background(background)
                    .effect(effect)
                    .decoration(decoration)
                    .byeoltongCase(byeoltongCase)
                    .bgm(bgm)
                    .savedAt(LocalDateTime.now())
                    .build();

            // default 아이템도 userItem에 추가
            userItemRepository.save(makeUserItem(user, background));
            userItemRepository.save(makeUserItem(user, effect));
            userItemRepository.save(makeUserItem(user, decoration));
            userItemRepository.save(makeUserItem(user, byeoltongCase));
            userItemRepository.save(makeUserItem(user, bgm));
            userEquippedItemRepository.save(newEquippedItem);
        }

        savedItems.add(RewardItemResponseDTO.of(background));
        savedItems.add(RewardItemResponseDTO.of(effect));
        savedItems.add(RewardItemResponseDTO.of(decoration));
        savedItems.add(RewardItemResponseDTO.of(byeoltongCase));
        savedItems.add(RewardItemResponseDTO.of(bgm));

        return savedItems;
    }

    private UserItem makeUserItem(User user, RewardItem item) {
        return UserItem.builder()
                .user(user)
                .item(item)
                .acquiredAt(LocalDateTime.now())
                .isOpened(true).build();
    }
}
