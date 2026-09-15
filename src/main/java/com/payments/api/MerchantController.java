package com.payments.api;

import com.payments.api.dto.MerchantResponse;
import com.payments.api.dto.PaymentResponse;
import com.payments.api.dto.RegisterMerchantRequest;
import com.payments.service.MerchantService;
import com.payments.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/merchants")
public class MerchantController {

    private final MerchantService merchants;
    private final PaymentService payments;

    public MerchantController(MerchantService merchants, PaymentService payments) {
        this.merchants = merchants;
        this.payments = payments;
    }

    @PostMapping
    public MerchantResponse register(@Valid @RequestBody RegisterMerchantRequest request) {
        return MerchantResponse.from(merchants.register(request.name(), request.supportedMethods()));
    }

    @GetMapping("/{id}")
    public MerchantResponse get(@PathVariable String id) {
        return MerchantResponse.from(merchants.get(id));
    }

    @GetMapping("/{id}/transactions")
    public List<PaymentResponse> history(@PathVariable String id) {
        return payments.historyForMerchant(id).stream().map(PaymentResponse::from).toList();
    }
}
