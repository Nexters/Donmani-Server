package donmani.donmani_server.fcm.repository;

import donmani.donmani_server.expense.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PushExpenseRepository extends JpaRepository<Expense, Long> {
    // 오늘 소비 기록이 없는 유저의 FCM 토큰 조회
    @Query("SELECT f.token FROM FCMToken f WHERE NOT EXISTS " +
            "(SELECT e FROM Expense e WHERE e.userId = f.user.id AND DATE(e.createdAt) = CURRENT_DATE)")
    List<String> findTokensWithoutExpenseToday();

    // 특정 날짜 소비 기록이 없는 유저의 FCM 토큰 조회
    @Query("SELECT f.token FROM FCMToken f WHERE NOT EXISTS " +
            "(SELECT e FROM Expense e WHERE e.userId = f.user.id " +
            "AND DATE(e.createdAt) = :expenseDate)")
    List<String> findTokensWithoutExpenseOn(@Param("expenseDate") LocalDate expenseDate);

    @Query("SELECT f.token FROM FCMToken f WHERE EXISTS " +
            "(SELECT fh FROM FortuneHistory fh WHERE fh.user = f.user " +
            "AND fh.fortune.targetDate = :fortuneDate " +
            "AND fh.readAt IS NOT NULL)")
    List<String> findTokensReadFortuneOn(
            @Param("fortuneDate") LocalDate fortuneDate
    );

    @Query("SELECT f.token FROM FCMToken f WHERE NOT EXISTS " +
            "(SELECT e FROM Expense e WHERE e.userId = f.user.id " +
            "AND DATE(e.createdAt) = :expenseDate) " +
            "AND NOT EXISTS " +
            "(SELECT fh FROM FortuneHistory fh WHERE fh.user = f.user " +
            "AND fh.fortune.targetDate = :fortuneDate " +
            "AND fh.readAt IS NOT NULL)")
    List<String> findTokensWithoutExpenseOnAndUnreadFortune(
            @Param("expenseDate") LocalDate expenseDate,
            @Param("fortuneDate") LocalDate fortuneDate
    );

    @Query("SELECT COUNT(e) > 0 FROM Expense e WHERE e.userId = :userId AND DATE(e.createdAt) = :expenseDate")
    boolean existsExpenseOn(
            @Param("userId") Long userId,
            @Param("expenseDate") LocalDate expenseDate
    );

}
