package com.kntransport.backend.repository;

import com.kntransport.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByRole(User.Role role);
    Page<User> findAllByOrderByNameAsc(Pageable pageable);
    Page<User> findByRoleOrderByNameAsc(User.Role role, Pageable pageable);
}
