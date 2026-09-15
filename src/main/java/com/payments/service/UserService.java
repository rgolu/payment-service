package com.payments.service;

import com.payments.domain.exception.NotFoundException;
import com.payments.domain.model.User;
import com.payments.domain.money.Money;
import com.payments.repository.UserRepository;
import com.payments.wallet.WalletLedger;

public class UserService {

    private final UserRepository users;
    private final WalletLedger ledger;
    private final IdGenerator ids;

    public UserService(UserRepository users, WalletLedger ledger, IdGenerator ids) {
        this.users = users;
        this.ledger = ledger;
        this.ids = ids;
    }

    public User register(String name, Money initialBalance) {
        User user = new User(ids.userId(), name, initialBalance);
        return users.save(user);
    }

    public User get(String id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("user not found: " + id));
    }

    public User topUp(String id, Money amount) {
        if (amount.isZero()) {
            throw new IllegalArgumentException("top-up amount must be positive");
        }
        User user = get(id);
        ledger.topUp(user, amount);
        return user;
    }
}
