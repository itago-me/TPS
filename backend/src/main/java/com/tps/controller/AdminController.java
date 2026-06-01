package com.tps.controller;

/**
 * 文件说明：控制器层，负责接收相关 HTTP 请求并委托业务层处理。
 */

import com.tps.dto.ApiResponse;
import com.tps.dto.PageResponse;
import com.tps.dto.feedback.FeedbackReplyRequest;
import com.tps.service.AdminService;
import com.tps.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final FeedbackService feedbackService;

    @GetMapping("/users")
    public ApiResponse<?> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(PageResponse.from(adminService.getUsers(page, size)));
    }

    @PutMapping("/users/{id}/ban")
    public ApiResponse<?> banUser(@PathVariable Long id) {
        adminService.banUser(id);
        return ApiResponse.success();
    }

    @PutMapping("/users/{id}/unban")
    public ApiResponse<?> unbanUser(@PathVariable Long id) {
        adminService.unbanUser(id);
        return ApiResponse.success();
    }

    @GetMapping("/products/reported")
    public ApiResponse<?> getReportedProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(PageResponse.from(adminService.getReportedProducts(page, size)));
    }

    @GetMapping("/reports")
    public ApiResponse<?> getReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(PageResponse.from(adminService.getReportedProducts(page, size)));
    }

    @GetMapping("/products")
    public ApiResponse<?> getProducts(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(PageResponse.from(adminService.getProducts(status, page, size)));
    }

    @PutMapping("/products/{id}/takedown")
    public ApiResponse<?> takedownProduct(@PathVariable Long id,
                                          @RequestParam String reason,
                                          Authentication authentication) {
        Long adminId = authentication != null && authentication.getPrincipal() instanceof Long
                ? (Long) authentication.getPrincipal()
                : null;
        adminService.takedownProduct(id, reason, adminId);
        return ApiResponse.success();
    }

    @PutMapping("/reports/{id}/handle")
    public ApiResponse<?> handleReport(@PathVariable Long id,
                                       @RequestParam(defaultValue = "true") boolean takedown,
                                       @RequestParam(required = false) String reason,
                                       Authentication authentication) {
        Long adminId = authentication != null && authentication.getPrincipal() instanceof Long
                ? (Long) authentication.getPrincipal()
                : null;
        adminService.handleReport(id, takedown, reason, adminId);
        return ApiResponse.success();
    }

    @GetMapping("/orders")
    public ApiResponse<?> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(PageResponse.from(adminService.getOrders(page, size)));
    }

    @GetMapping("/orders/refunding")
    public ApiResponse<?> getRefundingOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(PageResponse.from(adminService.getRefundingOrders(page, size)));
    }

    @PutMapping("/orders/{id}/refund/approve")
    public ApiResponse<?> approveRefund(@PathVariable Long id) {
        adminService.approveRefund(id);
        return ApiResponse.success();
    }

    @GetMapping("/stats")
    public ApiResponse<?> getStats() {
        return ApiResponse.success(adminService.getStats());
    }

    @PostMapping("/notifications")
    public ApiResponse<?> sendAnnouncement(@RequestParam String content) {
        adminService.sendAnnouncement(content);
        return ApiResponse.success();
    }

    @GetMapping("/feedback")
    public ApiResponse<?> getFeedback(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(PageResponse.from(feedbackService.adminList(status, page, size)));
    }

    @PutMapping("/feedback/{id}/reply")
    public ApiResponse<?> replyFeedback(@PathVariable Long id,
                                        @Valid @RequestBody FeedbackReplyRequest request) {
        return ApiResponse.success(feedbackService.reply(id, request.getReply()));
    }

    @PutMapping("/feedback/{id}/status")
    public ApiResponse<?> updateFeedbackStatus(@PathVariable Long id,
                                               @RequestParam String status) {
        return ApiResponse.success(feedbackService.updateStatus(id, status));
    }
}
