package com.example.expense.service;

import com.example.expense.model.Budget;
import com.example.expense.model.User;
import com.example.expense.repository.BudgetRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;

    public BudgetService(BudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
    }

    public Budget getBudgetForMonth(User user, String monthYear) {
        return budgetRepository.findByUserAndMonthYear(user, monthYear).orElse(null);
    }

    public Budget saveBudget(User user, String monthYear, Double amount) {
        Optional<Budget> existing = budgetRepository.findByUserAndMonthYear(user, monthYear);
        if (existing.isPresent()) {
            Budget budget = existing.get();
            budget.setAmount(amount);
            return budgetRepository.save(budget);
        } else {
            Budget budget = new Budget();
            budget.setUser(user);
            budget.setMonthYear(monthYear);
            budget.setAmount(amount);
            return budgetRepository.save(budget);
        }
    }
}
