package donmani.donmani_server.reward.repository;

import donmani.donmani_server.reward.entity.UserItem;
import donmani.donmani_server.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserItemRepository extends JpaRepository<UserItem, Long> {

    @Query("SELECT ui FROM UserItem ui " +
            "WHERE ui.user = :user " +
            "AND ui.acquiredAt BETWEEN :start AND :end")
    List<UserItem> findByUserAndAcquiredAtBetween(
            @Param("user") User user,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT ui FROM UserItem ui " +
            "WHERE ui.user = :user " +
            "AND ui.acquiredAt BETWEEN :start AND :end " +
            "AND ui.isOpened = false")
    List<UserItem> findByUserAndAcquiredAtBetweenAndNotOpened(
            @Param("user") User user,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

}
