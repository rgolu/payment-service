package com.payments.service;

import com.payments.domain.enums.PaymentMethod;
import com.payments.domain.exception.NotFoundException;
import com.payments.domain.exception.PaymentException;
import com.payments.domain.model.Merchant;
import com.payments.repository.MerchantRepository;

import java.util.Set;

public class MerchantService {

    private final MerchantRepository merchants;
    private final IdGenerator ids;

    public MerchantService(MerchantRepository merchants, IdGenerator ids) {
        this.merchants = merchants;
        this.ids = ids;
    }

    public Merchant register(String name, Set<PaymentMethod> methods) {
        if (methods == null || methods.isEmpty()) {
            throw new PaymentException("merchant must support at least one payment method");
        }
        Merchant merchant = new Merchant(ids.merchantId(), name, methods);
        return merchants.save(merchant);
    }

    public Merchant get(String id) {
        return merchants.findById(id).orElseThrow(() -> new NotFoundException("merchant not found: " + id));
    }
}
