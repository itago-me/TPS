package com.tps.dto.admin;

/**
 * 文件说明：数据传输对象，负责定义接口入参与出参结构。
 */

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReportResponse {
    private Long id;
    private Long reporterId;
    private Long productId;
    private String productTitle;
    private String productImageUrl;
    private String reason;
    private String status;
    private LocalDateTime createdAt;
}
