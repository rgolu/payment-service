package com.payments.service;

import com.payments.domain.exception.DuplicateEntityException;
import com.payments.domain.exception.NotFoundException;
import com.payments.domain.model.User;
import com.payments.domain.money.Money;
import com.payments.repository.UserRepository;
import com.payments.wallet.LockExecutor;
import com.payments.wallet.WalletLedger;

import java.util.concurrent.ConcurrentHashMap;

public class UserService {

    private record CreditReceipt(String userId, Money amount) {
    }

    private final UserRepository users;
    private final WalletLedger ledger;
    private final LockExecutor locks;
    private final IdGenerator ids;
    private final ConcurrentHashMap<String, CreditReceipt> credits = new ConcurrentHashMap<>();

    public UserService(UserRepository users, WalletLedger ledger, LockExecutor locks, IdGenerator ids) {
        this.users = users;
        this.ledger = ledger;
        this.locks = locks;
        this.ids = ids;
    }

    public User register(String name, Money initialBalance) {
        User user = new User(ids.userId(), name, initialBalance);
        return users.save(user);
    }

    public User get(String id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("user not found: " + id));
    }

    public User topUp(String id, Money amount, String creditId) {
        if (amount.isZero()) {
            throw new IllegalArgumentException("top-up amount must be positive");
        }
        return locks.execute("credit:" + creditId, () -> {
            CreditReceipt previous = credits.get(creditId);
            if (previous != null) {
                if (previous.userId().equals(id) && previous.amount().equals(amount)) {
                    return get(id);
                }
                throw new DuplicateEntityException("credit_id already exists with a different request: " + creditId);
            }
            User user = get(id);
            ledger.topUp(user, amount);
            credits.put(creditId, new CreditReceipt(id, amount));
            return user;
        });
    }
}
