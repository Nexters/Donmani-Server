package donmani.donmani_server.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import donmani.donmani_server.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByUserKey(String userKey);
}