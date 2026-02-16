package donmani.donmani_server.fcm.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import donmani.donmani_server.fcm.entity.Fortune;

public interface FortuneRepository extends JpaRepository<Fortune, Long> {
	@Query(value = "\n"
		+ "SELECT *\n"
		+ "FROM fortune a\n"
		+ "WHERE 1=1\n"
		+ "AND a.id NOT IN (\n"
		+ "  SELECT a.fortune_id\n"
		+ "  FROM fortune_histories a\n"
		+ "    LEFT JOIN fcm_logs b ON a.user_id = b.user_id\n"
		+ "    AND DATE_FORMAT(b.created_at , '%Y%m%d') = :localDate\n"
		+ "    AND b.notification_type = 'FORTUNE'\n"
		+ "    AND b.status            = 'SUCCESS'\n"
		+ "  WHERE 1=1\n"
		+ "  AND DATE_FORMAT(a.created_at , '%Y%m%d') = :localDate\n"
		+ "  AND a.user_id = :userId\n"
		+ ") \n"
		+ "AND a.target_date = :localDate\n", nativeQuery = true)
	Optional<Fortune> findFortuneByUserIdAndTargetDate(Long userId, LocalDate localDate);
}