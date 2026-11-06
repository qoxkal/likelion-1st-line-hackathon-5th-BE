package com.line1.booktalk.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByDeviceIdHash(String deviceIdHash);
}
