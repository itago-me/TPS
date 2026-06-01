package com.tps.config;

/**
 * 文件说明：启动初始化器，负责执行兼容迁移并确保管理员账号存在。
 */

import com.tps.entity.User;
import com.tps.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final String ADMIN_PHONE = "18888888888";
    private static final String ADMIN_PASSWORD = "admin123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        // 先补兼容迁移，再创建管理员账号，避免老库缺字段时启动即失败。
        applySchemaCompatibilityMigrations();

        User admin = userRepository.findByPhone(ADMIN_PHONE).orElseGet(User::new);
        admin.setPhone(ADMIN_PHONE);
        admin.setStudentId("00000000");
        admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setNickname("admin");
        admin.setRole(User.Role.ADMIN);
        admin.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(admin);
    }

    private void applySchemaCompatibilityMigrations() {
        // 这里做的是“尽量向前兼容”的本地开发迁移，不追求严谨版本管理，只保证旧库能跑起来。
        execute("ALTER TABLE users ADD COLUMN student_id VARCHAR(32) NULL COMMENT '学号'");
        execute("CREATE UNIQUE INDEX uk_users_student_id ON users(student_id)");
        execute("ALTER TABLE users ADD COLUMN shipping_address VARCHAR(255) NULL COMMENT '收货地址'");
        execute("ALTER TABLE users MODIFY COLUMN status ENUM('ACTIVE','BANNED','DEACTIVATED') DEFAULT 'ACTIVE' COMMENT '账号状态'");

        execute("ALTER TABLE products ADD COLUMN favorite_count INT DEFAULT 0 COMMENT '收藏数'");
        execute("ALTER TABLE products ADD COLUMN bumped_at DATETIME NULL COMMENT '擦亮时间'");
        execute("ALTER TABLE products ADD COLUMN bump_count_today INT DEFAULT 0 COMMENT '今日擦亮次数'");
        execute("ALTER TABLE products ADD COLUMN last_bump_date DATE NULL COMMENT '最近擦亮日期'");
        execute("ALTER TABLE products ADD COLUMN takedown_reason VARCHAR(255) NULL COMMENT '管理员下架原因'");
        execute("ALTER TABLE products ADD COLUMN takedown_by BIGINT NULL COMMENT '下架管理员ID'");
        execute("ALTER TABLE products ADD COLUMN takedown_at DATETIME NULL COMMENT '管理员下架时间'");

        execute("ALTER TABLE messages ADD COLUMN is_read TINYINT DEFAULT 0 COMMENT '是否已读'");

        execute("ALTER TABLE orders ADD COLUMN tracking_number VARCHAR(100) NULL COMMENT '物流单号'");
        execute("ALTER TABLE orders MODIFY COLUMN status ENUM('PENDING','PAID','SHIPPED','DONE','CANCELLED','REFUNDING','REFUNDED') DEFAULT 'PENDING' COMMENT '订单状态'");

        execute("""
                CREATE TABLE IF NOT EXISTS browsing_history (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  user_id BIGINT NOT NULL COMMENT '用户ID',
                  product_id BIGINT NOT NULL COMMENT '商品ID',
                  viewed_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
                  UNIQUE KEY uk_history_user_product (user_id, product_id),
                  INDEX idx_history_user_viewed (user_id, viewed_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);

        execute("""
                CREATE TABLE IF NOT EXISTS feedback (
                  id BIGINT PRIMARY KEY AUTO_INCREMENT,
                  user_id BIGINT NOT NULL COMMENT '用户ID',
                  type VARCHAR(30) COMMENT '反馈类型',
                  content VARCHAR(1000) NOT NULL COMMENT '反馈内容',
                  contact VARCHAR(100) COMMENT '联系方式',
                  status ENUM('PENDING','PROCESSING','DONE','CLOSED') DEFAULT 'PENDING' COMMENT '处理状态',
                  reply VARCHAR(1000) COMMENT '回复内容',
                  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  INDEX idx_feedback_user (user_id),
                  INDEX idx_feedback_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void execute(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception ignored) {
            // Best-effort migration for existing local databases; duplicate columns/indexes are expected.
        }
    }
}
