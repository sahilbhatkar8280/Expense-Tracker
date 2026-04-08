package com.example.expense.controller;

import com.example.expense.model.User;
import com.example.expense.repository.UserRepository;
import com.example.expense.service.ExpenseService;
import com.example.expense.service.InsightsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final ExpenseService expenseService;
    private final InsightsService insightsService;
    private final UserRepository userRepository;

    public DashboardController(ExpenseService expenseService, InsightsService insightsService, UserRepository userRepository) {
        this.expenseService = expenseService;
        this.insightsService = insightsService;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser() {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByUsername(username);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getDashboardData() {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(expenseService.getDashboardData(user));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Map<String, Object>>> getCategoryDistribution() {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(expenseService.getCategoryDistribution(user));
    }

    @GetMapping("/historical")
    public ResponseEntity<Map<String, Object>> getHistoricalData() {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(expenseService.getHistoricalData(user));
    }

    @GetMapping("/insights")
    public ResponseEntity<List<String>> getInsights() {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(insightsService.generateInsights(user));
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<List<Map<String, Object>>> getSubscriptions() {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(expenseService.getUpcomingSubscriptions(user));
    }
}
