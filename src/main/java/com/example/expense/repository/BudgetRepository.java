package com.example.expense.repository;

import com.example.expense.model.Budget;
import com.example.expense.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    Optional<Budget> findByUserAndMonthYear(User user, String monthYear);
}
