package com.payments.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.payments.domain.model.User;

import java.math.BigDecimal;
import java.time.Instant;

public record UserResponse(
        @JsonProperty("user_id") String userId,
        @JsonProperty("name") String name,
        @JsonProperty("wallet") BigDecimal wallet,
        @JsonProperty("created_at") Instant createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getWallet().rupees(), user.getCreatedAt());
    }
}
