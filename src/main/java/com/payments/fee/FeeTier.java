package com.payments.fee;

import com.payments.domain.money.Money;

/**
 * A band of the principal. {@code upTo == null} means the open-ended last band.
 */
public record FeeTier(Money upTo, int rateBps) {
}
