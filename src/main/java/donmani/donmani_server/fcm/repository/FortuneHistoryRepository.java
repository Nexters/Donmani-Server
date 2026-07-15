package donmani.donmani_server.fcm.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import donmani.donmani_server.fcm.entity.FortuneHistory;

public interface FortuneHistoryRepository extends JpaRepository<FortuneHistory, Long> {
	@Query("SELECT fh " +
		"FROM FortuneHistory fh " +
		"WHERE fh.user.userKey = :userKey " +
		"AND fh.fortune.targetDate = :targetDate ")
	Optional<FortuneHistory> findFortuneByTargetDate(
		@Param("userKey") String userKey,
		@Param("targetDate") LocalDate targetDate
	);

	@Query("SELECT fh " +
		"FROM FortuneHistory fh " +
		"JOIN FETCH fh.fortune f " +
		"WHERE fh.user.userKey = :userKey " +
		"AND f.targetDate BETWEEN :startDate AND :endDate " +
		"AND fh.readAt IS NOT NULL " +
		"ORDER BY f.targetDate ASC ")
	List<FortuneHistory> findReadFortunesByTargetDateBetween(
		@Param("userKey") String userKey,
		@Param("startDate") LocalDate startDate,
		@Param("endDate") LocalDate endDate
	);
}
