package com.payments.api;

import com.payments.api.dto.InitiatePaymentRequest;
import com.payments.api.dto.PaymentResponse;
import com.payments.api.dto.RefundRequest;
import com.payments.domain.money.Money;
import com.payments.service.PaymentService;
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

import static com.payments.api.ValidationPatterns.ID;
import static com.payments.api.ValidationPatterns.ID_MSG;

@Validated
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
                request.paymentId(),
                request.userId(),
                request.merchantId(),
                Money.rupees(request.amount()),
                request.method(),
                request.couponCode()
        ));
    }

    @PostMapping("/{payment_id}/complete")
    public PaymentResponse complete(
            @PathVariable("payment_id") @Size(max = 50) @Pattern(regexp = ID, message = ID_MSG) String paymentId
    ) {
        return PaymentResponse.from(payments.complete(paymentId));
    }

    @PostMapping("/{payment_id}/refund")
    public PaymentResponse refund(
            @PathVariable("payment_id") @Size(max = 50) @Pattern(regexp = ID, message = ID_MSG) String paymentId,
            @Valid @RequestBody RefundRequest request
    ) {
        return PaymentResponse.from(payments.refund(paymentId, request.refundId()));
    }

    @GetMapping("/{payment_id}")
    public PaymentResponse get(
            @PathVariable("payment_id") @Size(max = 50) @Pattern(regexp = ID, message = ID_MSG) String paymentId
    ) {
        return PaymentResponse.from(payments.get(paymentId));
    }
}
