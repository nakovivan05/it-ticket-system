package com.ticketsystem.it_ticket_system.service;

import com.ticketsystem.it_ticket_system.UserNotFoundException;
import com.ticketsystem.it_ticket_system.dto.UserDTO;
import com.ticketsystem.it_ticket_system.model.User;
import com.ticketsystem.it_ticket_system.model.UserRole;
import com.ticketsystem.it_ticket_system.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
        Optional<User> userOptional = userRepository.findById(id);

        return userOptional
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
        Optional<User> userOptional = userRepository.findByUsername(username);
        return userOptional
                .map(UserDTO::fromEntity)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));
    }

    @Transactional
    public UserDTO createUser(UserDTO userDTO) {
        User user = toEntity(userDTO);
        User savedUser = userRepository.save(user);
        return UserDTO.fromEntity(savedUser);
    }

    @Transactional
    public UserDTO updateUser(Long id, UserDTO userDTO) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        existingUser.setEmail(userDTO.getEmail());
        existingUser.setUsername(userDTO.getUsername());
        existingUser.setRole(UserRole.valueOf(userDTO.getRole()));

        User updatedUser = userRepository.save(existingUser);
        return UserDTO.fromEntity(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }


}
