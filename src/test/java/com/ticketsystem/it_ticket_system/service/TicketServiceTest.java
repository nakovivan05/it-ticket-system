package com.ticketsystem.it_ticket_system.service;

import com.ticketsystem.it_ticket_system.dto.CreateTicketDTO;
import com.ticketsystem.it_ticket_system.dto.TicketDTO;
import com.ticketsystem.it_ticket_system.dto.UpdateTicketDTO;
import com.ticketsystem.it_ticket_system.exception.CategoryNotFoundException;
import com.ticketsystem.it_ticket_system.exception.TicketNotFoundException;
import com.ticketsystem.it_ticket_system.exception.UserNotFoundException;
import com.ticketsystem.it_ticket_system.exception.ValidationException;
import com.ticketsystem.it_ticket_system.model.*;
import com.ticketsystem.it_ticket_system.repository.CategoryRepository;
import com.ticketsystem.it_ticket_system.repository.TicketRepository;
import com.ticketsystem.it_ticket_system.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

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
public class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private TicketService ticketService;

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
    void getTicketById_WhenTicketExists_ReturnsTicketDTO() {
        Long ticketId = 1L;
        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setTitle("Test Ticket");

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        TicketDTO result = ticketService.getTicketById(ticketId);

        assertEquals(ticketId, result.getId());
        assertEquals("Test Ticket", result.getTitle());
    }

    @Test
    void getTicketById_WhenTicketNotFound_ThrowsException() {
        Long ticketId = 999L;

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () -> ticketService.getTicketById(ticketId));
    }

    @Test
    void getTicketById_WhenEmployeeViewsOwnTicket_ReturnsTicketDTO() {
        setupSecurityContext("employee", "EMPLOYEE");
        Long ticketId = 1L;
        Ticket ticket = new Ticket();
        ticket.setId(ticketId);

        User reporter = new User();
        reporter.setUsername("employee");
        reporter.setRole(UserRole.EMPLOYEE);
        ticket.setReporter(reporter);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        TicketDTO result = ticketService.getTicketById(ticketId);

        assertEquals(ticketId, result.getId());
    }

    @Test
    void getTicketById_WhenEmployeeViewsOthersTicket_ThrowsSecurityException() {
        setupSecurityContext("employee", "EMPLOYEE");
        Long ticketId = 1L;
        Ticket ticket = new Ticket();
        ticket.setId(ticketId);

        User reporter = new User();
        reporter.setUsername("otheruser");
        reporter.setRole(UserRole.EMPLOYEE);
        ticket.setReporter(reporter);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThrows(SecurityException.class, () -> ticketService.getTicketById(ticketId));
    }

    @Test
    void getAllTickets_WhenAdmin_ReturnsAllTickets() {
        Ticket ticket1 = new Ticket();
        ticket1.setId(1L);

        Ticket ticket2 = new Ticket();
        ticket2.setId(2L);

        when(ticketRepository.findAll(any(Sort.class))).thenReturn(List.of(ticket1, ticket2));

        List<TicketDTO> result = ticketService.getAllTickets("createdAt", "desc");

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
    }

    @Test
    void getAllTickets_WhenEmployee_ReturnsOwnTickets() {
        setupSecurityContext("employee", "EMPLOYEE");
        Ticket ticket1 = new Ticket();
        ticket1.setId(1L);

        when(ticketRepository.findByReporterUsername(eq("employee"), any(Sort.class))).thenReturn(List.of(ticket1));

        List<TicketDTO> result = ticketService.getAllTickets("createdAt", "desc");

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void createTicket_WhenValidData_ReturnsTicketDTO() {
        setupSecurityContext("employee", "EMPLOYEE");
        CreateTicketDTO dto = CreateTicketDTO.builder()
                .title("New Ticket")
                .description("Description")
                .categoryId(1L)
                .build();

        Category category = new Category();
        category.setId(1L);

        User reporter = new User();
        reporter.setUsername("employee");
        reporter.setRole(UserRole.EMPLOYEE);

        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setTitle("New Ticket");
        ticket.setStatus(TicketStatus.NEW);
        ticket.setReporter(reporter);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(userRepository.findByUsername("employee")).thenReturn(Optional.of(reporter));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);

        TicketDTO result = ticketService.createTicket(dto);

        assertEquals("New Ticket", result.getTitle());
        assertEquals("NEW", result.getStatus());
        verify(auditLogService).auditLog(EntityType.TICKET, Operation.CREATE, "Ticket created", 1L, "employee");
    }

    @Test
    void createTicket_WhenCategoryNotFound_ThrowsException() {
        setupSecurityContext("employee", "EMPLOYEE");
        CreateTicketDTO dto = CreateTicketDTO.builder()
                .title("New Ticket")
                .categoryId(999L)
                .build();

        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> ticketService.createTicket(dto));
    }

    @Test
    void updateTicket_WhenAdminUpdatesAllFields_ReturnsUpdatedTicketDTO() {
        Long ticketId = 1L;
        UpdateTicketDTO dto = UpdateTicketDTO.builder()
                .title("Updated Title")
                .description("Updated Description")
                .assigneeId(2L)
                .categoryId(3L)
                .status(TicketStatus.ASSIGNED)
                .build();

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setTitle("Old Title");
        ticket.setStatus(TicketStatus.NEW);

        Category category = new Category();
        category.setId(3L);

        User assignee = new User();
        assignee.setId(2L);
        assignee.setRole(UserRole.TECHNICIAN);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);

        TicketDTO result = ticketService.updateTicket(ticketId, dto);

        assertEquals("Updated Title", result.getTitle());
        verify(auditLogService).auditLog(EntityType.TICKET, Operation.UPDATE, "Ticket updated by: admin", ticketId, "admin");
    }

    @Test
    void updateTicket_WhenEmployeeTriesToAssign_ThrowsException() {
        setupSecurityContext("employee", "EMPLOYEE");
        Long ticketId = 1L;
        UpdateTicketDTO dto = UpdateTicketDTO.builder()
                .assigneeId(2L)
                .build();

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);

        User reporter = new User();
        reporter.setUsername("employee");
        reporter.setRole(UserRole.EMPLOYEE);
        ticket.setReporter(reporter);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThrows(SecurityException.class, () -> ticketService.updateTicket(ticketId, dto));
    }

    @Test
    void updateTicket_WhenEmployeeTriesToChangeStatus_ThrowsException() {
        setupSecurityContext("employee", "EMPLOYEE");
        Long ticketId = 1L;
        UpdateTicketDTO dto = UpdateTicketDTO.builder()
                .status(TicketStatus.IN_PROGRESS)
                .build();

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);

        User reporter = new User();
        reporter.setUsername("employee");
        reporter.setRole(UserRole.EMPLOYEE);
        ticket.setReporter(reporter);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThrows(SecurityException.class, () -> ticketService.updateTicket(ticketId, dto));
    }

    @Test
    void updateTicket_WhenTechnicianTriesToChangeTitle_ThrowsException() {
        setupSecurityContext("technician", "TECHNICIAN");
        Long ticketId = 1L;
        UpdateTicketDTO dto = UpdateTicketDTO.builder()
                .title("New Title")
                .build();

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);

        User technician = new User();
        technician.setUsername("technician");
        technician.setRole(UserRole.TECHNICIAN);
        ticket.setAssignee(technician);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThrows(SecurityException.class, () -> ticketService.updateTicket(ticketId, dto));
    }

    @Test
    void updateTicket_WhenTechnicianTriesToChangeDescription_ThrowsException() {
        setupSecurityContext("technician", "TECHNICIAN");
        Long ticketId = 1L;
        UpdateTicketDTO dto = UpdateTicketDTO.builder()
                .description("New Description")
                .build();

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);

        User technician = new User();
        technician.setUsername("technician");
        technician.setRole(UserRole.TECHNICIAN);
        ticket.setAssignee(technician);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThrows(SecurityException.class, () -> ticketService.updateTicket(ticketId, dto));
    }

    @Test
    void updateTicket_WhenTechnicianUpdatesOthersTicket_ThrowsSecurityException() {
        setupSecurityContext("technician", "TECHNICIAN");
        Long ticketId = 1L;
        UpdateTicketDTO dto = UpdateTicketDTO.builder()
                .status(TicketStatus.IN_PROGRESS)
                .build();

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);

        User otherTechnician = new User();
        otherTechnician.setUsername("othertechnician");
        otherTechnician.setRole(UserRole.TECHNICIAN);
        ticket.setAssignee(otherTechnician);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThrows(SecurityException.class, () -> ticketService.updateTicket(ticketId, dto));
    }

    @Test
    void updateTicket_WhenAssigneeNotTechnicianOrAdmin_ThrowsException() {
        Long ticketId = 1L;
        UpdateTicketDTO dto = UpdateTicketDTO.builder()
                .assigneeId(2L)
                .build();

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);

        User employee = new User();
        employee.setId(2L);
        employee.setRole(UserRole.EMPLOYEE);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(2L)).thenReturn(Optional.of(employee));

        assertThrows(SecurityException.class, () -> ticketService.updateTicket(ticketId, dto));
    }

    @Test
    void updateTicket_WhenInvalidStatusTransition_ThrowsException() {
        Long ticketId = 1L;
        UpdateTicketDTO dto = UpdateTicketDTO.builder()
                .status(TicketStatus.RESOLVED)
                .build();

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setStatus(TicketStatus.NEW);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThrows(IllegalStateException.class, () -> ticketService.updateTicket(ticketId, dto));
    }

    @Test
    void updateTicket_WhenStatusTransitionRequiresAssigneeButNoneProvided_ThrowsException() {
        Long ticketId = 1L;
        UpdateTicketDTO dto = UpdateTicketDTO.builder()
                .status(TicketStatus.ASSIGNED)
                .build();

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setStatus(TicketStatus.NEW);
        ticket.setAssignee(null);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThrows(IllegalStateException.class, () -> ticketService.updateTicket(ticketId, dto));
    }

    @Test
    void deleteTicket_WhenEmployeeDeletesOwnTicket_DeletesTicket() {
        setupSecurityContext("employee", "EMPLOYEE");
        Long ticketId = 1L;
        Ticket ticket = new Ticket();
        ticket.setId(ticketId);

        User reporter = new User();
        reporter.setUsername("employee");
        reporter.setRole(UserRole.EMPLOYEE);
        ticket.setReporter(reporter);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        ticketService.deleteTicket(ticketId);

        verify(ticketRepository).delete(ticket);
        verify(auditLogService).auditLog(EntityType.TICKET, Operation.DELETE, "Ticket deleted", ticketId, "employee");
    }

    @Test
    void deleteTicket_WhenEmployeeDeletesOthersTicket_ThrowsSecurityException() {
        setupSecurityContext("employee", "EMPLOYEE");
        Long ticketId = 1L;
        Ticket ticket = new Ticket();
        ticket.setId(ticketId);

        User reporter = new User();
        reporter.setUsername("otheruser");
        reporter.setRole(UserRole.EMPLOYEE);
        ticket.setReporter(reporter);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThrows(SecurityException.class, () -> ticketService.deleteTicket(ticketId));
    }

    @Test
    void deleteTicket_WhenAdminDeletesAnyTicket_DeletesTicket() {
        Long ticketId = 1L;
        Ticket ticket = new Ticket();
        ticket.setId(ticketId);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        ticketService.deleteTicket(ticketId);

        verify(ticketRepository).delete(ticket);
        verify(auditLogService).auditLog(EntityType.TICKET, Operation.DELETE, "Ticket deleted", ticketId, "admin");
    }

    @Test
    void deleteTicket_WhenTicketNotFound_ThrowsException() {
        Long ticketId = 999L;

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () -> ticketService.deleteTicket(ticketId));
    }
}
