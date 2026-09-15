package com.payments.api.dto;

import com.payments.domain.model.User;

import java.math.BigDecimal;
import java.time.Instant;

public record UserResponse(String id, String name, BigDecimal wallet, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getWallet().rupees(), user.getCreatedAt());
    }
}
