package com.payments.api;

import com.payments.api.dto.PaymentResponse;
import com.payments.api.dto.RegisterUserRequest;
import com.payments.api.dto.TopUpRequest;
import com.payments.api.dto.UserResponse;
import com.payments.domain.money.Money;
import com.payments.service.PaymentService;
import com.payments.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable String id) {
        return UserResponse.from(users.get(id));
    }

    @PostMapping("/{id}/topup")
    public UserResponse topUp(@PathVariable String id, @Valid @RequestBody TopUpRequest request) {
        return UserResponse.from(users.topUp(id, Money.rupees(request.amount())));
    }

    @GetMapping("/{id}/transactions")
    public List<PaymentResponse> history(@PathVariable String id) {
        return payments.historyForUser(id).stream().map(PaymentResponse::from).toList();
    }
}
