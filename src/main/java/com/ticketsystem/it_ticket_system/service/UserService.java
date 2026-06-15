package com.ticketsystem.it_ticket_system.service;

import com.ticketsystem.it_ticket_system.dto.PasswordDTO;
import com.ticketsystem.it_ticket_system.exception.DuplicateEmailException;
import com.ticketsystem.it_ticket_system.exception.DuplicateUsernameException;
import com.ticketsystem.it_ticket_system.exception.UserNotFoundException;
import com.ticketsystem.it_ticket_system.exception.ValidationException;
import com.ticketsystem.it_ticket_system.dto.UserDTO;
import com.ticketsystem.it_ticket_system.model.User;
import com.ticketsystem.it_ticket_system.model.UserRole;
import com.ticketsystem.it_ticket_system.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private User toEntity(UserDTO userDTO) {
        return User.builder()
                .id(userDTO.getId())
                .email(userDTO.getEmail())
                .username(userDTO.getUsername())
                .role(UserRole.valueOf(userDTO.getRole()))
                .build();
    }

    public UserDTO getUserById(Long id) {

        return userRepository
                .findById(id)
                .map(UserDTO::fromEntity)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }

    public List<UserDTO> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(UserDTO::fromEntity)
                .toList();
    }

    public UserDTO getUserByUsername(String username) {

        return  userRepository.findByUsername(username)
                .map(UserDTO::fromEntity)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));
    }

    @Transactional
    public UserDTO updateUser(Long id, UserDTO userDTO) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        if (userDTO.getEmail() != null) {
            if (userDTO.getEmail().trim().isEmpty()) {
                throw new ValidationException("Email cannot be empty");
            }
            if (!existingUser.getEmail().equals(userDTO.getEmail()) &&
                    userRepository.existsByEmail(userDTO.getEmail())) {
                throw new DuplicateEmailException("Email is already registered: " + userDTO.getEmail());
            }
        }

        if (userDTO.getUsername() != null) {
            if (userDTO.getUsername().trim().isEmpty()) {
                throw new ValidationException("Username cannot be empty");
            }
            if (!existingUser.getUsername().equals(userDTO.getUsername()) &&
                    userRepository.existsByUsername(userDTO.getUsername())) {
                throw new DuplicateUsernameException("Username is already taken: " + userDTO.getUsername());
            }
        }

        if (userDTO.getRole() != null) {
            if (userDTO.getRole().trim().isEmpty()) {
                throw new ValidationException("Role cannot be empty");
            }
        }

        if (userDTO.getEmail() != null) {
            existingUser.setEmail(userDTO.getEmail());
        }
        if (userDTO.getUsername() != null) {
            existingUser.setUsername(userDTO.getUsername());
        }
        if (userDTO.getRole() != null) {
            existingUser.setRole(UserRole.valueOf(userDTO.getRole()));
        }

        User updatedUser = userRepository.save(existingUser);
        return UserDTO.fromEntity(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
        userRepository.delete(existingUser);
    }

    @Transactional
    public void updatePassword(Long id, PasswordDTO passwordDTO) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
        existingUser.setPassword(passwordEncoder.encode(passwordDTO.getPassword()));
        userRepository.save(existingUser);
    }

}