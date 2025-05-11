package donmani.donmani_server.webhook.repository;

import donmani.donmani_server.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;

public interface WebHookRepository extends JpaRepository<User, Long> {
	@Query("SELECT COUNT(*) FROM User u WHERE DATE_FORMAT(u.createdDate, '%Y%m%d') = DATE_FORMAT(:date, '%Y%m%d')")
	Integer countNewUsersOnDate(LocalDateTime date);

	@Query("SELECT COUNT(*) FROM User u WHERE DATE_FORMAT(u.createdDate, '%Y%m%d') <= DATE_FORMAT(:date, '%Y%m%d') or u.createdDate is null")
	Integer countAllUsersBefore(LocalDateTime date);

	@Query("SELECT COUNT(*) FROM User u WHERE DATE_FORMAT(u.lastLoginDate, '%Y%m%d') = DATE_FORMAT(:date, '%Y%m%d')")
	Integer countLoginUsersOnDate(LocalDateTime date);

	@Query(value =
		  "SELECT COUNT(*)\n"
		+ "FROM (\n"
		+ "      SELECT COUNT(*)\n"
		+ "      FROM Expense e\n"
		+ "      WHERE DATE_FORMAT(e.created_date, '%Y%m%d') = DATE_FORMAT(:date, '%Y%m%d')\n"
		+ "      GROUP BY e.user_id\n"
		+ "      ) a"
		, nativeQuery = true)
	Integer countExpenseSubmittersOnDate(LocalDateTime date);

	// @Query("SELECT COUNT(e) FROM User e WHERE e.isNoticeEnable = TRUE")
	@Query("SELECT COUNT(*) FROM FCMToken f")
	Integer countByNoticeEnableTrueUser();

	@Query(value =
		  "SELECT COUNT(*)\n"
		+ "FROM (\n"
		+ "      SELECT COUNT(*)\n"
		+ "      FROM (\n"
		+ "            SELECT e.user_id, e.created_date\n"
		+ "            FROM Expense e\n"
		+ "            WHERE DATE_FORMAT(e.created_date, '%Y%m%d') BETWEEN curdate() - (:day - 1) AND CURDATE()\n"
		+ "            GROUP BY e.user_id, e.created_date\n"
		+ "            ) a\n"
		+ "      GROUP BY a.user_id\n"
		+ "      HAVING COUNT(*) = :day\n"
		+ "      ) b"
		, nativeQuery = true)
	Integer countUsersWithStreak(int day);
}