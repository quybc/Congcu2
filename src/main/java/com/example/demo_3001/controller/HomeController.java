package com.example.demo_3001.controller;

import com.example.demo_3001.model.Category;
import com.example.demo_3001.model.Product;
import com.example.demo_3001.service.CartService;
import com.example.demo_3001.service.CategoryService;
import com.example.demo_3001.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final CartService cartService;

    @GetMapping("/")
    public String home(Model model) {
        // Add cart count
        model.addAttribute("cartCount", cartService.getCartCount());

        // Flash Sale: chỉ lấy sản phẩm Khuyến Mãi
        List<Product> flashSaleProducts = productService.getProductsByType("KHUYEN_MAI")
                .stream().limit(12).collect(Collectors.toList());

        // Gợi ý: chỉ lấy sản phẩm bình thường (NONE)
        List<Product> suggestedProducts = productService.getProductsByType("NONE")
                .stream().limit(12).collect(Collectors.toList());

        // Quà tặng
        List<Product> giftProducts = productService.getProductsByType("QUA_TANG")
                .stream().limit(12).collect(Collectors.toList());

        model.addAttribute("flashSaleProducts", flashSaleProducts);
        model.addAttribute("suggestedProducts", suggestedProducts);
        model.addAttribute("giftProducts", giftProducts);

        List<Category> allCategories = categoryService.getAllCategories();
        model.addAttribute("categories", allCategories);

        // Các danh mục lớp 1 (gốc cố định)
        List<String> level1Names = Arrays.asList(
            "Điện thoại","Laptop","Phụ kiện","Smartwatch","Đồng hồ",
            "Tablet","Máy cũ Thu cũ","Màn hình máy in","Sim Thẻ Cào","Dịch vụ tiện ích"
        );

        // Lớp 2: categories có parentCategory = một trong 10 gốc
        Map<String, List<Category>> level2Map = allCategories.stream()
            .filter(c -> c.getParentCategory() != null && level1Names.contains(c.getParentCategory()))
            .collect(Collectors.groupingBy(Category::getParentCategory, LinkedHashMap::new, Collectors.toList()));

        // Tập tên lớp 2
        Set<String> level2Names = allCategories.stream()
            .filter(c -> c.getParentCategory() != null && level1Names.contains(c.getParentCategory()))
            .map(Category::getName)
            .collect(Collectors.toSet());

        // Lớp 3: categories có parentCategory = một trong tên lớp 2
        Map<String, List<Category>> level3Map = allCategories.stream()
            .filter(c -> c.getParentCategory() != null && level2Names.contains(c.getParentCategory()))
            .collect(Collectors.groupingBy(Category::getParentCategory, LinkedHashMap::new, Collectors.toList()));

        // categoryMap = Map<L1, Map<L2name, List<L3>>>
        Map<String, Map<String, List<Category>>> categoryMap = new LinkedHashMap<>();
        for (String l1 : level1Names) {
            List<Category> l2List = level2Map.getOrDefault(l1, new ArrayList<>());
            if (!l2List.isEmpty()) {
                Map<String, List<Category>> subMap = new LinkedHashMap<>();
                for (Category l2 : l2List) {
                    List<Category> l3List = level3Map.getOrDefault(l2.getName(), new ArrayList<>());
                    subMap.put(l2.getName(), l3List);
                }
                categoryMap.put(l1, subMap);
            }
        }
        model.addAttribute("categoryMap", categoryMap);

        // categoryMap2 = Map<L1 hoặc L2, List<Category>> — dùng cho fallback 2 lớp cũ
        Map<String, List<Category>> categoryMap2 = allCategories.stream()
            .filter(c -> c.getParentCategory() != null && !c.getParentCategory().isEmpty())
            .collect(Collectors.groupingBy(Category::getParentCategory, LinkedHashMap::new, Collectors.toList()));
        model.addAttribute("categoryMap2", categoryMap2);

        return "index";
    }
}
