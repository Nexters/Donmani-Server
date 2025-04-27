package donmani.donmani_server.fcm.reposiory;

import donmani.donmani_server.expense.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PushExpenseRepository extends JpaRepository<Expense, Long> {
    // 오늘 소비 기록이 없는 유저의 FCM 토큰 조회
    @Query("SELECT f.token FROM FCMToken f WHERE NOT EXISTS " +
            "(SELECT e FROM Expense e WHERE e.userId = f.user.id AND DATE(e.createdAt) = CURRENT_DATE)")
    List<String> findTokensWithoutExpenseToday();

    // 어제 소비 기록이 없는 유저의 FCM 토큰 조회
    @Query("SELECT f.token FROM FCMToken f WHERE NOT EXISTS " +
            "(SELECT e FROM Expense e WHERE e.userId = f.user.id " +
            "AND e.createdAt >= :yesterday)")
    List<String> findTokensWithoutExpenseSince(@Param("yesterday") LocalDateTime yesterday);

}
