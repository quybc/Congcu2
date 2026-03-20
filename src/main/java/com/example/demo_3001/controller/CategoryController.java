package com.example.demo_3001.controller;

import com.example.demo_3001.model.Category;
import com.example.demo_3001.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class CategoryController {
    
    private final CategoryService categoryService;

    @GetMapping("/categories/add")
    public String showAddForm(Model model) {
        model.addAttribute("category", new Category());
        model.addAttribute("allCategories", categoryService.getAllCategories());
        return "/categories/add-category";
    }

    @PostMapping("/categories/add")
    public String addCategory(@Valid Category category, BindingResult result,
                              @RequestParam("imageCategory") MultipartFile imageCategory) {
        if (result.hasErrors()) {
            return "/categories/add-category";
        }
        saveImage(imageCategory, category);
        categoryService.addCategory(category);
        return "redirect:/categories";
    }

    @GetMapping("/categories")
    public String listCategories(Model model) {
        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("categories", categories);
        return "/categories/categories-list";
    }
    
    @GetMapping("/categories/edit/{id}")
    public String showUpdateForm(@PathVariable("id") Long id, Model model) {
        Category category = categoryService.getCategoryById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid category Id:" + id));
        model.addAttribute("category", category);
        model.addAttribute("allCategories", categoryService.getAllCategories());
        return "/categories/update-category";
    }

    @PostMapping("/categories/update/{id}")
    public String updateCategory(@PathVariable("id") Long id, @Valid Category category,
                                 BindingResult result, Model model,
                                 @RequestParam("imageCategory") MultipartFile imageCategory) {
        if (result.hasErrors()) {
            category.setId(id);
            return "/categories/update-category";
        }
        // Nếu không upload ảnh mới, giữ ảnh cũ
        if (imageCategory.isEmpty()) {
            Category existing = categoryService.getCategoryById(id).orElse(null);
            if (existing != null) category.setImage(existing.getImage());
        } else {
            saveImage(imageCategory, category);
        }
        categoryService.updateCategory(category);
        return "redirect:/categories";
    }

    /** Lưu file ảnh vào thư mục static/images và target/classes/static/images */
    private void saveImage(MultipartFile file, Category category) {
        if (file == null || file.isEmpty()) return;
        try {
            String fileName = file.getOriginalFilename();
            Path srcPath = Paths.get("src/main/resources/static/images/" + fileName);
            Files.createDirectories(srcPath.getParent());
            Files.write(srcPath, file.getBytes());
            Path targetPath = Paths.get("target/classes/static/images/" + fileName);
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, file.getBytes());
            category.setImage(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable("id") Long id, Model model) {
        Category category = categoryService.getCategoryById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid category Id:" + id));
        categoryService.deleteCategoryById(id);
        return "redirect:/categories";
    }
}
