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
import com.ticketsystem.it_ticket_system.model.UserRole;
import com.ticketsystem.it_ticket_system.repository.TicketRepository;
import com.ticketsystem.it_ticket_system.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
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

    @BeforeEach
    void setUp() {
        setupSecurityContext("admin", "ADMIN");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setupSecurityContext(String username, String role) {
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        lenient().when(authentication.getName()).thenReturn(username);

        Collection<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + role)
        );
        lenient().when(authentication.getAuthorities()).thenReturn((Collection) authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void getUserById_WhenUserExists_ReturnsUserDTO() {
        Long userId = 1L;
        User testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testuser");
        testUser.setRole(UserRole.EMPLOYEE);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        UserDTO result = userService.getUserById(userId);

        assertEquals("testuser", result.getUsername());
        assertEquals(userId, result.getId());
    }

    @Test
    void getUserById_WhenUserNotFound_ThrowsException() {
        Long userId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserById(userId));
    }

    @Test
    void getAllUsers_ReturnsList() {
        User testUser1 = new User();
        User testUser2 = new User();
        testUser1.setId(1L);
        testUser1.setRole(UserRole.EMPLOYEE);
        testUser2.setId(2L);
        testUser2.setRole(UserRole.TECHNICIAN);

        when(userRepository.findAll()).thenReturn(List.of(testUser1, testUser2));

        List<UserDTO> result = userService.getAllUsers();

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
    }

    @Test
    void getUserByUsername_WhenUserExists_ReturnsUserDTO() {
        String username = "testuser";
        User testUser = new User();
        testUser.setId(1L);
        testUser.setUsername(username);
        testUser.setRole(UserRole.EMPLOYEE);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        UserDTO result = userService.getUserByUsername(username);

        assertEquals(username, result.getUsername());
        assertEquals(1L, result.getId());
    }

    @Test
    void getUserByUsername_WhenUserNotFound_ThrowsException() {
        String username = "nonexistent";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserByUsername(username));
    }

    @Test
    void updateUser_WhenUserNotFound_ThrowsException() {
        Long userId = 999L;
        UpdateUserDTO userDTO = new UpdateUserDTO();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.updateUser(userId, userDTO));
    }

    @Test
    void updateUser_WhenDuplicateEmail_ThrowsException() {
        Long userId = 1L;
        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setEmail("old@email.com");
        existingUser.setRole(UserRole.EMPLOYEE);

        UpdateUserDTO userDTO = new UpdateUserDTO();
        userDTO.setEmail("new@email.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmail("new@email.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> userService.updateUser(userId, userDTO));
    }

    @Test
    void updateUser_WhenDuplicateUsername_ThrowsException() {
        Long userId = 1L;
        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setUsername("oldusername");
        existingUser.setRole(UserRole.EMPLOYEE);

        UpdateUserDTO userDTO = UpdateUserDTO.builder()
                .username("newusername")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByUsername("newusername")).thenReturn(true);

        assertThrows(DuplicateUsernameException.class, () -> userService.updateUser(userId, userDTO));
    }

    @Test
    void updateUser_WhenValidData_ReturnsUpdatedUserDTO() {
        Long userId = 1L;
        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setUsername("oldusername");
        existingUser.setEmail("old@email.com");
        existingUser.setRole(UserRole.EMPLOYEE);

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
                "User updated: oldusername",
                userId,
                "admin"
        );
    }

    @Test
    void deleteUser_WhenUserNotFound_ThrowsException() {
        Long userId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.deleteUser(userId));
    }

    @Test
    void deleteUser_WhenUserReferencedByTickets_ThrowsException() {
        Long userId = 1L;
        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setRole(UserRole.EMPLOYEE);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(ticketRepository.existsByReporterId(userId)).thenReturn(true);

        assertThrows(ValidationException.class, () -> userService.deleteUser(userId));
    }

    @Test
    void deleteUser_WhenValidUser_DeletesUser() {
        Long userId = 1L;
        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setUsername("testuser");
        existingUser.setRole(UserRole.EMPLOYEE);

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
    void updatePassword_WhenUserNotFound_ThrowsException() {
        setupSecurityContext("testuser", "EMPLOYEE");
        Long userId = 999L;
        PasswordDTO passwordDTO = PasswordDTO.builder()
                .currentPassword("oldpassword")
                .password("newpassword")
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.updatePassword(userId, passwordDTO));
    }

    @Test
    void updatePassword_WhenDifferentUser_ThrowsSecurityException() {
        setupSecurityContext("testuser", "EMPLOYEE");
        Long userId = 2L;
        PasswordDTO passwordDTO = PasswordDTO.builder()
                .currentPassword("oldpassword")
                .password("newpassword")
                .build();

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setRole(UserRole.EMPLOYEE);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(currentUser));

        assertThrows(SecurityException.class, () -> userService.updatePassword(userId, passwordDTO));
    }

    @Test
    void updatePassword_WhenCurrentPasswordIncorrect_ThrowsException() {
        setupSecurityContext("testuser", "EMPLOYEE");
        Long userId = 1L;
        PasswordDTO passwordDTO = PasswordDTO.builder()
                .currentPassword("wrongpassword")
                .password("newpassword")
                .build();

        User currentUser = new User();
        currentUser.setId(userId);
        currentUser.setPassword("encodedpassword");
        currentUser.setRole(UserRole.EMPLOYEE);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(currentUser));
        when(passwordEncoder.matches("wrongpassword", "encodedpassword")).thenReturn(false);

        assertThrows(ValidationException.class, () -> userService.updatePassword(userId, passwordDTO));
    }

    @Test
    void updatePassword_WhenValidData_UpdatesPassword() {
        setupSecurityContext("testuser", "EMPLOYEE");
        Long userId = 1L;
        PasswordDTO passwordDTO = PasswordDTO.builder()
                .currentPassword("oldpassword")
                .password("newpassword")
                .build();

        User currentUser = new User();
        currentUser.setId(userId);
        currentUser.setPassword("encodedoldpassword");
        currentUser.setRole(UserRole.EMPLOYEE);

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
