package com.example.camerarental.common.mq;

/**
 * RabbitMQ constants shared by producer and consumer services.
 */
public final class MqConstants {

    private MqConstants() {
    }

    public static final String ORDER_TOPIC_EXCHANGE = "camera.rental.order.topic.v1";
    public static final String ORDER_DLX_EXCHANGE = "camera.rental.order.dlx.v1";

    public static final String USER_NOTIFY_QUEUE = "camera.rental.user.notify.q";
    public static final String USER_NOTIFY_DLQ = "camera.rental.user.notify.q.dlq";
    public static final String DEVICE_SYNC_QUEUE = "camera.rental.device.sync.q";
    public static final String DEVICE_SYNC_DLQ = "camera.rental.device.sync.q.dlq";
    public static final String COUPON_SYNC_QUEUE = "camera.rental.coupon.sync.q";
    public static final String COUPON_SYNC_DLQ = "camera.rental.coupon.sync.q.dlq";

    public static final String ORDER_NOTIFY_USER_ROUTING_KEY = "order.*.notify.user";
    public static final String ORDER_CREATED_NOTIFY_USER_KEY = "order.created.notify.user";
    public static final String ORDER_PAID_NOTIFY_USER_KEY = "order.paid.notify.user";
    public static final String ORDER_SHIPPED_NOTIFY_USER_KEY = "order.shipped.notify.user";
    public static final String ORDER_RECEIVED_NOTIFY_USER_KEY = "order.received.notify.user";
    public static final String ORDER_RETURN_APPLIED_NOTIFY_USER_KEY = "order.return.applied.notify.user";
    public static final String ORDER_RETURN_CONFIRMED_NOTIFY_USER_KEY = "order.return.confirmed.notify.user";
    public static final String ORDER_CANCELLED_NOTIFY_USER_KEY = "order.cancelled.notify.user";
    public static final String ORDER_CANCELLED_RELEASE_STOCK_KEY = "order.cancelled.release.stock";
    public static final String ORDER_CANCELLED_ROLLBACK_COUPON_KEY = "order.cancelled.rollback.coupon";

    public static final String DISPUTE_CREATED_NOTIFY_ADMIN_KEY = "dispute.created.notify.admin";
    public static final String DISPUTE_RESOLVED_NOTIFY_USER_KEY = "dispute.resolved.notify.user";

    public static final String USER_NOTIFY_DLQ_ROUTING_KEY = "order.notify.user.dead";
    public static final String DEVICE_SYNC_DLQ_ROUTING_KEY = "order.cancelled.release.stock.dead";
    public static final String COUPON_SYNC_DLQ_ROUTING_KEY = "order.cancelled.rollback.coupon.dead";
}


