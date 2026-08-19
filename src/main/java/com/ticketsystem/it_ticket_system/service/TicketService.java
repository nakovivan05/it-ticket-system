package com.ticketsystem.it_ticket_system.service;

import com.ticketsystem.it_ticket_system.dto.CreateTicketDTO;
import com.ticketsystem.it_ticket_system.dto.TicketDTO;
import com.ticketsystem.it_ticket_system.dto.UpdateTicketDTO;
import com.ticketsystem.it_ticket_system.exception.CategoryNotFoundException;
import com.ticketsystem.it_ticket_system.exception.UserNotFoundException;
import com.ticketsystem.it_ticket_system.exception.ValidationException;
import com.ticketsystem.it_ticket_system.model.*;
import com.ticketsystem.it_ticket_system.repository.CategoryRepository;
import com.ticketsystem.it_ticket_system.repository.TicketRepository;
import com.ticketsystem.it_ticket_system.repository.UserRepository;
import com.ticketsystem.it_ticket_system.exception.TicketNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public TicketService(TicketRepository ticketRepository,
                         CategoryRepository categoryRepository,
                         UserRepository userRepository,
                         AuditLogService auditLogService) {
        this.ticketRepository = ticketRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'TECHNICIAN')")
    public TicketDTO getTicketById(Long id) {
        String currentUser = getCurrentUser();

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + id));

        if (hasRole("EMPLOYEE")) {
            if (!ticket.getReporter().getUsername().equals(currentUser)) {
                throw new SecurityException("You can only view your own tickets");
            }
        }

        return TicketDTO.fromEntity(ticket);
    }
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'TECHNICIAN')")
    public List<TicketDTO> getAllTickets(String sortBy, String order) {
        String currentUser = getCurrentUser();

        Sort.Direction direction = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);

        List<Ticket> tickets;

        if (hasRole("EMPLOYEE")) {
            tickets = ticketRepository.findByReporterUsername(currentUser, sort);
        } else {
            tickets = ticketRepository.findAll(sort);
        }

        return tickets.stream()
                .map(TicketDTO::fromEntity)
                .toList();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public TicketDTO createTicket(CreateTicketDTO createTicketDTO) {
        String currentUser = getCurrentUser();

        Category category = categoryRepository.findById(createTicketDTO.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with id: " + createTicketDTO.getCategoryId()));

        Ticket ticket = Ticket.builder()
                .title(createTicketDTO.getTitle())
                .description(createTicketDTO.getDescription())
                .category(category)
                .status(TicketStatus.NEW)
                .build();

        User user = userRepository.findByUsername(currentUser)
                    .orElseThrow(() -> new UserNotFoundException("Current user not found"));
        ticket.setReporter(user);

        Ticket savedTicket = ticketRepository.save(ticket);
        auditLogService.auditLog(EntityType.TICKET, Operation.CREATE, "Ticket created", savedTicket.getId(), currentUser);
        return TicketDTO.fromEntity(savedTicket);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'TECHNICIAN')")
    public TicketDTO updateTicket(Long id, UpdateTicketDTO updateTicketDTO) {

        String currentUser = getCurrentUser();

        Ticket existingTicket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + id));

        if (hasRole("EMPLOYEE")) {
            if (!existingTicket.getReporter().getUsername().equals(currentUser)) {
                throw new SecurityException("You can only update your own tickets");
            }
            if(updateTicketDTO.getAssigneeId()!=null)
            {
                throw new SecurityException("EMPLOYEEs cannot assign tickets");
            }
            if(updateTicketDTO.getStatus()!=null)
            {
                throw new SecurityException("EMPLOYEEs cannot change ticket status");
            }
        }

        if (hasRole("TECHNICIAN")) {
            boolean isUnassigned = existingTicket.getAssignee() == null;
            boolean isAssignedToMe = existingTicket.getAssignee() != null &&
                    existingTicket.getAssignee().getUsername().equals(currentUser);

            if (!isUnassigned && !isAssignedToMe) {
                throw new SecurityException("You can only update unassigned tickets or tickets assigned to you");
            }
            if (updateTicketDTO.getTitle() != null) {
                throw new SecurityException("TECHNICIANs cannot change ticket title");
            }
            if (updateTicketDTO.getDescription() != null) {
                throw new SecurityException("TECHNICIANs cannot change ticket description");
            }
        }

        if (updateTicketDTO.getTitle() != null) {
            existingTicket.setTitle(updateTicketDTO.getTitle());
        }

        if (updateTicketDTO.getDescription() != null) {
            existingTicket.setDescription(updateTicketDTO.getDescription());
        }

        if (updateTicketDTO.getAssigneeId() != null) {
            User assignee = userRepository.findById(updateTicketDTO.getAssigneeId())
                    .orElseThrow(() -> new UserNotFoundException("Assignee not found"));
            if (assignee.getRole() != UserRole.TECHNICIAN && assignee.getRole() != UserRole.ADMIN) {
                throw new SecurityException("Assignee must be a TECHNICIAN or ADMIN");
            }
            existingTicket.setAssignee(assignee);
        }

        if (updateTicketDTO.getCategoryId() != null) {
            Category category = categoryRepository.findById(updateTicketDTO.getCategoryId())
                    .orElseThrow(() -> new CategoryNotFoundException("Category not found"));
            existingTicket.setCategory(category);
        }

        if (updateTicketDTO.getStatus() != null) {
            TicketStatus newStatus = updateTicketDTO.getStatus();
            validateStatusTransition(existingTicket.getStatus(), newStatus, existingTicket.getAssignee());
            existingTicket.setStatus(newStatus);
            if (newStatus == TicketStatus.RESOLVED) {
                existingTicket.setResolvedAt(LocalDateTime.now());
            } else if (newStatus == TicketStatus.IN_PROGRESS && existingTicket.getResolvedAt() != null) {
                existingTicket.setResolvedAt(null);
            }
        }


        Ticket updatedTicket = ticketRepository.save(existingTicket);
        auditLogService.auditLog(EntityType.TICKET, Operation.UPDATE, "Ticket updated by: " + currentUser, updatedTicket.getId(), currentUser);
        return TicketDTO.fromEntity(updatedTicket);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public void deleteTicket(Long id) {
        String currentUser = getCurrentUser();

        Ticket existingTicket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + id));
        
        if (hasRole("EMPLOYEE")) {
            if (!existingTicket.getReporter().getUsername().equals(currentUser)) {
                throw new SecurityException("You can only delete your own tickets");
            }
        }

        ticketRepository.delete(existingTicket);
        auditLogService.auditLog(EntityType.TICKET, Operation.DELETE, "Ticket deleted", existingTicket.getId(), currentUser);
    }

    private void validateStatusTransition(TicketStatus currentStatus, TicketStatus newStatus, User assignee) {
        if (currentStatus == newStatus) {
            return;
        }

        boolean isValid = switch (currentStatus) {
            case NEW -> newStatus == TicketStatus.ASSIGNED;
            case ASSIGNED -> newStatus == TicketStatus.IN_PROGRESS;
            case IN_PROGRESS -> newStatus == TicketStatus.RESOLVED;
            case RESOLVED -> newStatus == TicketStatus.CLOSED || newStatus == TicketStatus.IN_PROGRESS;
            case CLOSED -> false;
        };

        if (!isValid) {
            throw new IllegalStateException(
                    "Invalid status transition from " + currentStatus + " to " + newStatus
            );
        }

        if ((newStatus == TicketStatus.ASSIGNED ||
                newStatus == TicketStatus.IN_PROGRESS ||
                newStatus == TicketStatus.RESOLVED) &&
                assignee == null) {
            throw new IllegalStateException("Assignee is required for status: " + newStatus);
        }
    }
    private boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }
    private String getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
