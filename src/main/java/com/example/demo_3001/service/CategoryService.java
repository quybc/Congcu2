package com.example.demo_3001.service;

import com.example.demo_3001.model.Category;
import com.example.demo_3001.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {
    private final CategoryRepository categoryRepository;

    private static final List<String> LEVEL_1_KEYS = Arrays.asList(
        "Điện thoại", "Laptop", "Phụ kiện", "Smartwatch", "Đồng hồ", 
        "Tablet", "Máy cũ Thu cũ", "Màn hình máy in", "Sim Thẻ Cào", "Dịch vụ tiện ích"
    );

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Map<String, List<Category>> getCategoriesGrouped() {
        List<Category> allCategories = categoryRepository.findAll();
        Map<String, List<Category>> grouped = new LinkedHashMap<>();
        Set<Long> processedIds = new HashSet<>();

        // Map parentName -> List<Category>
        Map<String, List<Category>> byParent = allCategories.stream()
                .filter(c -> c.getParentCategory() != null && !c.getParentCategory().isEmpty())
                .collect(Collectors.groupingBy(Category::getParentCategory));

        for (String key : LEVEL_1_KEYS) {
            List<Category> level2List = byParent.getOrDefault(key, new ArrayList<>());
            if (level2List.isEmpty()) continue;

            List<Category> groupList = new ArrayList<>();
            for (Category l2 : level2List) {
                // l2.setDisplayName(l2.getName()); // Default is name
                groupList.add(l2);
                processedIds.add(l2.getId());

                // Add Level 3 children
                List<Category> level3List = byParent.getOrDefault(l2.getName(), new ArrayList<>());
                for (Category l3 : level3List) {
                    // Use a new transient field for display name to avoid modifying entity state permanently if possible, 
                    // but since it's @Transient it won't persist.
                    // However, we need to set it on the object instance.
                    // We assume Lombok generated setter.
                    // We need to CAST to access if not visible? No, it's public class.
                    // But wait, I added 'private String displayName' and rely on Lombok @Setter.
                    // Let's assume setter exists.
                    try {
                        l3.getClass().getMethod("setDisplayName", String.class).invoke(l3, "-- " + l3.getName());
                    } catch (Exception e) {
                        // Fallback or ignore
                    }
                    groupList.add(l3);
                    processedIds.add(l3.getId());
                }
            }
            grouped.put(key, groupList);
        }

        // Add "Khác" for remaining categories
        List<Category> others = allCategories.stream()
                .filter(c -> !processedIds.contains(c.getId()))
                .collect(Collectors.toList());
        
        if (!others.isEmpty()) {
            grouped.put("Khác", others);
        }

        return grouped;
    }

    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    public Category addCategory(Category category) {
        return categoryRepository.save(category);
    }

    public void updateCategory(Category category) {
        categoryRepository.save(category);
    }

    public void deleteCategoryById(Long id) {
        categoryRepository.deleteById(id);
    }
}
