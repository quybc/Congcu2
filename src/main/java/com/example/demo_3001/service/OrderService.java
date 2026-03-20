package com.example.demo_3001.service;

import com.example.demo_3001.model.CartItem;
import com.example.demo_3001.model.Customer;
import com.example.demo_3001.model.CustomerVoucher;
import com.example.demo_3001.model.Order;
import com.example.demo_3001.model.OrderDetail;
import com.example.demo_3001.model.Product;
import com.example.demo_3001.repository.CustomerRepository;
import com.example.demo_3001.repository.CustomerVoucherRepository;
import com.example.demo_3001.repository.OrderDetailRepository;
import com.example.demo_3001.repository.OrderRepository;
import com.example.demo_3001.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;
    private final CustomerRepository customerRepository;
    private final CustomerVoucherRepository customerVoucherRepository;

    @Transactional
    public Order createOrder(String customerName, String customerPhone, String customerAddress, String customerGender, String paymentMethod) {
        // Find or create Customer
        Customer customer;
        Optional<Customer> existingCustomer = customerRepository.findByPhoneNumber(customerPhone);
        
        if (existingCustomer.isPresent()) {
            customer = existingCustomer.get();
            // Update info if changed (optional, but good for keeping up to date)
            customer.setFullName(customerName);
            customer.setAddress(customerAddress);
            customer.setGender(customerGender);
        } else {
            customer = new Customer();
            customer.setPhoneNumber(customerPhone);
            customer.setFullName(customerName);
            customer.setAddress(customerAddress);
            customer.setGender(customerGender);
            customer.setPoints(0);
        }
        
        // Save customer first to get ID
        customer = customerRepository.save(customer);
        
        Order order = new Order();
        order.setCustomerName(customerName);
        order.setCustomerPhone(customerPhone);
        order.setCustomerAddress(customerAddress);
        order.setCustomerGender(customerGender);
        // Link order to customer (optional, assuming we might add a relationship later, but for now we just use the ID/Phone logic)
        order.setCustomer(customer);
        order.setPaymentMethod(paymentMethod);
        order.setPaymentStatus("PENDING");
        order.setVoucherCode(cartService.getAppliedVoucherCode());
        order.setVoucherDiscount(cartService.getAppliedVoucherDiscount());
        
        double shippingFee = cartService.getShippingFee();
        order.setShippingFee(shippingFee);

        order = orderRepository.save(order);

        double totalOrderPrice = 0;
        List<CartItem> cartItems = cartService.getCartItems();
        List<OrderDetail> orderDetails = new ArrayList<>();

        for (CartItem item : cartItems) {
            Product product = productRepository.findById(item.getProduct().getId()).orElseThrow(() -> new RuntimeException("Product not found"));
            
            int quantityToBuy = item.getQuantity();
            int promoQtyAvailable = product.getPromotionQuantity();
            
            // Check if product is on promotion
            if ("KHUYEN_MAI".equals(product.getProductType())) {
                if (promoQtyAvailable >= quantityToBuy) {
                    // Case 1: Enough promo quantity
                    double promoPrice = product.getPrice() * (1 - product.getDiscount() / 100.0);
                    orderDetails.add(createOrderDetail(order, product, quantityToBuy, promoPrice));
                    totalOrderPrice += promoPrice * quantityToBuy;
                    
                    // Update promotion quantity
                    product.setPromotionQuantity(promoQtyAvailable - quantityToBuy);
                } else if (promoQtyAvailable > 0) {
                    // Case 2: Partial promo quantity
                    double promoPrice = product.getPrice() * (1 - product.getDiscount() / 100.0);
                    double normalPrice = product.getPrice();
                    
                    // Promo part
                    orderDetails.add(createOrderDetail(order, product, promoQtyAvailable, promoPrice));
                    totalOrderPrice += promoPrice * promoQtyAvailable;
                    
                    // Normal part
                    int normalQty = quantityToBuy - promoQtyAvailable;
                    orderDetails.add(createOrderDetail(order, product, normalQty, normalPrice));
                    totalOrderPrice += normalPrice * normalQty;
                    
                    // Update promotion quantity to 0
                    product.setPromotionQuantity(0);
                } else {
                    // Case 3: No promo quantity left, all normal price
                    double normalPrice = product.getPrice();
                    orderDetails.add(createOrderDetail(order, product, quantityToBuy, normalPrice));
                    totalOrderPrice += normalPrice * quantityToBuy;
                }
            } else {
                // Not a promotion product
                double normalPrice = product.getPrice();
                orderDetails.add(createOrderDetail(order, product, quantityToBuy, normalPrice));
                totalOrderPrice += normalPrice * quantityToBuy;
            }
            
            // Trừ số lượng tồn kho tổng
            if (product.getQuantity() < quantityToBuy) {
                throw new RuntimeException("Sản phẩm " + product.getName() + " đã hết hàng trong kho!");
            }
            product.setQuantity(product.getQuantity() - quantityToBuy);

            productRepository.save(product);
        }
        
        double totalAfterVoucher = Math.max(totalOrderPrice - cartService.getAppliedVoucherDiscount(), 0);
        order.setTotalPrice(totalAfterVoucher + shippingFee);
        order.setOrderDetails(orderDetails);
        orderRepository.save(order);
        
        // Update customer points (2 points per 15000 VND)
        int pointsEarned = (int) (totalAfterVoucher / 15000) * 2;
        customer.setPoints(customer.getPoints() + pointsEarned);
        customerRepository.save(customer);

        consumeAppliedVoucher();
        cartService.clearCart();
        return order;
    }

    private void consumeAppliedVoucher() {
        Long customerVoucherId = cartService.getAppliedVoucherId();
        if (customerVoucherId == null) {
            return;
        }
        Optional<CustomerVoucher> customerVoucherOpt = customerVoucherRepository.findById(customerVoucherId);
        if (customerVoucherOpt.isPresent()) {
            CustomerVoucher customerVoucher = customerVoucherOpt.get();
            if ("AVAILABLE".equals(customerVoucher.getStatus())) {
                customerVoucher.setStatus("USED");
                customerVoucherRepository.save(customerVoucher);
            }
        }
        cartService.clearAppliedVoucher();
    }

    private OrderDetail createOrderDetail(Order order, Product product, int quantity, double price) {
        OrderDetail detail = new OrderDetail();
        detail.setOrder(order);
        detail.setProduct(product);
        detail.setQuantity(quantity);
        detail.setPrice(price);
        return orderDetailRepository.save(detail);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Order not found"));
    }
}
