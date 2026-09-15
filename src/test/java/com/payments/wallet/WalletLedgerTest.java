package com.payments.wallet;

import com.payments.domain.enums.PaymentMethod;
import com.payments.domain.exception.InsufficientBalanceException;
import com.payments.domain.exception.InvalidStateException;
import com.payments.domain.model.Merchant;
import com.payments.domain.model.User;
import com.payments.domain.money.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WalletLedgerTest {

    private WalletLedger ledger;
    private User user;
    private Merchant merchant;

    @BeforeEach
    void setUp() {
        ledger = new WalletLedger(new LockExecutor());
        user = new User("usr_1", "Rishav", Money.rupees("100"));
        merchant = new Merchant("mer_1", "Cafe", Set.of(PaymentMethod.UPI));
    }

    @Test
    void debitUser_EnoughBalance_DecreasesWallet() {
        ledger.debitUser(user, Money.rupees("40"));
        assertEquals(Money.rupees("60"), user.getWallet());
    }

    @Test
    void debitUser_Insufficient_Throws() {
        assertThrows(InsufficientBalanceException.class, () -> ledger.debitUser(user, Money.rupees("101")));
        assertEquals(Money.rupees("100"), user.getWallet());
    }

    @Test
    void creditUserAndTopUp_IncreaseWallet() {
        ledger.creditUser(user, Money.rupees("5"));
        ledger.topUp(user, Money.rupees("10"));
        assertEquals(Money.rupees("115"), user.getWallet());
    }

    @Test
    void creditThenDebitMerchant_SettlementMustCoverRefund() {
        ledger.creditMerchant(merchant, Money.rupees("50"));
        ledger.debitMerchant(merchant, Money.rupees("50"));
        assertEquals(Money.ZERO, merchant.getSettlement());
        assertThrows(InvalidStateException.class, () -> ledger.debitMerchant(merchant, Money.rupees("1")));
    }
}
