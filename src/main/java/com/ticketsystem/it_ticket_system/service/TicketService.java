package com.ticketsystem.it_ticket_system.service;

import com.ticketsystem.it_ticket_system.dto.TicketDTO;
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
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'TECHNICIAN')")
public class TicketService {

    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public TicketService(TicketRepository ticketRepository,
                         CategoryRepository categoryRepository,
                         UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    public TicketDTO getTicketById(Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + id));

        if (hasRole(authentication, "EMPLOYEE")) {
            if (!ticket.getReporter().getUsername().equals(currentUser)) {
                throw new SecurityException("You can only view your own tickets");
            }
        }

        return TicketDTO.fromEntity(ticket);
    }

    public List<TicketDTO> getAllTickets(String sortBy, String order) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();

        Sort.Direction direction = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);

        List<Ticket> tickets;

        if (hasRole(authentication, "EMPLOYEE")) {
            tickets = ticketRepository.findByReporterUsername(currentUser, sort);
        } else {
            tickets = ticketRepository.findAll(sort);
        }

        return tickets.stream()
                .map(TicketDTO::fromEntity)
                .toList();
    }

    @Transactional
    public TicketDTO createTicket(TicketDTO ticketDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();

        if (ticketDTO.getTitle() == null || ticketDTO.getTitle().trim().isEmpty()) {
            throw new ValidationException("Title is required");
        }
        if (ticketDTO.getDescription() == null || ticketDTO.getDescription().trim().isEmpty()) {
            throw new ValidationException("Description is required");
        }
        if (ticketDTO.getCategory() == null || ticketDTO.getCategory().getId() == null) {
            throw new ValidationException("Category is required");
        }

        Ticket ticket = toEntity(ticketDTO);

        if (hasRole(authentication, "EMPLOYEE")) {
            User currentUserEntity = userRepository.findByUsername(currentUser)
                    .orElseThrow(() -> new UserNotFoundException("Current user not found"));
            ticket.setReporter(currentUserEntity);
        }

        if (ticket.getStatus() == null) {
            ticket.setStatus(TicketStatus.NEW);
        }
        if ((ticket.getStatus() == TicketStatus.ASSIGNED ||
                ticket.getStatus() == TicketStatus.IN_PROGRESS ||
                ticket.getStatus() == TicketStatus.RESOLVED) &&
                ticket.getAssignee() == null) {
            throw new IllegalStateException("Assignee is required for status: " + ticket.getStatus());
        }

        Ticket savedTicket = ticketRepository.save(ticket);
        return TicketDTO.fromEntity(savedTicket);
    }

    @Transactional
    public TicketDTO updateTicket(Long id, TicketDTO ticketDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();

        Ticket existingTicket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + id));

        if (hasRole(authentication, "EMPLOYEE")) {
            if (!existingTicket.getReporter().getUsername().equals(currentUser)) {
                throw new SecurityException("You can only update your own tickets");
            }
        }

        if (ticketDTO.getTitle() != null && ticketDTO.getTitle().trim().isEmpty()) {
            throw new ValidationException("Title cannot be empty");
        }
        if (ticketDTO.getDescription() != null && ticketDTO.getDescription().trim().isEmpty()) {
            throw new ValidationException("Description cannot be empty");
        }

        if(ticketDTO.getTitle()!=null) {
            existingTicket.setTitle(ticketDTO.getTitle());
        }

        if(ticketDTO.getDescription()!=null) {
            existingTicket.setDescription(ticketDTO.getDescription());
        }

        if (ticketDTO.getAssignee() != null) {
            User assignee = userRepository.findById(ticketDTO.getAssignee().getId())
                    .orElseThrow(() -> new UserNotFoundException("Assignee not found"));
            if (assignee.getRole() != UserRole.TECHNICIAN && assignee.getRole() != UserRole.ADMIN) {
                throw new ValidationException("Assignee must be a TECHNICIAN or ADMIN");
            }
            existingTicket.setAssignee(assignee);
        }

        if (ticketDTO.getCategory() != null) {
            Category category = categoryRepository.findById(ticketDTO.getCategory().getId())
                    .orElseThrow(() -> new CategoryNotFoundException("Category not found"));
            existingTicket.setCategory(category);
        }

        if (ticketDTO.getStatus() != null) {
            TicketStatus newStatus = TicketStatus.valueOf(ticketDTO.getStatus());
            validateStatusTransition(existingTicket.getStatus(), newStatus, existingTicket.getAssignee());
            existingTicket.setStatus(newStatus);
            if (newStatus == TicketStatus.RESOLVED) {
                existingTicket.setResolvedAt(LocalDateTime.now());
            } else if (newStatus == TicketStatus.IN_PROGRESS && existingTicket.getResolvedAt() != null) {
                existingTicket.setResolvedAt(null);
            }
        } else if (existingTicket.getStatus() == TicketStatus.NEW && existingTicket.getAssignee() != null) {
            existingTicket.setStatus(TicketStatus.ASSIGNED);
        }

        Ticket updatedTicket = ticketRepository.save(existingTicket);
        return TicketDTO.fromEntity(updatedTicket);
    }

    @Transactional
    public void deleteTicket(Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();

        Ticket existingTicket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + id));
        
        if (hasRole(authentication, "EMPLOYEE")) {
            if (!existingTicket.getReporter().getUsername().equals(currentUser)) {
                throw new SecurityException("You can only delete your own tickets");
            }
        }

        ticketRepository.delete(existingTicket);
    }

    private Ticket toEntity(TicketDTO dto) {
        Ticket ticket = new Ticket();
        ticket.setTitle(dto.getTitle());
        ticket.setDescription(dto.getDescription());

        if (dto.getStatus() != null) {
            ticket.setStatus(TicketStatus.valueOf(dto.getStatus()));
        }

        if (dto.getCategory() != null && dto.getCategory().getId() != null) {
            Category category = categoryRepository.findById(dto.getCategory().getId())
                    .orElseThrow(() -> new CategoryNotFoundException("Category not found with id: " + dto.getCategory().getId()));
            ticket.setCategory(category);
        }

        if (dto.getReporter() != null && dto.getReporter().getId() != null) {
            User reporter = userRepository.findById(dto.getReporter().getId())
                    .orElseThrow(() -> new UserNotFoundException("Reporter not found with id: " + dto.getReporter().getId()));
            ticket.setReporter(reporter);
        }

        if (dto.getAssignee() != null && dto.getAssignee().getId() != null) {
            User assignee = userRepository.findById(dto.getAssignee().getId())
                    .orElseThrow(() -> new UserNotFoundException("Assignee not found with id: " + dto.getAssignee().getId()));
            ticket.setAssignee(assignee);
        }

        return ticket;
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
    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }
}
