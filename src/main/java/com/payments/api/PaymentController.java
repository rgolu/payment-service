package com.payments.api;

import com.payments.api.dto.InitiatePaymentRequest;
import com.payments.api.dto.PaymentResponse;
import com.payments.domain.money.Money;
import com.payments.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService payments;

    public PaymentController(PaymentService payments) {
        this.payments = payments;
    }

    @PostMapping("/initiate")
    public PaymentResponse initiate(@Valid @RequestBody InitiatePaymentRequest request) {
        return PaymentResponse.from(payments.initiate(
                request.userId(),
                request.merchantId(),
                Money.rupees(request.amount()),
                request.method(),
                request.couponCode()
        ));
    }

    @PostMapping("/{id}/complete")
    public PaymentResponse complete(@PathVariable String id) {
        return PaymentResponse.from(payments.complete(id));
    }

    @PostMapping("/{id}/refund")
    public PaymentResponse refund(@PathVariable String id) {
        return PaymentResponse.from(payments.refund(id));
    }

    @GetMapping("/{id}")
    public PaymentResponse get(@PathVariable String id) {
        return PaymentResponse.from(payments.get(id));
    }
}
