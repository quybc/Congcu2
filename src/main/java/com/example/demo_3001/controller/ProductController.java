package com.example.demo_3001.controller;

import com.example.demo_3001.model.Category;
import com.example.demo_3001.model.Product;
import com.example.demo_3001.service.CartService;
import com.example.demo_3001.service.CategoryService;
import com.example.demo_3001.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/products")
public class ProductController {
    
    private final ProductService productService;
    private final CategoryService categoryService;
    private final CartService cartService;

    @Autowired
    public ProductController(ProductService productService, CategoryService categoryService, CartService cartService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.cartService = cartService;
    }

    @GetMapping
    public String listProducts(Model model) {
        List<Product> products = productService.getAllProducts();
        model.addAttribute("products", products);
        return "products/product-list";
    }

    @GetMapping("/new")
    public String showProductForm(Model model) {
        Product product = new Product();
        model.addAttribute("product", product);
        model.addAttribute("groupedCategories", categoryService.getCategoriesGrouped());
        return "products/product-form";
    }

    @PostMapping("/save")
    public String saveProduct(@ModelAttribute("product") Product product, 
                              @org.springframework.web.bind.annotation.RequestParam("imageProduct") org.springframework.web.multipart.MultipartFile imageProduct) {
        if (!imageProduct.isEmpty()) {
            try {
                String fileName = imageProduct.getOriginalFilename();
                java.nio.file.Path srcPath = java.nio.file.Paths.get("src/main/resources/static/images/" + fileName);
                java.nio.file.Files.createDirectories(srcPath.getParent());
                java.nio.file.Files.write(srcPath, imageProduct.getBytes());

                java.nio.file.Path targetPath = java.nio.file.Paths.get("target/classes/static/images/" + fileName);
                java.nio.file.Files.createDirectories(targetPath.getParent());
                java.nio.file.Files.write(targetPath, imageProduct.getBytes());
                
                product.setImage(fileName);
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
        productService.saveProduct(product);
        return "redirect:/products";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@org.springframework.web.bind.annotation.PathVariable("id") Long id, Model model) {
        Product product = productService.getProductById(id)
            .orElseThrow(() -> new IllegalArgumentException("Invalid product Id:" + id));
        model.addAttribute("product", product);
        model.addAttribute("groupedCategories", categoryService.getCategoriesGrouped());
        return "products/product-form";
    }

    @GetMapping("/detail/{id}")
    public String viewProductDetail(@PathVariable("id") Long id, Model model) {
        Product product = productService.getProductById(id)
            .orElseThrow(() -> new IllegalArgumentException("Invalid product Id:" + id));
        model.addAttribute("product", product);
        model.addAttribute("cartCount", cartService.getCartCount());
        addCategoryMapToModel(model);
        return "products/product-detail";
    }

    private void addCategoryMapToModel(Model model) {
        List<Category> allCategories = categoryService.getAllCategories();
        model.addAttribute("categories", allCategories);

        List<String> level1Names = Arrays.asList(
            "Điện thoại","Laptop","Phụ kiện","Smartwatch","Đồng hồ",
            "Tablet","Máy cũ Thu cũ","Màn hình máy in","Sim Thẻ Cào","Dịch vụ tiện ích"
        );

        Map<String, List<Category>> level2Map = allCategories.stream()
            .filter(c -> c.getParentCategory() != null && level1Names.contains(c.getParentCategory()))
            .collect(Collectors.groupingBy(Category::getParentCategory, LinkedHashMap::new, Collectors.toList()));

        Set<String> level2Names = allCategories.stream()
            .filter(c -> c.getParentCategory() != null && level1Names.contains(c.getParentCategory()))
            .map(Category::getName)
            .collect(Collectors.toSet());

        Map<String, List<Category>> level3Map = allCategories.stream()
            .filter(c -> c.getParentCategory() != null && level2Names.contains(c.getParentCategory()))
            .collect(Collectors.groupingBy(Category::getParentCategory, LinkedHashMap::new, Collectors.toList()));

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
    }

    @GetMapping("/delete/{id}")
    public String deleteProduct(@org.springframework.web.bind.annotation.PathVariable("id") Long id) {
        productService.deleteProductById(id);
        return "redirect:/products";
    }
}
