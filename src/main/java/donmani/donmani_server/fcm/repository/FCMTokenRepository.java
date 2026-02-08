package donmani.donmani_server.fcm.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import donmani.donmani_server.fcm.entity.FCMToken;
import donmani.donmani_server.user.entity.User;

@Repository
public interface FCMTokenRepository extends JpaRepository<FCMToken, Long> {
	Optional<FCMToken> findByUser(User user);
}
