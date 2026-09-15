package com.payments.service;

import java.util.UUID;

public class IdGenerator {

    public String userId() {
        return "usr_" + shortId();
    }

    public String merchantId() {
        return "mer_" + shortId();
    }

    public String paymentId() {
        return "pay_" + shortId();
    }

    private String shortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
