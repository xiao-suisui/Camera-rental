-- ============================================================
-- 修复设备状态：将已完成订单对应的设备从 RENTED 改为 ACTIVE
-- 原因：confirmReturn() 之前没有释放设备状态
-- ============================================================

USE camera_rental;

-- 1. 先查看受影响的设备数量
SELECT COUNT(*) AS affected_devices
FROM device d
WHERE d.status = 'RENTED'
  AND d.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM `order` o
    WHERE o.device_id = d.id
      AND o.status IN ('PENDING_PAY', 'PAID', 'TO_SHIP', 'TO_RETURN', 'RETURNED', 'OVERDUE')
  );

-- 2. 查看受影响的设备详情（可选，用于确认）
SELECT d.id, d.title, d.status AS current_status, d.updated_at
FROM device d
WHERE d.status = 'RENTED'
  AND d.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM `order` o
    WHERE o.device_id = d.id
      AND o.status IN ('PENDING_PAY', 'PAID', 'TO_SHIP', 'TO_RETURN', 'RETURNED', 'OVERDUE')
  );

-- 3. 执行修复：将没有活跃订单的 RENTED 设备改为 ACTIVE
UPDATE device d
SET d.status = 'ACTIVE',
    d.updated_at = NOW()
WHERE d.status = 'RENTED'
  AND d.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM `order` o
    WHERE o.device_id = d.id
      AND o.status IN ('PENDING_PAY', 'PAID', 'TO_SHIP', 'TO_RETURN', 'RETURNED', 'OVERDUE')
  );

-- 4. 验证修复结果
SELECT d.id, d.title, d.status, d.updated_at
FROM device d
WHERE d.deleted = 0
  AND d.status = 'RENTED';
