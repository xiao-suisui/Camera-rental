-- Schema for camera rental platform (MySQL 8.x)
CREATE DATABASE IF NOT EXISTS camera_rental
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE camera_rental;

-- Users
CREATE TABLE IF NOT EXISTS `user` (
  id BIGINT NOT NULL AUTO_INCREMENT,
  phone VARCHAR(64) NOT NULL,
  password VARCHAR(128) NOT NULL,
  nickname VARCHAR(64) NOT NULL,
  avatar VARCHAR(256) DEFAULT NULL,
  address VARCHAR(128) DEFAULT NULL,
  role ENUM('USER','ADMIN') NOT NULL,
  status ENUM('ACTIVE','DISABLED') NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_phone (phone)
) ENGINE=InnoDB;

-- Devices
CREATE TABLE IF NOT EXISTS device (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  brand VARCHAR(64) NOT NULL,
  model VARCHAR(64) NOT NULL,
  description VARCHAR(2000) NOT NULL,
  image_paths TEXT NOT NULL,
  rent_price DECIMAL(10,2) NOT NULL,
  deposit DECIMAL(10,2) NOT NULL,
  address VARCHAR(128) NOT NULL,
  audit_status ENUM('PENDING','APPROVED','REJECTED') NOT NULL,
  status ENUM('ACTIVE','OFFLINE') NOT NULL,
  batch_id VARCHAR(64) DEFAULT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_device_user (user_id),
  KEY idx_device_brand (brand),
  KEY idx_device_status (status),
  KEY idx_device_audit (audit_status),
  CONSTRAINT fk_device_user FOREIGN KEY (user_id) REFERENCES `user` (id)
) ENGINE=InnoDB;

-- Orders
CREATE TABLE IF NOT EXISTS `order` (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_no VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  device_id BIGINT NOT NULL,
  lessor_id BIGINT NOT NULL,
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  total_rent DECIMAL(10,2) NOT NULL,
  deposit DECIMAL(10,2) NOT NULL,
  coupon_id BIGINT DEFAULT NULL,
  discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  pay_amount DECIMAL(10,2) NOT NULL,
  virtual_order_no VARCHAR(64) DEFAULT NULL,
  status ENUM('PENDING_PAY','PAID','TO_SHIP','TO_RETURN','RETURNED','OVERDUE','FINISHED','CANCELED') NOT NULL,
  deposit_status ENUM('UNPAID','PAID','REFUNDED') NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_order_user (user_id),
  KEY idx_order_device (device_id),
  KEY idx_order_status (status),
  KEY idx_order_status_created (status, created_at),
  CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES `user` (id),
  CONSTRAINT fk_order_device FOREIGN KEY (device_id) REFERENCES device (id),
  CONSTRAINT fk_order_lessor FOREIGN KEY (lessor_id) REFERENCES `user` (id),
  CONSTRAINT fk_order_coupon FOREIGN KEY (coupon_id) REFERENCES coupon (id)
) ENGINE=InnoDB;

-- Coupons
CREATE TABLE IF NOT EXISTS coupon (
  id BIGINT NOT NULL AUTO_INCREMENT,
  type ENUM('NO_THRESHOLD','THRESHOLD') NOT NULL,
  denomination DECIMAL(10,2) NOT NULL,
  threshold DECIMAL(10,2) DEFAULT NULL,
  total_count INT NOT NULL,
  used_count INT NOT NULL DEFAULT 0,
  valid_days INT NOT NULL DEFAULT 7,
  status ENUM('DRAFT','PUBLISHED','EXPIRED','OFFLINE') NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_coupon_status (status)
) ENGINE=InnoDB;

-- User coupons
CREATE TABLE IF NOT EXISTS user_coupon (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  coupon_id BIGINT NOT NULL,
  receive_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expire_time DATETIME NOT NULL,
  use_status ENUM('UNUSED','USED','EXPIRED') NOT NULL,
  order_id BIGINT DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_coupon (user_id, coupon_id),
  KEY idx_user_coupon_status (use_status),
  CONSTRAINT fk_user_coupon_user FOREIGN KEY (user_id) REFERENCES `user` (id),
  CONSTRAINT fk_user_coupon_coupon FOREIGN KEY (coupon_id) REFERENCES coupon (id),
  CONSTRAINT fk_user_coupon_order FOREIGN KEY (order_id) REFERENCES `order` (id)
) ENGINE=InnoDB;

-- Evaluations
CREATE TABLE IF NOT EXISTS evaluation (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  evaluator_id BIGINT NOT NULL,
  evaluated_id BIGINT NOT NULL,
  device_id BIGINT NOT NULL,
  score INT NOT NULL,
  content VARCHAR(1000) NOT NULL,
  image_paths TEXT DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_eval_order (order_id),
  KEY idx_eval_device (device_id),
  KEY idx_eval_evaluated (evaluated_id),
  CONSTRAINT fk_eval_order FOREIGN KEY (order_id) REFERENCES `order` (id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_eval_evaluator FOREIGN KEY (evaluator_id) REFERENCES `user` (id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_eval_evaluated FOREIGN KEY (evaluated_id) REFERENCES `user` (id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_eval_device FOREIGN KEY (device_id) REFERENCES device (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- Messages
CREATE TABLE IF NOT EXISTS message (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  content VARCHAR(1000) NOT NULL,
  type ENUM('DEVICE_AUDIT','ORDER_STATUS','COUPON','SYSTEM') NOT NULL,
  read_status ENUM('UNREAD','READ') NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_message_user (user_id),
  KEY idx_message_read (read_status),
  CONSTRAINT fk_message_user FOREIGN KEY (user_id) REFERENCES `user` (id)
) ENGINE=InnoDB;

-- Device favorites
CREATE TABLE IF NOT EXISTS device_favorite (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  device_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_device (user_id, device_id),
  KEY idx_favorite_user (user_id),
  KEY idx_favorite_device (device_id),
  CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE,
  CONSTRAINT fk_favorite_device FOREIGN KEY (device_id) REFERENCES device (id) ON DELETE CASCADE
) ENGINE=InnoDB;

