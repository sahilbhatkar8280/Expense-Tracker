package com.example.expense.controller;

import com.example.expense.model.Budget;
import com.example.expense.model.User;
import com.example.expense.repository.UserRepository;
import com.example.expense.service.BudgetService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;
    private final UserRepository userRepository;

    public BudgetController(BudgetService budgetService, UserRepository userRepository) {
        this.budgetService = budgetService;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser() {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByUsername(username);
    }

    @GetMapping("/{monthYear}")
    public ResponseEntity<Budget> getBudget(@PathVariable String monthYear) {
        User user = getAuthenticatedUser();
        Budget budget = budgetService.getBudgetForMonth(user, monthYear);
        if (budget == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(budget);
    }

    @PostMapping("/{monthYear}")
    public ResponseEntity<Budget> setBudget(@PathVariable String monthYear, @RequestBody Map<String, Double> payload) {
        User user = getAuthenticatedUser();
        Double amount = payload.get("amount");
        return ResponseEntity.ok(budgetService.saveBudget(user, monthYear, amount));
    }
}
