package com.payments.fee;

import com.payments.domain.enums.PaymentMethod;
import com.payments.domain.money.Money;

public interface FeePolicy {

    Money quoteFee(Money amount, PaymentMethod feeMethod);
}
