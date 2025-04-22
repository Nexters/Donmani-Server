package donmani.donmani_server.webhook.repository;

import donmani.donmani_server.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;

public interface WebHookRepository extends JpaRepository<User, Long> {
	@Query("SELECT COUNT(u) FROM User u WHERE u.createdDate BETWEEN :start AND :end")
	long countNewUsersOnDate(LocalDateTime start, LocalDateTime end);

	@Query("SELECT COUNT(u) FROM User u WHERE u.createdDate <= :end or u.createdDate is null")
	long countAllUsersBefore(LocalDateTime end);

	@Query("SELECT COUNT(u) FROM User u WHERE u.lastLoginDate BETWEEN :start AND :end")
	long countLoginUsersOnDate(LocalDateTime start, LocalDateTime end);

	@Query("SELECT COUNT(e) FROM Expense e WHERE e.createdDate BETWEEN :start AND :end group by e.userId")
	long countExpenseSubmittersOnDate(LocalDateTime start, LocalDateTime end);

	// @Query("SELECT COUNT(e) FROM User e WHERE e.isNoticeEnable = TRUE")
	@Query("SELECT COUNT(f) FROM FCMToken f")
	long countByNoticeEnableTrueUser();

	@Query(value =
		      "SELECT COUNT(*)\n"
			+ "FROM (\n"
			+ "  SELECT user_id\n"
			+ "  FROM (\n"
			+ "    SELECT \n"
			+ "      user_id,\n"
			+ "      DATE(created_date) AS log_date,\n"
			+ "      ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY DATE(created_date)) \n"
			+ "        - DENSE_RANK() OVER (PARTITION BY user_id ORDER BY DATE(created_date)) AS grp\n"
			+ "    FROM expense\n"
			+ "    GROUP BY user_id, DATE(created_date)\n"
			+ "  ) a\n"
			+ "  GROUP BY user_id, grp\n"
			+ "  HAVING COUNT(*) >= :day) b\n"
			+ "GROUP BY user_id",
		   nativeQuery = true)
	long countUsersWithStreak(int day);
}