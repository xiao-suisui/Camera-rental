-- ====================================================================================
-- Camera Rental Platform - Test Data
-- 相机租赁平台测试数据
-- 版本: 2.0
-- 更新时间: 2026-02-10
-- ====================================================================================

USE camera_rental;

-- 清空现有数据（可选，如果需要重新初始化）
-- TRUNCATE TABLE device_favorite;
-- TRUNCATE TABLE evaluation;
-- TRUNCATE TABLE message;
-- TRUNCATE TABLE user_coupon;
-- TRUNCATE TABLE `order`;
-- TRUNCATE TABLE coupon;
-- TRUNCATE TABLE device;
-- TRUNCATE TABLE `user`;

-- ====================================================================================
-- 1. 用户数据 (Users)
-- ====================================================================================
-- 说明：
-- - 所有用户密码经过BCrypt加密
-- - 管理员账号: 13800000000 / admin123
-- - 普通用户: 13800138000-13800138004 / 123456
-- ====================================================================================

INSERT INTO `user` (id, phone, password, nickname, avatar, address, role, status, created_at, updated_at) VALUES
-- 管理员账号 (密码: admin123)
(1, '13800000000', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', NULL, '北京市朝阳区CBD商务中心', 'ADMIN', 'ACTIVE', NOW(), NOW()),

-- 普通用户账号 (密码: 123456)
(2, '13800138000', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '摄影爱好者小王', 'https://minio.example.com/avatars/user2.jpg', '上海市浦东新区陆家嘴金融区', 'USER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 25 DAY), DATE_SUB(NOW(), INTERVAL 25 DAY)),

(3, '13800138001', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '摄影师李明', 'https://minio.example.com/avatars/user3.jpg', '广州市天河区珠江新城', 'USER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 24 DAY), DATE_SUB(NOW(), INTERVAL 24 DAY)),

(4, '13800138002', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '旅行博主张三', 'https://minio.example.com/avatars/user4.jpg', '深圳市南山区科技园', 'USER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 23 DAY), DATE_SUB(NOW(), INTERVAL 23 DAY)),

(5, '13800138003', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '婚礼摄影师王五', 'https://minio.example.com/avatars/user5.jpg', '杭州市西湖区文二路', 'USER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 22 DAY), DATE_SUB(NOW(), INTERVAL 22 DAY)),

(6, '13800138004', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '摄影工作室赵六', 'https://minio.example.com/avatars/user6.jpg', '成都市武侯区科华北路', 'USER', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 21 DAY), DATE_SUB(NOW(), INTERVAL 21 DAY));

-- ====================================================================================
-- 2. 设备数据 (Devices)
-- ====================================================================================
-- 说明：
-- - 包含佳能、索尼、尼康、富士等主流品牌
-- - 状态包括：已审核上架、待审核、已下架
-- - 包含批量发布的设备示例
-- ====================================================================================

INSERT INTO device (id, user_id, title, brand, model, description, image_paths, rent_price, deposit, address, audit_status, status, batch_id, deleted, created_at, updated_at) VALUES

-- 佳能相机系列 (用户3发布)
(1, 3, '佳能EOS R5 全画幅微单相机', 'Canon', 'EOS R5', '佳能EOS R5是一款专业级全画幅微单相机，配备4500万像素传感器，支持8K视频录制，是摄影师和视频制作者的理想选择。机身状况极好，配件齐全，包含原装充电器、两块电池、肩带等。适合婚礼摄影、商业摄影、视频制作等专业场景。',
'https://minio.example.com/devices/canon-r5-1.jpg,https://minio.example.com/devices/canon-r5-2.jpg,https://minio.example.com/devices/canon-r5-3.jpg',
350.00, 5000.00, '广州市天河区珠江新城', 'APPROVED', 'ACTIVE', NULL, 0, DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY)),

(2, 3, '佳能EOS 5D Mark IV 单反相机', 'Canon', '5D Mark IV', '经典的佳能5D4，3040万像素全画幅传感器，61点自动对焦系统，4K视频拍摄。经过专业清洁保养，功能完好，快门次数约3万次。适合婚礼摄影、商业摄影、人像摄影等专业场景。配原装电池2块、充电器、相机包等配件。',
'https://minio.example.com/devices/canon-5d4-1.jpg,https://minio.example.com/devices/canon-5d4-2.jpg',
280.00, 4000.00, '广州市天河区珠江新城', 'APPROVED', 'ACTIVE', NULL, 0, DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY)),

-- 索尼相机系列 (用户4发布)
(3, 4, '索尼A7M4 全画幅微单相机', 'Sony', 'A7 IV', '索尼A7M4是目前最受欢迎的全画幅微单之一，3300万像素，693点相位对焦，10fps连拍，4K 60P视频。具有出色的视频性能和对焦能力，是vlog拍摄和视频创作的完美选择。机身九成新，屏幕无划痕，功能完好。配原装电池3块、充电器等。',
'https://minio.example.com/devices/sony-a7m4-1.jpg,https://minio.example.com/devices/sony-a7m4-2.jpg,https://minio.example.com/devices/sony-a7m4-3.jpg',
320.00, 4500.00, '深圳市南山区科技园', 'APPROVED', 'ACTIVE', NULL, 0, DATE_SUB(NOW(), INTERVAL 19 DAY), DATE_SUB(NOW(), INTERVAL 19 DAY)),

(4, 4, '索尼A7S3 专业视频微单', 'Sony', 'A7S III', '索尼A7S3是视频拍摄神器，1200万像素，超强暗光性能，4K 120P视频，10bit 4:2:2内录。暗光拍摄能力极强，是婚礼录像、活动拍摄、纪录片制作的最佳选择。机身成色95新，配原装电池4块、V90存储卡2张等配件。',
'https://minio.example.com/devices/sony-a7s3-1.jpg,https://minio.example.com/devices/sony-a7s3-2.jpg',
380.00, 5500.00, '深圳市南山区科技园', 'APPROVED', 'ACTIVE', NULL, 0, DATE_SUB(NOW(), INTERVAL 19 DAY), DATE_SUB(NOW(), INTERVAL 19 DAY)),

-- 尼康相机系列 (用户5发布)
(5, 5, '尼康Z6II 全画幅微单相机', 'Nikon', 'Z6 II', '尼康Z6II，2450万像素，双EXPEED 6处理器，273点混合对焦系统，4K 60P视频。经典可靠的全能机型，适合各类摄影场景。机身保养良好，无磕碰，功能完美。配尼康原装电池2块、充电器、相机包等。',
'https://minio.example.com/devices/nikon-z6ii-1.jpg,https://minio.example.com/devices/nikon-z6ii-2.jpg',
300.00, 4200.00, '杭州市西湖区文二路', 'APPROVED', 'ACTIVE', NULL, 0, DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_SUB(NOW(), INTERVAL 18 DAY)),

(6, 5, '尼康D850 全画幅单反相机', 'Nikon', 'D850', '尼康D850是单反时代的巅峰之作，4575万像素，153点对焦系统，7fps连拍，4K视频。超高像素适合风光摄影、商业摄影、建筑摄影等。机身快门次数约2万次，成色九成新。配原装电池2块、MB-D18手柄、充电器等配件。',
'https://minio.example.com/devices/nikon-d850-1.jpg,https://minio.example.com/devices/nikon-d850-2.jpg,https://minio.example.com/devices/nikon-d850-3.jpg',
290.00, 4300.00, '杭州市西湖区文二路', 'APPROVED', 'ACTIVE', NULL, 0, DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_SUB(NOW(), INTERVAL 18 DAY)),

-- 富士相机系列 (用户6发布)
(7, 6, '富士X-T4 APS-C微单相机', 'Fujifilm', 'X-T4', '富士X-T4，2610万像素X-Trans CMOS 4传感器，机身防抖，4K 60P视频，胶片模拟模式。富士经典的色彩科学，直出JPG即可使用，非常适合街拍、旅行摄影和视频vlog拍摄。机身九成新，配原装电池3块、充电器等。',
'https://minio.example.com/devices/fuji-xt4-1.jpg,https://minio.example.com/devices/fuji-xt4-2.jpg',
250.00, 3500.00, '成都市武侯区科华北路', 'APPROVED', 'ACTIVE', NULL, 0, DATE_SUB(NOW(), INTERVAL 17 DAY), DATE_SUB(NOW(), INTERVAL 17 DAY)),

(8, 6, '富士X-H2S 旗舰微单相机', 'Fujifilm', 'X-H2S', '富士X-H2S，2616万像素堆栈式传感器，40fps连拍，6.2K 30P视频。富士APS-C画幅旗舰，堆栈式传感器带来极快的读取速度，适合运动摄影、野生动物拍摄、专业视频制作。全新机器，配件齐全，支持长租优惠。',
'https://minio.example.com/devices/fuji-xh2s-1.jpg,https://minio.example.com/devices/fuji-xh2s-2.jpg',
330.00, 4800.00, '成都市武侯区科华北路', 'APPROVED', 'ACTIVE', NULL, 0, DATE_SUB(NOW(), INTERVAL 17 DAY), DATE_SUB(NOW(), INTERVAL 17 DAY)),

-- 待审核的设备 (用户2发布)
(9, 2, '佳能EOS R6 Mark II 微单相机', 'Canon', 'EOS R6 II', '佳能最新款全画幅微单，2420万像素，40fps电子快门连拍，6K超采样4K视频。全新机器，刚购入不久，配件齐全，支持长租优惠。机身配原装电池2块、充电器、64GB CFexpress Type B存储卡等。',
'https://minio.example.com/devices/canon-r6ii-1.jpg,https://minio.example.com/devices/canon-r6ii-2.jpg',
360.00, 5200.00, '上海市浦东新区陆家嘴', 'PENDING', 'OFFLINE', NULL, 0, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),

(10, 2, '索尼A7R5 高像素微单相机', 'Sony', 'A7R V', '索尼A7R5，6100万像素，AI智能对焦，8K 24P视频。超高像素适合商业摄影、风光摄影等对画质要求极高的场景。全新机器，配原装电池3块、充电器、128GB CFexpress Type A存储卡2张等配件。',
'https://minio.example.com/devices/sony-a7r5-1.jpg,https://minio.example.com/devices/sony-a7r5-2.jpg',
400.00, 6000.00, '上海市浦东新区陆家嘴', 'PENDING', 'OFFLINE', NULL, 0, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),

-- 批量发布的设备 - 婚礼摄影套装 (用户5发布，共3台示例)
(11, 5, '尼康Z6II 全画幅微单相机 #1', 'Nikon', 'Z6 II', '尼康Z6II，2450万像素，双EXPEED 6处理器，273点混合对焦系统，4K 60P视频。婚礼摄影套装，共10台同款设备可租，适合婚礼摄影团队批量租赁。机身成色九成新，配原装电池2块、充电器等。',
'https://minio.example.com/devices/nikon-z6ii-batch-1.jpg',
280.00, 4000.00, '杭州市西湖区文二路', 'APPROVED', 'ACTIVE', 'BATCH20260124001', 0, DATE_SUB(NOW(), INTERVAL 16 DAY), DATE_SUB(NOW(), INTERVAL 16 DAY)),

(12, 5, '尼康Z6II 全画幅微单相机 #2', 'Nikon', 'Z6 II', '尼康Z6II，2450万像素，双EXPEED 6处理器，273点混合对焦系统，4K 60P视频。婚礼摄影套装，共10台同款设备可租，适合婚礼摄影团队批量租赁。机身成色九成新，配原装电池2块、充电器等。',
'https://minio.example.com/devices/nikon-z6ii-batch-2.jpg',
280.00, 4000.00, '杭州市西湖区文二路', 'APPROVED', 'ACTIVE', 'BATCH20260124001', 0, DATE_SUB(NOW(), INTERVAL 16 DAY), DATE_SUB(NOW(), INTERVAL 16 DAY)),

(13, 5, '尼康Z6II 全画幅微单相机 #3', 'Nikon', 'Z6 II', '尼康Z6II，2450万像素，双EXPEED 6处理器，273点混合对焦系统，4K 60P视频。婚礼摄影套装，共10台同款设备可租，适合婚礼摄影团队批量租赁。机身成色九成新，配原装电池2块、充电器等。',
'https://minio.example.com/devices/nikon-z6ii-batch-3.jpg',
280.00, 4000.00, '杭州市西湖区文二路', 'APPROVED', 'ACTIVE', 'BATCH20260124001', 0, DATE_SUB(NOW(), INTERVAL 16 DAY), DATE_SUB(NOW(), INTERVAL 16 DAY)),

-- 已下架的设备 (用户3)
(14, 3, '佳能EOS R 全画幅微单相机', 'Canon', 'EOS R', '佳能首款全画幅微单，3030万像素，5655个对焦点，4K 30P视频。已售出，暂不出租。',
'https://minio.example.com/devices/canon-r-1.jpg',
200.00, 3000.00, '广州市天河区珠江新城', 'APPROVED', 'OFFLINE', NULL, 0, DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY));

-- ====================================================================================
-- 3. 优惠券数据 (Coupons)
-- ====================================================================================
-- 说明：
-- - 包含无门槛券和满减券
-- - 状态包括：已发布、已过期
-- ====================================================================================

INSERT INTO coupon (id, type, denomination, threshold, total_count, used_count, valid_days, status, created_at, updated_at) VALUES

-- 无门槛优惠券
(1, 'NO_THRESHOLD', 20.00, NULL, 1000, 150, 30, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 25 DAY), DATE_SUB(NOW(), INTERVAL 25 DAY)),
(2, 'NO_THRESHOLD', 50.00, NULL, 500, 80, 30, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY)),

-- 满减优惠券
(3, 'THRESHOLD', 100.00, 500.00, 300, 45, 30, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY)),
(4, 'THRESHOLD', 200.00, 1000.00, 200, 20, 30, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY)),

-- 新用户专享
(5, 'NO_THRESHOLD', 30.00, NULL, 10000, 500, 7, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY)),

-- 已过期的优惠券
(6, 'NO_THRESHOLD', 10.00, NULL, 100, 100, 7, 'EXPIRED', DATE_SUB(NOW(), INTERVAL 40 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY));

-- ====================================================================================
-- 4. 用户优惠券数据 (User Coupons)
-- ====================================================================================

INSERT INTO user_coupon (id, user_id, coupon_id, receive_time, expire_time, use_status, order_id) VALUES

-- 用户2的优惠券
(1, 2, 1, DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_ADD(NOW(), INTERVAL 10 DAY), 'UNUSED', NULL),
(2, 2, 3, DATE_SUB(NOW(), INTERVAL 14 DAY), DATE_ADD(NOW(), INTERVAL 16 DAY), 'UNUSED', NULL),
(3, 2, 5, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY), 'UNUSED', NULL),

-- 用户3的优惠券
(4, 3, 1, DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_ADD(NOW(), INTERVAL 12 DAY), 'UNUSED', NULL),
(5, 3, 2, DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_ADD(NOW(), INTERVAL 15 DAY), 'USED', 2),

-- 用户4的优惠券
(6, 4, 1, DATE_SUB(NOW(), INTERVAL 17 DAY), DATE_ADD(NOW(), INTERVAL 13 DAY), 'UNUSED', NULL),
(7, 4, 3, DATE_SUB(NOW(), INTERVAL 13 DAY), DATE_ADD(NOW(), INTERVAL 17 DAY), 'UNUSED', NULL),

-- 用户5的优惠券
(8, 5, 5, DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_ADD(NOW(), INTERVAL 0 DAY), 'UNUSED', NULL),

-- 用户6的优惠券（已过期）
(9, 6, 6, DATE_SUB(NOW(), INTERVAL 35 DAY), DATE_SUB(NOW(), INTERVAL 28 DAY), 'EXPIRED', NULL);

-- ====================================================================================
-- 5. 订单数据 (Orders)
-- ====================================================================================
-- 说明：
-- - 包含各种状态的订单：已完成、已归还、租赁中、待发货、待支付、已取消
-- - 模拟真实的租赁场景和时间线
-- ====================================================================================

INSERT INTO `order` (id, order_no, user_id, device_id, lessor_id, start_time, end_time, total_rent, deposit, coupon_id, discount_amount, pay_amount, virtual_order_no, status, deposit_status, created_at, updated_at) VALUES

-- 已完成的订单
(1, 'ORD20260125101520ABC001', 2, 1, 3, DATE_SUB(NOW(), INTERVAL 14 DAY), DATE_SUB(NOW(), INTERVAL 11 DAY), 1050.00, 5000.00, NULL, 0.00, 6050.00, 'VORD20260125101520001', 'FINISHED', 'REFUNDED', DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY)),

(2, 'ORD20260126143025DEF002', 3, 3, 4, DATE_SUB(NOW(), INTERVAL 13 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY), 1280.00, 4500.00, 2, 50.00, 5730.00, 'VORD20260126143025002', 'FINISHED', 'REFUNDED', DATE_SUB(NOW(), INTERVAL 14 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY)),

-- 已归还待评价
(3, 'ORD20260128092341GHI003', 4, 5, 5, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 7 DAY), 900.00, 4200.00, NULL, 0.00, 5100.00, 'VORD20260128092341003', 'RETURNED', 'REFUNDED', DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),

-- 租赁中（即将到期）
(4, 'ORD20260201154512JKL004', 2, 4, 4, DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY), 3040.00, 5500.00, 1, 20.00, 8520.00, 'VORD20260201154512004', 'TO_RETURN', 'PAID', DATE_SUB(NOW(), INTERVAL 9 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),

(5, 'ORD20260203110023MNO005', 3, 7, 6, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_ADD(NOW(), INTERVAL 4 DAY), 2500.00, 3500.00, NULL, 0.00, 6000.00, 'VORD20260203110023005', 'TO_RETURN', 'PAID', DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),

-- 待发货
(6, 'ORD20260205083456PQR006', 4, 2, 3, DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 4 DAY), 560.00, 4000.00, 3, 100.00, 4460.00, 'VORD20260205083456006', 'TO_SHIP', 'PAID', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),

-- 待支付
(7, 'ORD20260207192034STU007', 5, 8, 6, DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 5 DAY), 990.00, 4800.00, NULL, 0.00, 5790.00, NULL, 'PENDING_PAY', 'UNPAID', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),

(8, 'ORD20260208112345VWX008', 6, 6, 5, DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 6 DAY), 870.00, 4300.00, NULL, 0.00, 5170.00, NULL, 'PENDING_PAY', 'UNPAID', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),

-- 已取消
(9, 'ORD20260120143025YZA009', 2, 1, 3, DATE_SUB(NOW(), INTERVAL 18 DAY), DATE_SUB(NOW(), INTERVAL 15 DAY), 1050.00, 5000.00, NULL, 0.00, 6050.00, NULL, 'CANCELED', 'UNPAID', DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 19 DAY));

-- ====================================================================================
-- 6. 评价数据 (Evaluations)
-- ====================================================================================
-- 说明：
-- - 包含租客对出租者的评价和出租者对租客的评价
-- - 评分范围1-5星
-- ====================================================================================

INSERT INTO evaluation (id, order_id, evaluator_id, evaluated_id, device_id, score, content, image_paths, created_at) VALUES

-- 租客评价出租者
(1, 1, 2, 3, 1, 5, '设备非常好，李明很专业，交接顺利，相机性能出色，拍摄效果满意。推荐！设备保养得很好，配件齐全，完全符合描述。',
'https://minio.example.com/evaluations/eval1-1.jpg,https://minio.example.com/evaluations/eval1-2.jpg',
DATE_SUB(NOW(), INTERVAL 10 DAY)),

(2, 2, 3, 4, 3, 5, '张三的设备维护得很好，索尼A7M4拍摄视频非常流畅，对焦快速准确。满分好评！交接过程很专业，还分享了很多使用技巧。',
'https://minio.example.com/evaluations/eval2-1.jpg',
DATE_SUB(NOW(), INTERVAL 9 DAY)),

-- 出租者评价租客
(3, 1, 3, 2, 1, 5, '小王很爱惜设备，按时归还，交流愉快，是个好租客。设备归还时非常干净，没有任何问题。',
NULL,
DATE_SUB(NOW(), INTERVAL 10 DAY)),

(4, 2, 4, 3, 3, 5, '李明使用设备很规范，归还及时，设备完好无损。好评！推荐这位租客，值得信赖。',
NULL,
DATE_SUB(NOW(), INTERVAL 9 DAY));

-- ====================================================================================
-- 7. 消息数据 (Messages)
-- ====================================================================================
-- 说明：
-- - 包含设备审核通知、订单状态通知、优惠券通知、系统通知
-- ====================================================================================

INSERT INTO message (id, user_id, content, type, read_status, created_at) VALUES

-- 设备审核通知
(1, 3, '您的设备【佳能EOS R5 全画幅微单相机】已通过审核，现已上架。', 'DEVICE_AUDIT', 'READ', DATE_SUB(NOW(), INTERVAL 20 DAY)),
(2, 3, '您的设备【佳能EOS 5D Mark IV 单反相机】已通过审核，现已上架。', 'DEVICE_AUDIT', 'READ', DATE_SUB(NOW(), INTERVAL 20 DAY)),
(3, 2, '您的设备【佳能EOS R6 Mark II 微单相机】正在审核中，请耐心等待。', 'DEVICE_AUDIT', 'UNREAD', DATE_SUB(NOW(), INTERVAL 2 DAY)),

-- 订单状态通知
(4, 2, '您的订单【ORD20260125101520ABC001】已支付成功，出租者将尽快发货。', 'ORDER_STATUS', 'READ', DATE_SUB(NOW(), INTERVAL 15 DAY)),
(5, 2, '您的订单【ORD20260125101520ABC001】已完成，感谢您的使用！押金已退还。', 'ORDER_STATUS', 'READ', DATE_SUB(NOW(), INTERVAL 10 DAY)),
(6, 2, '您的订单【ORD20260201154512JKL004】租赁期即将到期，请按时归还设备。', 'ORDER_STATUS', 'UNREAD', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(7, 4, '您的订单【ORD20260205083456PQR006】已支付成功，出租者将尽快发货。', 'ORDER_STATUS', 'READ', DATE_SUB(NOW(), INTERVAL 5 DAY)),

-- 优惠券通知
(8, 2, '恭喜您获得一张20元无门槛优惠券，有效期30天，快去使用吧！', 'COUPON', 'READ', DATE_SUB(NOW(), INTERVAL 20 DAY)),
(9, 5, '新用户专享30元优惠券已发放，快去领取吧！', 'COUPON', 'UNREAD', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(10, 3, '您领取的50元优惠券即将过期，请尽快使用！', 'COUPON', 'UNREAD', DATE_ADD(NOW(), INTERVAL 0 DAY)),

-- 系统通知
(11, 2, '欢迎加入相机租赁平台！完善个人信息可获得更多优惠。', 'SYSTEM', 'READ', DATE_SUB(NOW(), INTERVAL 25 DAY)),
(12, 3, '平台推出春节优惠活动，租赁满500元立减100元！', 'SYSTEM', 'UNREAD', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(13, 4, '您的账户安全等级较低，建议完善个人信息并开启双重认证。', 'SYSTEM', 'UNREAD', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(14, 5, '恭喜您成为平台优质出租者，享受更多推广权益！', 'SYSTEM', 'UNREAD', DATE_SUB(NOW(), INTERVAL 16 DAY));

-- ====================================================================================
-- 8. 收藏数据 (Device Favorites)
-- ====================================================================================
-- 说明：
-- - 用户收藏的设备列表
-- ====================================================================================

INSERT INTO device_favorite (id, user_id, device_id, created_at) VALUES

-- 用户2的收藏
(1, 2, 3, DATE_SUB(NOW(), INTERVAL 19 DAY)),
(2, 2, 4, DATE_SUB(NOW(), INTERVAL 19 DAY)),
(3, 2, 7, DATE_SUB(NOW(), INTERVAL 17 DAY)),

-- 用户3的收藏
(4, 3, 5, DATE_SUB(NOW(), INTERVAL 18 DAY)),
(5, 3, 6, DATE_SUB(NOW(), INTERVAL 18 DAY)),

-- 用户4的收藏
(6, 4, 1, DATE_SUB(NOW(), INTERVAL 20 DAY)),
(7, 4, 8, DATE_SUB(NOW(), INTERVAL 17 DAY)),
(8, 4, 2, DATE_SUB(NOW(), INTERVAL 16 DAY)),

-- 用户5的收藏
(9, 5, 3, DATE_SUB(NOW(), INTERVAL 19 DAY)),
(10, 5, 4, DATE_SUB(NOW(), INTERVAL 19 DAY)),

-- 用户6的收藏
(11, 6, 1, DATE_SUB(NOW(), INTERVAL 20 DAY)),
(12, 6, 2, DATE_SUB(NOW(), INTERVAL 20 DAY));

-- ====================================================================================
-- 设置自增ID起始值
-- ====================================================================================
ALTER TABLE `user` AUTO_INCREMENT = 7;
ALTER TABLE device AUTO_INCREMENT = 15;
ALTER TABLE coupon AUTO_INCREMENT = 7;
ALTER TABLE user_coupon AUTO_INCREMENT = 10;
ALTER TABLE `order` AUTO_INCREMENT = 10;
ALTER TABLE evaluation AUTO_INCREMENT = 5;
ALTER TABLE message AUTO_INCREMENT = 15;
ALTER TABLE device_favorite AUTO_INCREMENT = 13;

-- ====================================================================================
-- 数据统计查询
-- ====================================================================================
SELECT '========================================' as '';
SELECT '测试数据初始化完成！' as message;
SELECT '========================================' as '';
SELECT CONCAT('用户总数: ', COUNT(*), ' (',
    SUM(CASE WHEN role='ADMIN' THEN 1 ELSE 0 END), ' 管理员 + ',
    SUM(CASE WHEN role='USER' THEN 1 ELSE 0 END), ' 普通用户)') as user_stats
FROM `user`;

SELECT CONCAT('设备总数: ', COUNT(*), ' (',
    SUM(CASE WHEN audit_status='APPROVED' AND status='ACTIVE' THEN 1 ELSE 0 END), ' 已上架 + ',
    SUM(CASE WHEN audit_status='PENDING' THEN 1 ELSE 0 END), ' 待审核 + ',
    SUM(CASE WHEN status='OFFLINE' THEN 1 ELSE 0 END), ' 已下架)') as device_stats
FROM device;

SELECT CONCAT('订单总数: ', COUNT(*), ' (',
    SUM(CASE WHEN status='FINISHED' THEN 1 ELSE 0 END), ' 已完成 + ',
    SUM(CASE WHEN status='TO_RETURN' THEN 1 ELSE 0 END), ' 租赁中 + ',
    SUM(CASE WHEN status='TO_SHIP' THEN 1 ELSE 0 END), ' 待发货 + ',
    SUM(CASE WHEN status='PENDING_PAY' THEN 1 ELSE 0 END), ' 待支付 + ',
    SUM(CASE WHEN status='CANCELED' THEN 1 ELSE 0 END), ' 已取消)') as order_stats
FROM `order`;

SELECT CONCAT('优惠券: ', COUNT(*), ' (',
    SUM(CASE WHEN status='PUBLISHED' THEN 1 ELSE 0 END), ' 可用 + ',
    SUM(CASE WHEN status='EXPIRED' THEN 1 ELSE 0 END), ' 已过期)') as coupon_stats
FROM coupon;

SELECT CONCAT('评价总数: ', COUNT(*)) as evaluation_stats FROM evaluation;
SELECT CONCAT('消息总数: ', COUNT(*), ' (',
    SUM(CASE WHEN read_status='UNREAD' THEN 1 ELSE 0 END), ' 未读 + ',
    SUM(CASE WHEN read_status='READ' THEN 1 ELSE 0 END), ' 已读)') as message_stats
FROM message;
SELECT CONCAT('收藏总数: ', COUNT(*)) as favorite_stats FROM device_favorite;
SELECT '========================================' as '';

-- ====================================================================================
-- 数据初始化完成
-- ====================================================================================
