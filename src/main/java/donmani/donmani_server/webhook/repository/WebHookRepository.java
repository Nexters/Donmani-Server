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
		+ "      SELECT e.user_id\n"
		+ "      FROM expense e\n"
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
		+ "      SELECT a.user_id\n"
		+ "      FROM (\n"
		+ "            SELECT e.user_id, DATE_FORMAT(e.created_date, '%Y%m%d')\n"
		+ "            FROM expense e\n"
		+ "            WHERE DATE_FORMAT(e.created_date, '%Y%m%d') BETWEEN DATE_FORMAT(DATE_SUB(:date, INTERVAL :day DAY), '%Y%m%d') AND DATE_FORMAT(:date, '%Y%m%d')\n"
		+ "            AND e.created_date IS NOT NULL\n"
		+ "            GROUP BY e.user_id, DATE_FORMAT(e.created_date, '%Y%m%d')\n"
		+ "            ) a\n"
		+ "      GROUP BY a.user_id\n"
		+ "      HAVING COUNT(*) >= :day + 1\n"
		+ "      ) b"
		, nativeQuery = true)
	Integer countUsersWithStreak(LocalDateTime date, int day);
}