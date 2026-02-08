package donmani.donmani_server.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import donmani.donmani_server.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
	@Query("SELECT DISTINCT u " +
		   "FROM User u " +
		   "LEFT JOIN FCMToken f ON f.user = u " +
		   "WHERE u.userKey = :identifier OR f.token = :identifier")
	Optional<User> findByIdentifier(@Param("identifier") String identifier);
}