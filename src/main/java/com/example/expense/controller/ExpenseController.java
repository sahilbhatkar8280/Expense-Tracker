package com.example.expense.controller;

import com.example.expense.model.Expense;
import com.example.expense.model.User;
import com.example.expense.repository.UserRepository;
import com.example.expense.service.ExpenseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final UserRepository userRepository;

    public ExpenseController(ExpenseService expenseService, UserRepository userRepository) {
        this.expenseService = expenseService;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser() {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByUsername(username);
    }

    private void handleBillUpload(Expense expense, MultipartFile billFile) {
        if (billFile != null && !billFile.isEmpty()) {
            try {
                String uploadsDir = System.getProperty("user.dir") + "/uploads/";
                File dir = new File(uploadsDir);
                if (!dir.exists()) dir.mkdirs();
                
                String filename = UUID.randomUUID().toString() + "_" + billFile.getOriginalFilename().replaceAll("[^a-zA-Z0-9.-]", "_");
                Path filePath = Paths.get(uploadsDir, filename);
                Files.copy(billFile.getInputStream(), filePath);
                
                expense.setBillPath(filename);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to store bill file");
            }
        }
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<Expense> addExpense(@ModelAttribute Expense expense, 
                                              @RequestParam(value="billFile", required=false) MultipartFile billFile,
                                              @RequestParam(value="currencyCode", defaultValue="INR") String currencyCode) {
        User user = getAuthenticatedUser();
        expense.setUser(user);
        handleBillUpload(expense, billFile);
        return ResponseEntity.ok(expenseService.addExpense(expense, currencyCode));
    }

    @GetMapping
    public ResponseEntity<List<Expense>> getExpenses() {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(expenseService.getUserExpenses(user));
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<Expense> updateExpense(@PathVariable Long id, @ModelAttribute Expense expense, @RequestParam(value="billFile", required=false) MultipartFile billFile) {
        User user = getAuthenticatedUser();
        handleBillUpload(expense, billFile);
        return ResponseEntity.ok(expenseService.updateExpense(id, expense, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteExpense(@PathVariable Long id) {
        User user = getAuthenticatedUser();
        expenseService.deleteExpense(id, user);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<Expense>> search(@RequestParam String keyword) {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(expenseService.searchExpenses(user, keyword));
    }
}
