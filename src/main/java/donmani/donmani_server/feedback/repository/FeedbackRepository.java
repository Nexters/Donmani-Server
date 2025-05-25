package donmani.donmani_server.feedback.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import donmani.donmani_server.feedback.entity.Feedback;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
	@Query("SELECT f FROM Feedback f JOIN f.user u JOIN f.expense e WHERE u.id = :userId AND DATE_FORMAT(e.createdAt, '%Y%m%d') = DATE_FORMAT(:date, '%Y%m%d') AND f.isOpened IS FALSE")
	List<Feedback> findFeedbackByUserIdAndCreatedAt(Long userId, LocalDateTime date);

	@Query("SELECT f FROM Feedback f JOIN f.user u WHERE u.id = :userId ORDER BY f.createdDate DESC")
	List<Feedback> findFeedbackByUserIdAndCreatedDate(Long userId, LocalDateTime date);

	@Query("SELECT f FROM Feedback f JOIN f.user u WHERE u.id = :userId AND f.isOpened IS TRUE")
	List<Feedback> findFeedbackByUserIdIsFirstOpen(Long userId);

	@Query("SELECT f.title FROM Feedback f JOIN f.user u WHERE u.id = :userId")
	List<String> findFeedbackByUserIdUsedTitle(Long userId);
}