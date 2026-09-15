package com.payments.api;

import com.payments.api.dto.MerchantResponse;
import com.payments.api.dto.PaymentResponse;
import com.payments.api.dto.RegisterMerchantRequest;
import com.payments.service.MerchantService;
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

import java.util.List;

import static com.payments.api.ValidationPatterns.ID;
import static com.payments.api.ValidationPatterns.ID_MSG;

@Validated
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

    @GetMapping("/{merchant_id}")
    public MerchantResponse get(
            @PathVariable("merchant_id") @Size(max = 50) @Pattern(regexp = ID, message = ID_MSG) String merchantId
    ) {
        return MerchantResponse.from(merchants.get(merchantId));
    }

    @GetMapping("/{merchant_id}/transactions")
    public List<PaymentResponse> history(
            @PathVariable("merchant_id") @Size(max = 50) @Pattern(regexp = ID, message = ID_MSG) String merchantId
    ) {
        return payments.historyForMerchant(merchantId).stream().map(PaymentResponse::from).toList();
    }
}
