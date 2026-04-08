package com.example.expense.service;

import com.example.expense.model.Category;
import com.example.expense.model.User;
import com.example.expense.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> getUserCategories(User user) {
        List<Category> categories = categoryRepository.findByUser(user);
        if (categories.isEmpty()) {
            return initializeDefaultCategories(user);
        }
        return categories;
    }

    public List<Category> initializeDefaultCategories(User user) {
        List<String> defaults = Arrays.asList("Food", "Rent", "Transportation", "Utilities", "Entertainment", "Salary", "Other");
        defaults.forEach(name -> {
            Category cat = new Category();
            cat.setName(name);
            cat.setUser(user);
            categoryRepository.save(cat);
        });
        return categoryRepository.findByUser(user);
    }

    public Category addCategory(User user, String name) {
        Category cat = new Category();
        cat.setName(name);
        cat.setUser(user);
        return categoryRepository.save(cat);
    }

    public void deleteCategory(User user, Long id) {
        categoryRepository.findById(id).ifPresent(cat -> {
            if (cat.getUser().getId().equals(user.getId())) {
                categoryRepository.delete(cat);
            }
        });
    }
}
