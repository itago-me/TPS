package com.tps.entity;

/**
 * 文件说明：JPA 实体定义，负责映射数据库表结构与领域对象。
 */

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 30)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "`condition`")
    private Condition condition = Condition.GOOD;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status = ProductStatus.ON_SALE;

    @Column(length = 100)
    private String location;

    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Column(name = "favorite_count")
    private Integer favoriteCount = 0;

    @Column(name = "bumped_at")
    private LocalDateTime bumpedAt;

    @Column(name = "bump_count_today")
    private Integer bumpCountToday = 0;

    @Column(name = "last_bump_date")
    private java.time.LocalDate lastBumpDate;

    @Column(name = "takedown_reason", length = 255)
    private String takedownReason;

    @Column(name = "takedown_by")
    private Long takedownBy;

    @Column(name = "takedown_at")
    private LocalDateTime takedownAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private List<ProductImage> images;

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum Condition { NEW, LIKE_NEW, GOOD, FAIR }
    public enum ProductStatus { ON_SALE, SOLD, OFF }
}
