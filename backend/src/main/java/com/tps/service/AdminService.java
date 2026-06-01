package com.tps.service;

/**
 * 文件说明：业务服务层，负责封装核心业务规则、事务与对象组装。
 */

import com.tps.dto.admin.ReportResponse;
import com.tps.dto.order.OrderResponse;
import com.tps.dto.product.ProductResponse;
import com.tps.dto.user.UserProfileResponse;
import com.tps.entity.*;
import com.tps.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ReportRepository reportRepository;
    private final NotificationRepository notificationRepository;
    private final ProductService productService;
    private final ProductImageRepository productImageRepository;
    private final FileService fileService;

    public Page<UserProfileResponse> getUsers(int page, int size) {
        return userRepository.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(this::toUserProfile);
    }

    @Transactional
    public void banUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        user.setStatus(User.UserStatus.BANNED);
        userRepository.save(user);
    }

    @Transactional
    public void unbanUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        user.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user);
    }

    private final OrderService orderService;

    public Page<ProductResponse> getProducts(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (status == null || status.isBlank()) {
            return productRepository.findAll(pageable).map(product -> productService.toResponse(product, null));
        }
        Product.ProductStatus productStatus = Product.ProductStatus.valueOf(status);
        return productRepository.findByStatus(productStatus, pageable)
                .map(product -> productService.toResponse(product, null));
    }

    public Page<ReportResponse> getReportedProducts(int page, int size) {
        return reportRepository.findByStatus(Report.ReportStatus.PENDING,
                PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(this::toReportResponse);
    }

    @Transactional
    public void takedownProduct(Long productId) {
        takedownProduct(productId, "平台审核下架", null);
    }

    @Transactional
    public void takedownProduct(Long productId, String reason, Long adminId) {
        String normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedReason.isBlank()) {
            throw new IllegalArgumentException("下架原因不能为空");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (product.getStatus() == Product.ProductStatus.SOLD) {
            throw new IllegalArgumentException("已售出商品不能强制下架");
        }
        product.setStatus(Product.ProductStatus.OFF);
        product.setTakedownReason(normalizedReason);
        product.setTakedownBy(adminId);
        product.setTakedownAt(LocalDateTime.now());
        productRepository.save(product);

        Notification notification = new Notification();
        notification.setUserId(product.getUserId());
        notification.setType("PRODUCT_TAKEDOWN");
        notification.setTitle("商品已被平台下架");
        notification.setContent("你的商品《" + product.getTitle() + "》已被平台下架，原因：" + normalizedReason);
        notificationRepository.save(notification);
    }

    public Page<OrderResponse> getOrders(int page, int size) {
        return orderRepository.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(orderService::toResponse);
    }

    public Page<OrderResponse> getRefundingOrders(int page, int size) {
        return orderRepository.findByStatus(Order.OrderStatus.REFUNDING,
                PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(orderService::toResponse);
    }

    @Transactional
    public void handleReport(Long reportId, boolean takedown) {
        handleReport(reportId, takedown, null, null);
    }

    @Transactional
    public void handleReport(Long reportId, boolean takedown, String reason, Long adminId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("举报不存在"));
        if (takedown) {
            String takedownReason = reason == null || reason.isBlank() ? report.getReason() : reason;
            takedownProduct(report.getProductId(), takedownReason, adminId);
        }
        report.setStatus(Report.ReportStatus.HANDLED);
        reportRepository.save(report);
    }

    @Transactional
    public void approveRefund(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        if (order.getStatus() != Order.OrderStatus.REFUNDING) {
            throw new IllegalArgumentException("订单不在退款中");
        }
        order.setStatus(Order.OrderStatus.REFUNDED);
        productRepository.findById(order.getProductId()).ifPresent(product -> {
            product.setStatus(Product.ProductStatus.ON_SALE);
            productRepository.save(product);
        });
        orderRepository.save(order);
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalProducts", productRepository.count());
        stats.put("totalOrders", orderRepository.count());
        stats.put("totalAmount", orderRepository.sumFinalPrice());
        return stats;
    }

    private UserProfileResponse toUserProfile(User user) {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setPhone(user.getPhone());
        response.setStudentId(user.getStudentId());
        response.setNickname(user.getNickname());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setBio(user.getBio());
        response.setLocation(user.getLocation());
        response.setShippingAddress(user.getShippingAddress());
        response.setCreditScore(user.getCreditScore());
        response.setRole(user.getRole().name());
        response.setStatus(user.getStatus().name());
        response.setProductCount(productRepository.findByUserId(user.getId()).size());
        return response;
    }

    private ReportResponse toReportResponse(Report report) {
        ReportResponse response = new ReportResponse();
        response.setId(report.getId());
        response.setReporterId(report.getReporterId());
        response.setProductId(report.getProductId());
        productRepository.findById(report.getProductId())
                .ifPresent(product -> response.setProductTitle(product.getTitle()));
        productImageRepository.findByProductIdOrderBySortOrder(report.getProductId()).stream()
                .findFirst()
                .ifPresent(image -> response.setProductImageUrl(fileService.toAbsoluteUrl(image.getImageUrl())));
        response.setReason(report.getReason());
        response.setStatus(report.getStatus().name());
        response.setCreatedAt(report.getCreatedAt());
        return response;
    }

    @Transactional
    public void sendAnnouncement(String content) {
        // 给所有用户创建系统通知
        userRepository.findAll().forEach(user -> {
            Notification n = new Notification();
            n.setUserId(user.getId());
            n.setType("SYSTEM");
            n.setContent(content);
            notificationRepository.save(n);
        });
    }
}
