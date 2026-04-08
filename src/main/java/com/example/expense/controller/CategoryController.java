package com.example.expense.controller;

import com.example.expense.model.Category;
import com.example.expense.model.User;
import com.example.expense.repository.UserRepository;
import com.example.expense.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final UserRepository userRepository;

    public CategoryController(CategoryService categoryService, UserRepository userRepository) {
        this.categoryService = categoryService;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser() {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByUsername(username);
    }

    @GetMapping
    public ResponseEntity<List<Category>> getCategories() {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(categoryService.getUserCategories(user));
    }

    @PostMapping
    public ResponseEntity<Category> addCategory(@RequestBody Map<String, String> payload) {
        User user = getAuthenticatedUser();
        String name = payload.get("name");
        return ResponseEntity.ok(categoryService.addCategory(user, name));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        User user = getAuthenticatedUser();
        categoryService.deleteCategory(user, id);
        return ResponseEntity.ok().build();
    }
}
