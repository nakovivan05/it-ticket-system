package com.ticketsystem.it_ticket_system.service;

import com.ticketsystem.it_ticket_system.dto.PasswordDTO;
import com.ticketsystem.it_ticket_system.dto.UpdateUserDTO;
import com.ticketsystem.it_ticket_system.dto.UserDTO;
import com.ticketsystem.it_ticket_system.exception.DuplicateEmailException;
import com.ticketsystem.it_ticket_system.exception.DuplicateUsernameException;
import com.ticketsystem.it_ticket_system.exception.UserNotFoundException;
import com.ticketsystem.it_ticket_system.exception.ValidationException;
import com.ticketsystem.it_ticket_system.model.EntityType;
import com.ticketsystem.it_ticket_system.model.Operation;
import com.ticketsystem.it_ticket_system.model.User;
import com.ticketsystem.it_ticket_system.repository.TicketRepository;
import com.ticketsystem.it_ticket_system.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private UserService userService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserById_WhenUserExists_ReturnsUserDTO() {
        Long userId = 1L;
        User testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testuser");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        UserDTO result = userService.getUserById(userId);

        assertEquals("testuser", result.getUsername());
        assertEquals(userId, result.getId());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserById_WhenUserNotFound_ThrowsException() {
        Long userId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserById(userId));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_ReturnsList() {
        User testUser1 = new User();
        User testUser2 = new User();
        testUser1.setId(1L);
        testUser2.setId(2L);

        when(userRepository.findAll()).thenReturn(List.of(testUser1, testUser2));

        List<UserDTO> result = userService.getAllUsers();

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserByUsername_WhenUserExists_ReturnsUserDTO() {
        String username = "testuser";
        User testUser = new User();
        testUser.setId(1L);
        testUser.setUsername(username);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        UserDTO result = userService.getUserByUsername(username);

        assertEquals(username, result.getUsername());
        assertEquals(1L, result.getId());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserByUsername_WhenUserNotFound_ThrowsException() {
        String username = "nonexistent";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserByUsername(username));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_WhenUserNotFound_ThrowsException() {
        Long userId = 999L;
        UpdateUserDTO userDTO = new UpdateUserDTO();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.updateUser(userId, userDTO));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_WhenDuplicateEmail_ThrowsException() {
        Long userId = 1L;
        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setEmail("old@email.com");

        UpdateUserDTO userDTO = new UpdateUserDTO();
        userDTO.setEmail("new@email.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmail("new@email.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> userService.updateUser(userId, userDTO));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_WhenDuplicateUsername_ThrowsException() {
        Long userId = 1L;
        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setUsername("oldusername");

        UpdateUserDTO userDTO = UpdateUserDTO.builder()
                .username("newusername")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByUsername("newusername")).thenReturn(true);

        assertThrows(DuplicateUsernameException.class, () -> userService.updateUser(userId, userDTO));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_WhenValidData_ReturnsUpdatedUserDTO() {
        Long userId = 1L;
        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setEmail("old@email.com");

        UpdateUserDTO userDTO = new UpdateUserDTO();
        userDTO.setEmail("new@email.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmail("new@email.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        UserDTO result = userService.updateUser(userId, userDTO);

        assertEquals("new@email.com", result.getEmail());

        verify(auditLogService).auditLog(
                EntityType.USER,
                Operation.UPDATE,
                "User updated: old@email.com",
                userId,
                "admin"
        );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_WhenUserNotFound_ThrowsException() {
        Long userId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.deleteUser(userId));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_WhenUserReferencedByTickets_ThrowsException() {
        Long userId = 1L;
        User existingUser = new User();
        existingUser.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(ticketRepository.existsByReporterId(userId)).thenReturn(true);

        assertThrows(ValidationException.class, () -> userService.deleteUser(userId));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_WhenValidUser_DeletesUser() {
        Long userId = 1L;
        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setUsername("testuser");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(ticketRepository.existsByReporterId(userId)).thenReturn(false);
        when(ticketRepository.existsByAssigneeId(userId)).thenReturn(false);

        userService.deleteUser(userId);

        verify(userRepository).delete(existingUser);

        verify(auditLogService).auditLog(
                EntityType.USER,
                Operation.DELETE,
                "User deleted: testuser",  
                userId,
                "admin"
        );
    }

    @Test
    @WithMockUser(username = "testuser")
    void updatePassword_WhenUserNotFound_ThrowsException() {
        Long userId = 999L;
        PasswordDTO passwordDTO = PasswordDTO.builder()
                .currentPassword("oldpassword")
                .password("newpassword")
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.updatePassword(userId, passwordDTO));
    }

    @Test
    @WithMockUser(username = "testuser")
    void updatePassword_WhenDifferentUser_ThrowsSecurityException() {
        Long userId = 2L;
        PasswordDTO passwordDTO = PasswordDTO.builder()
                .currentPassword("oldpassword")
                .password("newpassword")
                .build();

        User currentUser = new User();
        currentUser.setId(1L);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(currentUser));

        assertThrows(SecurityException.class, () -> userService.updatePassword(userId, passwordDTO));
    }

    @Test
    @WithMockUser(username = "testuser")
    void updatePassword_WhenCurrentPasswordIncorrect_ThrowsException() {
        Long userId = 1L;
        PasswordDTO passwordDTO = PasswordDTO.builder()
                .currentPassword("wrongpassword")
                .password("newpassword")
                .build();

        User currentUser = new User();
        currentUser.setId(userId);
        currentUser.setPassword("encodedpassword");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(currentUser));
        when(passwordEncoder.matches("wrongpassword", "encodedpassword")).thenReturn(false);

        assertThrows(ValidationException.class, () -> userService.updatePassword(userId, passwordDTO));
    }

    @Test
    @WithMockUser(username = "testuser")
    void updatePassword_WhenValidData_UpdatesPassword() {
        Long userId = 1L;
        PasswordDTO passwordDTO = PasswordDTO.builder()
                .currentPassword("oldpassword")
                .password("newpassword")
                .build();

        User currentUser = new User();
        currentUser.setId(userId);
        currentUser.setPassword("encodedoldpassword");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(currentUser));
        when(passwordEncoder.matches("oldpassword", "encodedoldpassword")).thenReturn(true);
        when(passwordEncoder.encode("newpassword")).thenReturn("encodednewpassword");

        userService.updatePassword(userId, passwordDTO);

        verify(userRepository).save(currentUser);

        verify(auditLogService).auditLog(
                EntityType.USER,
                Operation.UPDATE,
                "Password updated",
                userId,
                "testuser"
        );
    }
}
