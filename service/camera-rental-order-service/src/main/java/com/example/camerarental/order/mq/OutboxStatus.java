package com.example.camerarental.order.mq;

public final class OutboxStatus {

    private OutboxStatus() {
    }

    public static final String NEW = "NEW";
    public static final String SENT = "SENT";
    public static final String FAILED = "FAILED";
}

