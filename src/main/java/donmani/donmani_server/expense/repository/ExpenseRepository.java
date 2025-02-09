package donmani.donmani_server.expense.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import donmani.donmani_server.expense.entity.Expense;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
	List<Expense> findByUserId(Long userId);
	@Query("SELECT e FROM Expense e WHERE e.userId = :userId AND YEAR(e.createdAt) = :year AND MONTH(e.createdAt) = :month")
	List<Expense> findByUserIdAndMonth(@Param("userId") Long userId, @Param("year") int year, @Param("month") int month);

}