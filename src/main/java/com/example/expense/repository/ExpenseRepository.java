package com.example.expense.repository;

import com.example.expense.model.Expense;
import com.example.expense.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    
    List<Expense> findByUserOrderByDateTimeDesc(User user);
    
    List<Expense> findByUserAndIsRecurringTrue(User user);
    
    List<Expense> findByUserAndDateTimeBetweenOrderByDateTimeDesc(User user, LocalDateTime start, LocalDateTime end);
    
    List<Expense> findByUserAndCategoryOrderByDateTimeDesc(User user, String category);
    
    @Query("SELECT e FROM Expense e WHERE e.user = :user AND LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY e.dateTime DESC")
    List<Expense> searchByKeyword(@Param("user") User user, @Param("keyword") String keyword);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user = :user AND e.type = 'INCOME'")
    Double getTotalIncome(@Param("user") User user);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user = :user AND e.type = 'EXPENSE'")
    Double getTotalExpense(@Param("user") User user);
    
    @Query("SELECT e.category, SUM(e.amount) FROM Expense e WHERE e.user = :user AND e.type = 'EXPENSE' GROUP BY e.category")
    List<Object[]> getCategoryWiseExpense(@Param("user") User user);
}
