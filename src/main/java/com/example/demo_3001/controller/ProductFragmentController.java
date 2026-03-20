package com.example.demo_3001.controller;

import com.example.demo_3001.model.Product;
import com.example.demo_3001.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class ProductFragmentController {

    private final ProductService productService;

    @GetMapping("/products/load-more")
    public String loadMoreProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "false") boolean isFlashSale,
            Model model) {
        
        Page<Product> productPage = productService.getProducts(page, size);
        
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("isFlashSale", isFlashSale);
        
        return "fragments/product-list :: product-list";
    }
}
