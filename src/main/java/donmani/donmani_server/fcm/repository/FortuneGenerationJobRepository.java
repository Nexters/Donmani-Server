package donmani.donmani_server.fcm.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import donmani.donmani_server.fcm.entity.FortuneGenerationJob;
import donmani.donmani_server.fcm.entity.FortuneGenerationJobStatus;

public interface FortuneGenerationJobRepository extends JpaRepository<FortuneGenerationJob, Long> {
	Optional<FortuneGenerationJob> findByTargetMonth(LocalDate targetMonth);

	Optional<FortuneGenerationJob> findFirstByStatusOrderByTargetMonthAsc(FortuneGenerationJobStatus status);

	Optional<FortuneGenerationJob> findFirstByApprovedAtIsNotNullAndStatusInOrderByTargetMonthAsc(
		Collection<FortuneGenerationJobStatus> statuses
	);
}
