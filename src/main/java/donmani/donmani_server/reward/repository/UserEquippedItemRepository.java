package donmani.donmani_server.reward.repository;

import donmani.donmani_server.reward.entity.UserEquippedItem;
import donmani.donmani_server.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserEquippedItemRepository extends JpaRepository<UserEquippedItem, Long> {
    @Query(value = "SELECT * FROM user_equipped_item e " +
            "WHERE e.user_id = :userId " +
            "AND YEAR(e.saved_at) = :year " +
            "AND MONTH(e.saved_at) = :month " +
            "ORDER BY e.saved_at DESC " +
            "LIMIT 1", nativeQuery = true)
    Optional<UserEquippedItem> findTopByUserAndSavedAtInCurrentMonth(
            @Param("userId") Long userId,
            @Param("year") int year,
            @Param("month") int month
    );

    @Query(value = "SELECT * FROM user_equipped_item e " +
            "WHERE e.user_id = :userId " +
            "AND e.saved_at < :beforeDate " +
            "ORDER BY e.saved_at DESC " +
            "LIMIT 1", nativeQuery = true)
    Optional<UserEquippedItem> findLeastBeforeDate(
            @Param("userId") Long userId,
            @Param("beforeDate") LocalDateTime beforeDate
    );


}
