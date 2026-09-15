package com.payments.api;

import com.payments.api.dto.PaymentResponse;
import com.payments.api.dto.RegisterUserRequest;
import com.payments.api.dto.TopUpRequest;
import com.payments.api.dto.UserResponse;
import com.payments.domain.money.Money;
import com.payments.service.PaymentService;
import com.payments.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.payments.api.ValidationPatterns.ID;
import static com.payments.api.ValidationPatterns.ID_MSG;

@Validated
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService users;
    private final PaymentService payments;

    public UserController(UserService users, PaymentService payments) {
        this.users = users;
        this.payments = payments;
    }

    @PostMapping
    public UserResponse register(@Valid @RequestBody RegisterUserRequest request) {
        return UserResponse.from(users.register(request.name(), Money.rupees(request.initialBalance())));
    }

    @GetMapping("/{user_id}")
    public UserResponse get(
            @PathVariable("user_id") @Size(max = 50) @Pattern(regexp = ID, message = ID_MSG) String userId
    ) {
        return UserResponse.from(users.get(userId));
    }

    @PostMapping("/{user_id}/topup")
    public UserResponse topUp(
            @PathVariable("user_id") @Size(max = 50) @Pattern(regexp = ID, message = ID_MSG) String userId,
            @Valid @RequestBody TopUpRequest request
    ) {
        return UserResponse.from(users.topUp(userId, Money.rupees(request.amount()), request.creditId()));
    }

    @GetMapping("/{user_id}/transactions")
    public List<PaymentResponse> history(
            @PathVariable("user_id") @Size(max = 50) @Pattern(regexp = ID, message = ID_MSG) String userId
    ) {
        return payments.historyForUser(userId).stream().map(PaymentResponse::from).toList();
    }
}
