package com.example.userservicefeb25.repositories;

import com.example.userservicefeb25.models.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByValue(String value);
}
