package com.payments.fee;

import com.payments.domain.enums.PaymentMethod;

import java.math.BigDecimal;

/** Bonus seam: peak/load surcharge lives here, not in payment orchestration. */
public interface FeeMultiplier {

    BigDecimal factor(PaymentMethod method);
}
