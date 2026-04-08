package com.example.expense.service;

import com.example.expense.model.Expense;
import com.example.expense.model.User;
import com.example.expense.repository.ExpenseRepository;
import com.example.expense.repository.BudgetRepository;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ExpenseService {

    private final ExpenseRepository repository;
    private final BudgetRepository budgetRepository;

    public ExpenseService(ExpenseRepository repository, BudgetRepository budgetRepository) {
        this.repository = repository;
        this.budgetRepository = budgetRepository;
    }

    public Expense addExpense(Expense expense, String currencyCode) {
        if (currencyCode != null && !"INR".equalsIgnoreCase(currencyCode)) {
            try {
                RestTemplate restTemplate = new RestTemplate();
                String url = "https://open.er-api.com/v6/latest/" + currencyCode;
                Map<String, Object> response = restTemplate.getForObject(url, Map.class);
                if (response != null && response.containsKey("rates")) {
                    Map<String, Object> rates = (Map<String, Object>) response.get("rates");
                    if (rates.containsKey("INR")) {
                        double rate = ((Number) rates.get("INR")).doubleValue();
                        expense.setAmount(expense.getAmount() * rate);
                        expense.setDescription((expense.getDescription() != null ? expense.getDescription() : "") + " [" + currencyCode + " Converted]");
                    }
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        if (expense.getDateTime() == null) {
            expense.setDateTime(LocalDateTime.now());
        }
        return repository.save(expense);
    }

    public Expense updateExpense(Long id, Expense updatedExpense, User user) {
        Optional<Expense> optional = repository.findById(id);
        if (optional.isPresent()) {
            Expense existing = optional.get();
            if(!existing.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("Unauthorized");
            }
            existing.setAmount(updatedExpense.getAmount());
            existing.setCategory(updatedExpense.getCategory());
            existing.setType(updatedExpense.getType());
            existing.setDescription(updatedExpense.getDescription());
            if(updatedExpense.getDateTime() != null) {
                existing.setDateTime(updatedExpense.getDateTime());
            }
            if(updatedExpense.getBillPath() != null) {
                existing.setBillPath(updatedExpense.getBillPath());
            }
            return repository.save(existing);
        }
        throw new RuntimeException("Expense not found");
    }

    public void deleteExpense(Long id, User user) {
        Optional<Expense> optional = repository.findById(id);
        if(optional.isPresent()){
            Expense existing = optional.get();
            if(!existing.getUser().getId().equals(user.getId())){
                throw new RuntimeException("Unauthorized");
            }
            repository.deleteById(id);
        }
    }

    public List<Expense> getUserExpenses(User user) {
        return repository.findByUserOrderByDateTimeDesc(user);
    }

    public List<Expense> getExpensesBetween(User user, LocalDateTime start, LocalDateTime end) {
        return repository.findByUserAndDateTimeBetweenOrderByDateTimeDesc(user, start, end);
    }
    
    public List<Expense> searchExpenses(User user, String keyword) {
        return repository.searchByKeyword(user, keyword);
    }

    public Map<String, Object> getDashboardData(User user) {
        Double totalIncome = repository.getTotalIncome(user);
        Double totalExpense = repository.getTotalExpense(user);
        
        if(totalIncome == null) totalIncome = 0.0;
        if(totalExpense == null) totalExpense = 0.0;
        
        Map<String, Object> data = new HashMap<>();
        data.put("totalIncome", totalIncome);
        data.put("totalExpense", totalExpense);
        data.put("balance", totalIncome - totalExpense);
        data.put("recentTransactions", repository.findByUserOrderByDateTimeDesc(user).stream().limit(5).toList());
        
        // Month tracking
        YearMonth currentMonth = YearMonth.now();
        String monthYear = String.format("%02d-%d", currentMonth.getMonthValue(), currentMonth.getYear());
        LocalDateTime start = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime end = currentMonth.atEndOfMonth().atTime(23, 59, 59, 999999999);
        
        List<Expense> monthlyExpenses = repository.findByUserAndDateTimeBetweenOrderByDateTimeDesc(user, start, end);
        double monthlyExpenseTotal = monthlyExpenses.stream().filter(e -> "EXPENSE".equals(e.getType())).mapToDouble(Expense::getAmount).sum();
        
        data.put("monthlyExpense", monthlyExpenseTotal);
        
        budgetRepository.findByUserAndMonthYear(user, monthYear).ifPresentOrElse(
            b -> data.put("monthlyBudget", b.getAmount()),
            () -> data.put("monthlyBudget", 0.0) // 0 implies none set
        );
        
        data.put("currentMonthStr", monthYear);

        return data;
    }
    
    public List<Map<String, Object>> getCategoryDistribution(User user) {
        List<Object[]> rawList = repository.getCategoryWiseExpense(user);
        return rawList.stream().map(objects -> {
            Map<String, Object> map = new HashMap<>();
            map.put("category", objects[0]);
            map.put("total", objects[1]);
            return map;
        }).toList();
    }

    public Map<String, Object> getHistoricalData(User user) {
        Map<String, Object> response = new HashMap<>();
        List<String> labels = new java.util.ArrayList<>();
        List<Double> incomes = new java.util.ArrayList<>();
        List<Double> expenses = new java.util.ArrayList<>();
        
        YearMonth current = YearMonth.now().minusMonths(5);
        for (int i = 0; i < 6; i++) {
            LocalDateTime start = current.atDay(1).atStartOfDay();
            LocalDateTime end = current.atEndOfMonth().atTime(23, 59, 59, 999999999);
            
            List<Expense> monthTxs = repository.findByUserAndDateTimeBetweenOrderByDateTimeDesc(user, start, end);
            double inc = monthTxs.stream().filter(e -> "INCOME".equals(e.getType())).mapToDouble(Expense::getAmount).sum();
            double exp = monthTxs.stream().filter(e -> "EXPENSE".equals(e.getType())).mapToDouble(Expense::getAmount).sum();
            
            labels.add(current.getMonth().toString().substring(0, 3) + " " + current.getYear());
            incomes.add(inc);
            expenses.add(exp);
            
            current = current.plusMonths(1);
        }
        
        response.put("labels", labels);
        response.put("incomes", incomes);
        response.put("expenses", expenses);
        
        return response;
    }

    public List<Map<String, Object>> getUpcomingSubscriptions(User user) {
        List<Expense> recurrings = repository.findByUserAndIsRecurringTrue(user);
        List<Map<String, Object>> subs = new java.util.ArrayList<>();
        
        Map<String, Expense> uniqueSubs = new HashMap<>();
        for(Expense e : recurrings) {
            String identifier = (e.getDescription() != null && !e.getDescription().isEmpty()) ? e.getDescription() : e.getCategory();
            if(!uniqueSubs.containsKey(identifier) || e.getDateTime().isAfter(uniqueSubs.get(identifier).getDateTime())) {
                uniqueSubs.put(identifier, e);
            }
        }
        
        for (Expense e : uniqueSubs.values()) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", (e.getDescription() != null && !e.getDescription().isEmpty()) ? e.getDescription() : e.getCategory());
            map.put("amount", e.getAmount());
            
            LocalDateTime nextCharge = e.getDateTime();
            while(nextCharge.isBefore(LocalDateTime.now())) {
                nextCharge = nextCharge.plusMonths(1);
            }
            map.put("nextCharge", nextCharge.toLocalDate().toString());
            subs.add(map);
        }
        
        subs.sort((a,b) -> ((String)a.get("nextCharge")).compareTo((String)b.get("nextCharge")));
        return subs;
    }
}
