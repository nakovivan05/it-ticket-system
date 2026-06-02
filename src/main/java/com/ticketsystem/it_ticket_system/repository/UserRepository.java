package com.ticketsystem.it_ticket_system.repository;

import com.ticketsystem.it_ticket_system.model.User;
import com.ticketsystem.it_ticket_system.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    List<User> findByRole(UserRole role);
}
