package com.ticketsystem.it_ticket_system.service;

import com.ticketsystem.it_ticket_system.dto.TicketDTO;
import com.ticketsystem.it_ticket_system.exception.CategoryNotFoundException;
import com.ticketsystem.it_ticket_system.exception.UserNotFoundException;
import com.ticketsystem.it_ticket_system.exception.ValidationException;
import com.ticketsystem.it_ticket_system.model.Category;
import com.ticketsystem.it_ticket_system.model.Ticket;
import com.ticketsystem.it_ticket_system.model.TicketStatus;
import com.ticketsystem.it_ticket_system.model.User;
import com.ticketsystem.it_ticket_system.repository.CategoryRepository;
import com.ticketsystem.it_ticket_system.repository.TicketRepository;
import com.ticketsystem.it_ticket_system.repository.UserRepository;
import com.ticketsystem.it_ticket_system.exception.TicketNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
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
        return ticketRepository.findById(id)
                .map(TicketDTO::fromEntity)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + id));
    }

    public List<TicketDTO> getAllTickets(String sortBy, String order) {
        Sort.Direction direction = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);

        return ticketRepository.findAll(sort)
                .stream()
                .map(TicketDTO::fromEntity)
                .toList();
    }

    @Transactional
    public TicketDTO createTicket(TicketDTO ticketDTO) {

        if (ticketDTO.getTitle() == null || ticketDTO.getTitle().trim().isEmpty()) {
            throw new ValidationException("Title is required");
        }
        if (ticketDTO.getDescription() == null || ticketDTO.getDescription().trim().isEmpty()) {
            throw new ValidationException("Description is required");
        }
        if (ticketDTO.getReporter() == null || ticketDTO.getReporter().getId() == null) {
            throw new ValidationException("Reporter is required");
        }
        if (ticketDTO.getCategory() == null || ticketDTO.getCategory().getId() == null) {
            throw new ValidationException("Category is required");
        }

        Ticket ticket = toEntity(ticketDTO);
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
        Ticket existingTicket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + id));

        if (ticketDTO.getTitle() != null && ticketDTO.getTitle().trim().isEmpty()) {
            throw new ValidationException("Title cannot be empty");
        }
        if (ticketDTO.getDescription() != null && ticketDTO.getDescription().trim().isEmpty()) {
            throw new ValidationException("Description cannot be empty");
        }

        if(ticketDTO.getTitle()!=null)
        {
            existingTicket.setTitle(ticketDTO.getTitle());
        }

        if(ticketDTO.getDescription()!=null)
        {
            existingTicket.setDescription(ticketDTO.getDescription());
        }

        if (ticketDTO.getAssignee() != null) {
            User assignee = userRepository.findById(ticketDTO.getAssignee().getId())
                    .orElseThrow(() -> new UserNotFoundException("Assignee not found"));
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
        Ticket existingTicket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + id));
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
}
