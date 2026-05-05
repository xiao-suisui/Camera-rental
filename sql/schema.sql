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
      status ENUM('ACTIVE','OFFLINE','RENTED') NOT NULL,
      batch_id VARCHAR(64) DEFAULT NULL,
      deleted TINYINT NOT NULL DEFAULT 0,
      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      PRIMARY KEY (id),
      KEY idx_device_user (user_id),
      KEY idx_device_brand (brand),
      KEY idx_device_status (status),
      KEY idx_device_audit (audit_status),
      KEY idx_device_search (deleted, audit_status, status),
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
      KEY idx_order_lessor (lessor_id),
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
      KEY idx_user_coupon_order (order_id),
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
      reply VARCHAR(1000) DEFAULT NULL,
      replied_at DATETIME DEFAULT NULL,
      replied_by BIGINT DEFAULT NULL,
      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (id),
      KEY idx_eval_order (order_id),
      KEY idx_eval_device (device_id),
      KEY idx_eval_evaluator (evaluator_id),
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

    -- MQ outbox (producer side)
    CREATE TABLE IF NOT EXISTS mq_outbox (
      id BIGINT NOT NULL AUTO_INCREMENT,
      event_id VARCHAR(64) NOT NULL,
      event_type VARCHAR(64) NOT NULL,
      aggregate_id VARCHAR(64) NOT NULL,
      routing_key VARCHAR(128) NOT NULL,
      payload LONGTEXT NOT NULL,
      status VARCHAR(16) NOT NULL DEFAULT 'NEW',
      retry_count INT NOT NULL DEFAULT 0,
      next_retry_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
      sent_at DATETIME DEFAULT NULL,
      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      PRIMARY KEY (id),
      UNIQUE KEY uk_mq_outbox_event_id (event_id),
      KEY idx_mq_outbox_status_retry (status, next_retry_at),
      KEY idx_mq_outbox_aggregate (aggregate_id),
      KEY idx_mq_outbox_created (created_at)
    ) ENGINE=InnoDB;

    -- MQ consume log (consumer side idempotency)
    CREATE TABLE IF NOT EXISTS mq_consume_log (
      id BIGINT NOT NULL AUTO_INCREMENT,
      event_id VARCHAR(64) NOT NULL,
      consumer VARCHAR(128) NOT NULL,
      consumed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (id),
      UNIQUE KEY uk_mq_consume_log_event_consumer (event_id, consumer),
      KEY idx_mq_consume_log_consumed_at (consumed_at)
    ) ENGINE=InnoDB;

    -- Operation log
    CREATE TABLE IF NOT EXISTS operation_log (
      id BIGINT NOT NULL AUTO_INCREMENT,
      type VARCHAR(64) NOT NULL,
      description VARCHAR(256) NOT NULL,
      method VARCHAR(256) DEFAULT NULL,
      url VARCHAR(512) DEFAULT NULL,
      request_method VARCHAR(16) DEFAULT NULL,
      request_params LONGTEXT DEFAULT NULL,
      response_result LONGTEXT DEFAULT NULL,
      user_id BIGINT DEFAULT NULL,
      ip VARCHAR(64) DEFAULT NULL,
      execution_time BIGINT DEFAULT NULL,
      status VARCHAR(16) NOT NULL DEFAULT 'SUCCESS',
      error_msg LONGTEXT DEFAULT NULL,
      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (id),
      KEY idx_operation_log_type (type),
      KEY idx_operation_log_user (user_id),
      KEY idx_operation_log_created (created_at)
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

    -- Chat conversations
    CREATE TABLE IF NOT EXISTS chat_conversation (
      id BIGINT NOT NULL AUTO_INCREMENT,
      user_id1 BIGINT NOT NULL,
      user_id2 BIGINT NOT NULL,
      device_id BIGINT DEFAULT NULL,
      order_id BIGINT DEFAULT NULL,
      last_message VARCHAR(500) DEFAULT NULL,
      last_message_at DATETIME DEFAULT NULL,
      unread_count1 INT NOT NULL DEFAULT 0,
      unread_count2 INT NOT NULL DEFAULT 0,
      deleted_by_user1 TINYINT NOT NULL DEFAULT 0,
      deleted_by_user2 TINYINT NOT NULL DEFAULT 0,
      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
      updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      PRIMARY KEY (id),
      UNIQUE KEY uk_conversation (user_id1, user_id2),
      KEY idx_conv_user1 (user_id1),
      KEY idx_conv_user2 (user_id2)
    ) ENGINE=InnoDB;

    -- Chat messages
    CREATE TABLE IF NOT EXISTS chat_message (
      id BIGINT NOT NULL AUTO_INCREMENT,
      conversation_id BIGINT NOT NULL,
      sender_id BIGINT NOT NULL,
      receiver_id BIGINT NOT NULL,
      content VARCHAR(2000) NOT NULL,
      is_read TINYINT NOT NULL DEFAULT 0,
      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (id),
      KEY idx_msg_conversation (conversation_id, created_at),
      KEY idx_msg_sender (sender_id),
      KEY idx_msg_receiver (receiver_id, is_read),
      CONSTRAINT fk_chat_msg_conv FOREIGN KEY (conversation_id) REFERENCES chat_conversation(id)
    ) ENGINE=InnoDB;

