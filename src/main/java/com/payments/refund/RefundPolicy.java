package com.payments.refund;

import com.payments.domain.money.Money;

/**
 * Bonus seam: refund fee is max(₹2, 1% of principal), capped at the principal
 * so a tiny payment cannot produce a negative credit.
 */
public class RefundPolicy {

    private static final Money MIN_FEE = Money.rupees("2");

    public Money refundFee(Money principal) {
        Money pct = principal.percent(1);
        Money fee = pct.compareTo(MIN_FEE) >= 0 ? pct : MIN_FEE;
        return fee.compareTo(principal) <= 0 ? fee : principal;
    }
}
