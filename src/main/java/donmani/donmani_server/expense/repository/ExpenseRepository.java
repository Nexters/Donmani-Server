package donmani.donmani_server.expense.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import donmani.donmani_server.expense.entity.Expense;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
	List<Expense> findByUserId(Long userId);

	@Query("SELECT e FROM Expense e WHERE e.userId = :userId AND YEAR(e.createdAt) = :year AND MONTH(e.createdAt) = :month")
	List<Expense> findByUserIdAndMonth(@Param("userId") Long userId, @Param("year") int year, @Param("month") int month);

	@Query("SELECT DISTINCT e.createdAt FROM Expense e WHERE e.userId = :userId ORDER BY e.createdAt DESC")
	Page<LocalDateTime> findDistinctCreatedAt(@Param("userId") Long userId, Pageable pageable);

	@Query("SELECT e FROM Expense e WHERE e.createdAt IN :localDateTimes ORDER BY e.createdAt DESC")
	List<Expense> findByCreatedAtIn(@Param("userId") Long userId, List<LocalDateTime> localDateTimes);

	// 소비가 GOOD, BAD 모두 존재하는 경우 랜덤으로 하나만 피드백 카드 생성
	@Query("SELECT e FROM Expense e WHERE e.userId = :userId AND DATE_FORMAT(e.createdAt, '%Y%m%d') = DATE_FORMAT(:date, '%Y%m%d') ORDER BY RAND() LIMIT 1")
	Expense findExpenseByUserIdAndAndCreatedAt(Long userId, LocalDateTime date);

	// @Query("SELECT DISTINCT e.createdAt FROM Expense e WHERE DATE_FORMAT(e.createdAt, '%Y%m%d') >= DATE_FORMAT('20250525', '%Y%m%d') AND e.userId = :userId")
	@Query("SELECT DISTINCT e.createdAt FROM Expense e WHERE e.userId = :userId")
	List<LocalDateTime> findTotalExpensesCount(Long userId);

	Expense findExpenseById(Long id);

}