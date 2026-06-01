package com.tps.dto.product;

/**
 * 文件说明：数据传输对象，负责定义接口入参与出参结构。
 */

import com.tps.entity.Product;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductResponse {
    private Long id;
    private Long userId;
    private String sellerNickname;
    private String sellerAvatar;
    private String title;
    private String description;
    private BigDecimal price;
    private String category;
    private String condition;
    private String status;
    private String location;
    private Integer viewCount;
    private Integer favoriteCount;
    private LocalDateTime bumpedAt;
    private String takedownReason;
    private Long takedownBy;
    private LocalDateTime takedownAt;
    private List<String> imageUrls;
    private boolean favorited;
    private LocalDateTime createdAt;
}
