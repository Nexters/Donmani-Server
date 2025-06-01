package donmani.donmani_server.reward.repository;

import donmani.donmani_server.reward.entity.UserEquippedItem;
import donmani.donmani_server.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserEquippedItemRepository extends JpaRepository<UserEquippedItem, Long> {
    @Query("SELECT e FROM UserEquippedItem e " +
            "WHERE e.user = :user " +
            "AND FUNCTION('YEAR', e.savedAt) = :year " +
            "AND FUNCTION('MONTH', e.savedAt) = :month")
    Optional<UserEquippedItem> findByUserAndSavedAtInCurrentMonth(
            @Param("user") User user,
            @Param("year") int year,
            @Param("month") int month
    );

}
