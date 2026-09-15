package com.payments.service;

import com.payments.TestHarness;
import com.payments.domain.exception.DuplicateEntityException;
import com.payments.domain.exception.NotFoundException;
import com.payments.domain.model.User;
import com.payments.domain.money.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.payments.common.TestConstants.CREDIT_ID;
import static com.payments.common.TestConstants.USER_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UserServiceTest {

    private TestHarness h;
    private User user;

    @BeforeEach
    void setUp() {
        h = new TestHarness();
        user = h.users.register(USER_NAME, Money.rupees("100"));
    }

    @Test
    void register_NewUser_CreatesWallet() {
        assertEquals(USER_NAME, user.getName());
        assertEquals(Money.rupees("100"), user.getWallet());
        assertTrue(user.getId().startsWith("usr_"));
    }

    @Test
    void get_UnknownUser_Throws() {
        assertThrows(NotFoundException.class, () -> h.users.get("usr_missing"));
    }

    @Test
    void topUp_ZeroAmount_Throws() {
        assertThrows(IllegalArgumentException.class, () -> h.users.topUp(user.getId(), Money.ZERO, CREDIT_ID));
    }

    @Test
    void topUp_SameCreditId_Replays() {
        // GIVEN
        h.users.topUp(user.getId(), Money.rupees("50"), CREDIT_ID);

        // TEST
        User replay = h.users.topUp(user.getId(), Money.rupees("50"), CREDIT_ID);

        // VERIFY
        assertEquals(Money.rupees("150"), replay.getWallet());
    }

    @Test
    void topUp_SameCreditIdDifferentPayload_Conflicts() {
        // GIVEN
        h.users.topUp(user.getId(), Money.rupees("50"), CREDIT_ID);

        // TEST + VERIFY
        assertThrows(DuplicateEntityException.class, () -> h.users.topUp(user.getId(), Money.rupees("10"), CREDIT_ID));
    }
}
