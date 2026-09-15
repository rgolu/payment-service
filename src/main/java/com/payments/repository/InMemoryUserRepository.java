package com.payments.repository;

import com.payments.domain.model.User;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryUserRepository implements UserRepository {

    private final ConcurrentHashMap<String, User> store = new ConcurrentHashMap<>();

    @Override
    public User save(User user) {
        store.put(user.getId(), user);
        return user;
    }

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }
}
