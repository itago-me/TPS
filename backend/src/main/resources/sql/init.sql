-- 脚本说明：数据库初始化脚本，负责创建核心表结构与基础数据。

CREATE DATABASE IF NOT EXISTS tps DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE tps;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  phone         VARCHAR(11) UNIQUE NOT NULL COMMENT '手机号',
  student_id    VARCHAR(32) UNIQUE COMMENT '学号',
  password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
  nickname      VARCHAR(50) COMMENT '昵称',
  avatar_url    VARCHAR(255) COMMENT '头像URL',
  credit_score  INT DEFAULT 100 COMMENT '信用分',
  role          ENUM('USER','ADMIN') DEFAULT 'USER' COMMENT '角色',
  status        ENUM('ACTIVE','BANNED','DEACTIVATED') DEFAULT 'ACTIVE' COMMENT '账号状态',
  bio           VARCHAR(200) COMMENT '简介',
  location      VARCHAR(100) COMMENT '所在地',
  shipping_address VARCHAR(255) COMMENT '收货地址',
  created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 商品表
CREATE TABLE IF NOT EXISTS products (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT NOT NULL COMMENT '发布者ID',
  title       VARCHAR(100) NOT NULL COMMENT '标题',
  description TEXT COMMENT '描述',
  price       DECIMAL(10,2) NOT NULL COMMENT '价格',
  category    VARCHAR(30) COMMENT '分类',
  `condition` ENUM('NEW','LIKE_NEW','GOOD','FAIR') DEFAULT 'GOOD' COMMENT '成色',
  status      ENUM('ON_SALE','SOLD','OFF') DEFAULT 'ON_SALE' COMMENT '状态',
  location    VARCHAR(100) COMMENT '所在地',
  view_count  INT DEFAULT 0 COMMENT '浏览量',
  favorite_count INT DEFAULT 0 COMMENT '收藏数',
  bumped_at   DATETIME COMMENT '擦亮时间',
  bump_count_today INT DEFAULT 0 COMMENT '今日擦亮次数',
  last_bump_date DATE COMMENT '最近擦亮日期',
  takedown_reason VARCHAR(255) COMMENT '管理员下架原因',
  takedown_by BIGINT COMMENT '下架管理员ID',
  takedown_at DATETIME COMMENT '管理员下架时间',
  created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_id (user_id),
  INDEX idx_status (status),
  INDEX idx_category (category),
  INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 商品图片表
CREATE TABLE IF NOT EXISTS product_images (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL COMMENT '商品ID',
  image_url  VARCHAR(255) NOT NULL COMMENT '图片URL',
  sort_order INT DEFAULT 0 COMMENT '排序',
  INDEX idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 收藏表
CREATE TABLE IF NOT EXISTS favorites (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id    BIGINT NOT NULL COMMENT '用户ID',
  product_id BIGINT NOT NULL COMMENT '商品ID',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_product (user_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 会话表
CREATE TABLE IF NOT EXISTS conversations (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  buyer_id      BIGINT NOT NULL COMMENT '买家ID',
  seller_id     BIGINT NOT NULL COMMENT '卖家ID',
  product_id    BIGINT NOT NULL COMMENT '商品ID',
  last_message  VARCHAR(255) COMMENT '最新消息',
  unread_buyer  INT DEFAULT 0 COMMENT '买家未读数',
  unread_seller INT DEFAULT 0 COMMENT '卖家未读数',
  updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_buyer_seller_product (buyer_id, seller_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 消息表
CREATE TABLE IF NOT EXISTS messages (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  conversation_id BIGINT NOT NULL COMMENT '会话ID',
  sender_id       BIGINT NOT NULL COMMENT '发送者ID',
  content         TEXT NOT NULL COMMENT '消息内容',
  type            ENUM('TEXT','IMAGE') DEFAULT 'TEXT' COMMENT '消息类型',
  is_read         TINYINT DEFAULT 0 COMMENT '是否已读',
  created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_conversation_id (conversation_id),
  INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单表
CREATE TABLE IF NOT EXISTS orders (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL COMMENT '商品ID',
  buyer_id   BIGINT NOT NULL COMMENT '买家ID',
  seller_id  BIGINT NOT NULL COMMENT '卖家ID',
  price      DECIMAL(10,2) NOT NULL COMMENT '成交价',
  status     ENUM('PENDING','PAID','SHIPPED','DONE','CANCELLED','REFUNDING','REFUNDED') DEFAULT 'PENDING' COMMENT '订单状态',
  remark     VARCHAR(255) COMMENT '备注',
  tracking_number VARCHAR(100) COMMENT '物流单号',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_buyer_id (buyer_id),
  INDEX idx_seller_id (seller_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 评价表
CREATE TABLE IF NOT EXISTS reviews (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id    BIGINT NOT NULL COMMENT '订单ID',
  reviewer_id BIGINT NOT NULL COMMENT '评价人ID',
  reviewee_id BIGINT NOT NULL COMMENT '被评价人ID',
  score       INT NOT NULL COMMENT '评分1-5',
  content     VARCHAR(500) COMMENT '评价内容',
  created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 举报表
CREATE TABLE IF NOT EXISTS reports (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  reporter_id BIGINT NOT NULL COMMENT '举报人ID',
  product_id  BIGINT NOT NULL COMMENT '被举报商品ID',
  reason      VARCHAR(255) COMMENT '举报原因',
  status      ENUM('PENDING','HANDLED') DEFAULT 'PENDING' COMMENT '处理状态',
  created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 通知表
CREATE TABLE IF NOT EXISTS notifications (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id    BIGINT NOT NULL COMMENT '接收用户ID',
  type       VARCHAR(30) COMMENT '通知类型',
  title      VARCHAR(100) COMMENT '标题',
  content    VARCHAR(500) COMMENT '内容',
  is_read    TINYINT DEFAULT 0 COMMENT '是否已读',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 浏览历史表
CREATE TABLE IF NOT EXISTS browsing_history (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id    BIGINT NOT NULL COMMENT '用户ID',
  product_id BIGINT NOT NULL COMMENT '商品ID',
  viewed_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
  UNIQUE KEY uk_history_user_product (user_id, product_id),
  INDEX idx_history_user_viewed (user_id, viewed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 客服反馈表
CREATE TABLE IF NOT EXISTS feedback (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id    BIGINT NOT NULL COMMENT '用户ID',
  type       VARCHAR(30) COMMENT '反馈类型',
  content    VARCHAR(1000) NOT NULL COMMENT '反馈内容',
  contact    VARCHAR(100) COMMENT '联系方式',
  status     ENUM('PENDING','PROCESSING','DONE','CLOSED') DEFAULT 'PENDING' COMMENT '处理状态',
  reply      VARCHAR(1000) COMMENT '回复内容',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_feedback_user (user_id),
  INDEX idx_feedback_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 初始管理员账号（密码: admin123，BCrypt加密）
INSERT IGNORE INTO users (phone, student_id, password_hash, nickname, role, status)
VALUES ('18000000000', '00000000', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '平台管理员', 'ADMIN', 'ACTIVE');
