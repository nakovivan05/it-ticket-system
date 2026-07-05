package com.ticketsystem.it_ticket_system.service;

import com.ticketsystem.it_ticket_system.dto.PasswordDTO;
import com.ticketsystem.it_ticket_system.dto.UpdateUserDTO;
import com.ticketsystem.it_ticket_system.exception.DuplicateEmailException;
import com.ticketsystem.it_ticket_system.exception.DuplicateUsernameException;
import com.ticketsystem.it_ticket_system.exception.UserNotFoundException;
import com.ticketsystem.it_ticket_system.exception.ValidationException;
import com.ticketsystem.it_ticket_system.dto.UserDTO;
import com.ticketsystem.it_ticket_system.model.EntityType;
import com.ticketsystem.it_ticket_system.model.Operation;
import com.ticketsystem.it_ticket_system.model.User;
import com.ticketsystem.it_ticket_system.model.UserRole;
import com.ticketsystem.it_ticket_system.repository.AuditLogRepository;
import com.ticketsystem.it_ticket_system.repository.TicketRepository;
import com.ticketsystem.it_ticket_system.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService{
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public UserService(UserRepository userRepository, TicketRepository ticketRepository, PasswordEncoder passwordEncoder, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public UserDTO getUserById(Long id) {

        return userRepository
                .findById(id)
                .map(UserDTO::fromEntity)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<UserDTO> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(UserDTO::fromEntity)
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public UserDTO getUserByUsername(String username) {
        return  userRepository.findByUsername(username)
                .map(UserDTO::fromEntity)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UserDTO updateUser(Long id, UpdateUserDTO userDTO) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        if (userDTO.getEmail() != null) {
            if (!existingUser.getEmail().equals(userDTO.getEmail()) &&
                    userRepository.existsByEmail(userDTO.getEmail())) {
                throw new DuplicateEmailException("Email is already registered: " + userDTO.getEmail());
            }
            existingUser.setEmail(userDTO.getEmail());
        }

        if (userDTO.getUsername() != null) {
            if (!existingUser.getUsername().equals(userDTO.getUsername()) &&
                    userRepository.existsByUsername(userDTO.getUsername())) {
                throw new DuplicateUsernameException("Username is already taken: " + userDTO.getUsername());
            }
            existingUser.setUsername(userDTO.getUsername());
        }

        if (userDTO.getRole() != null) {
            existingUser.setRole(userDTO.getRole());
        }

        if (userDTO.getAccountNonExpired() != null) {
            existingUser.setAccountNonExpired(userDTO.getAccountNonExpired());
        }
        if(userDTO.getAccountNonLocked()!=null){
            existingUser.setAccountNonLocked(userDTO.getAccountNonLocked());
        }
        if(userDTO.getEnabled()!=null){
            existingUser.setEnabled(userDTO.getEnabled());
        }
        if(userDTO.getCredentialsNonExpired()!=null)
        {
            existingUser.setCredentialsNonExpired(userDTO.getCredentialsNonExpired());
        }
        User updatedUser = userRepository.save(existingUser);
        auditLogService.auditLog(EntityType.USER, Operation.UPDATE, "User updated: " + updatedUser.getUsername(), id, getCurrentUser());
        return UserDTO.fromEntity(updatedUser);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(Long id) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
        if (ticketRepository.existsByReporterId(id) || ticketRepository.existsByAssigneeId(id)) {
            throw new ValidationException("Cannot delete user that is referenced by tickets");
        }
        userRepository.delete(existingUser);
        auditLogService.auditLog(EntityType.USER, Operation.DELETE, "User deleted: " + existingUser.getUsername(), id, getCurrentUser());
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'EMPLOYEE', 'ADMIN')")
    public void updatePassword(Long id, PasswordDTO passwordDTO) {
        String user = getCurrentUser();
        User currentUser = userRepository.findByUsername(user)
                .orElseThrow(() -> new UserNotFoundException("Current user not found"));
        if (!currentUser.getId().equals(id)) {
            throw new SecurityException("You are not authorized to update this user's password");
        }
        if(!passwordEncoder.matches(passwordDTO.getCurrentPassword(), currentUser.getPassword())) {
            throw new ValidationException("Current password is incorrect");
        }
        currentUser.setPassword(passwordEncoder.encode(passwordDTO.getPassword()));
        userRepository.save(currentUser);
        auditLogService.auditLog(EntityType.USER, Operation.UPDATE, "Password updated", id, getCurrentUser());
    }

    private String getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

}