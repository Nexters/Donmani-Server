package donmani.donmani_server.reward.repository;

import donmani.donmani_server.reward.entity.UserItem;
import donmani.donmani_server.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserItemRepository extends JpaRepository<UserItem, Long> {
    @Query("SELECT ui FROM UserItem ui " +
            "WHERE ui.user = :user " +
            "AND ui.isOpened = true " +
            "ORDER BY ui.acquiredAt DESC")
    List<UserItem> findByUserOrderByAcquiredAtDesc(
            @Param("user") User user
    );

    @Query("SELECT ui FROM UserItem ui " +
            "WHERE ui.user = :user " +
            "AND ui.isOpened = false")
    List<UserItem> findByUserNotOpened(
            @Param("user") User user
    );

    @Query("SELECT ui FROM UserItem ui " +
            "WHERE ui.user = :user ")
    List<UserItem> findAllByUser(
            @Param("user") User user
    );

    @Query("SELECT ui FROM UserItem ui " +
            "JOIN ui.item i " +
            "WHERE ui.user = :user " +
            "AND ui.isOpened = false " +
            "AND i.isHidden = true")
    Optional<UserItem> findOneUnopenedHiddenItem(@Param("user") User user);
}
