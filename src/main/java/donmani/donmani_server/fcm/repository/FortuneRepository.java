package donmani.donmani_server.fcm.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import donmani.donmani_server.fcm.entity.Fortune;

public interface FortuneRepository extends JpaRepository<Fortune, Long> {
	Optional<Fortune> findByTargetDate(LocalDate date);
}