package donmani.donmani_server.fcm.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import donmani.donmani_server.fcm.entity.FCMToken;
import donmani.donmani_server.user.entity.User;

@Repository
public interface FCMTokenRepository extends JpaRepository<FCMToken, Long> {
	Optional<FCMToken> findByUser(User user);

	Optional<FCMToken> findByToken(String token);

	@Query(value = "\n"
		+ "SELECT token\n"
		+ "FROM fcmtoken\n"
		+ "WHERE 1=1\n"
		+ "AND token NOT IN (\n"
		+ "  SELECT a.token\n"
		+ "  FROM fcmtoken a\n"
		+ "  LEFT JOIN fortune_histories b ON a.user_id = b.user_id\n"
		+ "    AND DATE_FORMAT(b.created_at, '%Y%m%d') = :localDate\n"
		+ "  LEFT JOIN fcm_logs d ON a.user_id = d.user_id\n"
		+ "    AND DATE_FORMAT(d.created_at, '%Y%m%d') = :localDate\n"
		+ "    AND d.notification_type = 'FORTUNE'\n"
		+ "    AND d.status            = 'SUCCESS'\n"
		+ "  JOIN fortune c ON b.fortune_id = c.id\n"
		+ "    AND c.target_date = :localDate\n"
		+ ")\n", nativeQuery = true)
	List<String> findAllTokensToSendFortune(LocalDate localDate);

	@Query(value = "\n"
		+ "SELECT a.token\n"
		+ "FROM fcmtoken a\n"
		+ "JOIN fortune_histories b ON a.user_id = b.user_id\n"
		+ "JOIN fortune c ON b.fortune_id = c.id\n"
		+ "WHERE c.target_date = :localDate\n"
		+ "AND b.read_at IS NULL\n", nativeQuery = true)
	List<String> findAllTokensToResendUnreadFortune(LocalDate localDate);
}
