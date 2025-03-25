package donmani.donmani_server.fcm.reposiory;

import donmani.donmani_server.fcm.entity.FCMToken;
import donmani.donmani_server.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FCMTokenRepository extends JpaRepository<FCMToken, Long> {
    Optional<FCMToken> findByUser(User user);
}
