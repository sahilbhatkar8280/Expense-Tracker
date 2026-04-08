package com.example.expense.service;

import com.example.expense.model.Budget;
import com.example.expense.model.Expense;
import com.example.expense.model.User;
import com.example.expense.repository.BudgetRepository;
import com.example.expense.repository.ExpenseRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class InsightsService {

    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;

    public InsightsService(ExpenseRepository expenseRepository, BudgetRepository budgetRepository) {
        this.expenseRepository = expenseRepository;
        this.budgetRepository = budgetRepository;
    }

    public List<String> generateInsights(User user) {
        List<String> insights = new ArrayList<>();
        
        YearMonth currentMonth = YearMonth.now();
        String monthStr = String.format("%02d-%d", currentMonth.getMonthValue(), currentMonth.getYear());
        LocalDateTime start = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime end = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        List<Expense> monthlyExpenses = expenseRepository.findByUserAndDateTimeBetweenOrderByDateTimeDesc(user, start, end);
        double totalExpense = monthlyExpenses.stream().filter(e -> "EXPENSE".equals(e.getType())).mapToDouble(Expense::getAmount).sum();
        double totalIncome = monthlyExpenses.stream().filter(e -> "INCOME".equals(e.getType())).mapToDouble(Expense::getAmount).sum();

        Optional<Budget> budgetOpt = budgetRepository.findByUserAndMonthYear(user, monthStr);
        if (budgetOpt.isPresent() && budgetOpt.get().getAmount() > 0) {
            double budgetAmount = budgetOpt.get().getAmount();
            double percent = (totalExpense / budgetAmount) * 100;
            if (percent >= 90) {
                insights.add("CRITICAL: You have consumed " + String.format("%.0f", percent) + "% of your monthly budget!");
            } else if (percent >= 70) {
                insights.add("WARNING: Approaching budget limits (" + String.format("%.0f", percent) + "% used).");
            } else {
                insights.add("On track! You have only used " + String.format("%.0f", percent) + "% of your monthly budget.");
            }
        } else {
            insights.add("Tip: Setting a monthly budget helps you control spending.");
        }

        if (totalIncome > 0) {
            double savings = totalIncome - totalExpense;
            if (savings > 0) {
                insights.add("Great job! You have a net positive savings of ₹" + savings + " this month.");
            } else {
                insights.add("Careful! Your expenses have exceeded your income this month.");
            }
        }

        List<Object[]> cats = expenseRepository.getCategoryWiseExpense(user);
        if (!cats.isEmpty()) {
            double maxCatVal = 0;
            String maxCatName = "";
            for(Object[] row : cats) {
                double val = (Double) row[1];
                if(val > maxCatVal) { maxCatVal = val; maxCatName = (String) row[0]; }
            }
            if(maxCatVal > 0) {
                insights.add("Your highest spending category is " + maxCatName + " (₹" + maxCatVal + ").");
            }
        }

        if(insights.isEmpty()) {
            insights.add("Welcome! Start logging transactions to unlock AI Insights.");
        }
        
        return insights;
    }
}
