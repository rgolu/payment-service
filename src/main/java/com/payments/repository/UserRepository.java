package com.payments.repository;

import com.payments.domain.model.User;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(String id);
}
