package com.ticketsystem.it_ticket_system.service;

import com.ticketsystem.it_ticket_system.dto.TicketDTO;
import com.ticketsystem.it_ticket_system.exception.CategoryNotFoundException;
import com.ticketsystem.it_ticket_system.exception.UserNotFoundException;
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

    public List<TicketDTO> getAllTickets() {
        return ticketRepository.findAll().stream()
                .map(TicketDTO::fromEntity)
                .toList();
    }

    @Transactional
    public TicketDTO createTicket(TicketDTO ticketDTO) {
        Ticket ticket = toEntity(ticketDTO);
        if (ticket.getStatus() == null) {
            ticket.setStatus(TicketStatus.NEW);
        }

        Ticket savedTicket = ticketRepository.save(ticket);
        return TicketDTO.fromEntity(savedTicket);
    }

    @Transactional
    public TicketDTO updateTicket(Long id, TicketDTO ticketDTO) {
        Ticket existingTicket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + id));

        existingTicket.setTitle(ticketDTO.getTitle());
        existingTicket.setDescription(ticketDTO.getDescription());

        if (ticketDTO.getStatus() != null) {
            TicketStatus newStatus = TicketStatus.valueOf(ticketDTO.getStatus());
            existingTicket.setStatus(newStatus);

            if (newStatus == TicketStatus.RESOLVED && existingTicket.getResolvedAt() == null) {
                existingTicket.setResolvedAt(LocalDateTime.now());
            }
        }

        if (ticketDTO.getCategory() != null) {
            Category category = categoryRepository.findById(ticketDTO.getCategory().getId())
                    .orElseThrow(() -> new CategoryNotFoundException("Category not found"));
            existingTicket.setCategory(category);
        }

        if (ticketDTO.getAssignee() != null) {
            User assignee = userRepository.findById(ticketDTO.getAssignee().getId())
                    .orElseThrow(() -> new UserNotFoundException("Assignee not found"));
            existingTicket.setAssignee(assignee);
        }

        Ticket updatedTicket = ticketRepository.save(existingTicket);
        return TicketDTO.fromEntity(updatedTicket);
    }

    @Transactional
    public void deleteTicket(Long id) {
        Ticket existingTicket = ticketRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
        ticketRepository.delete(existingTicket);
    }

    public List<TicketDTO> getAllTicketsByCreatedAtDesc() {
        return ticketRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream().map(TicketDTO::fromEntity).toList();
    }

    public List<TicketDTO> getAllTicketsByCreatedAtAsc() {
        return ticketRepository.findAll(Sort.by(Sort.Direction.ASC, "createdAt"))
                .stream().map(TicketDTO::fromEntity).toList();
    }

    public List<TicketDTO> getAllTicketsByUpdatedAtDesc() {
        return ticketRepository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"))
                .stream().map(TicketDTO::fromEntity).toList();
    }

    public List<TicketDTO> getAllTicketsByUpdatedAtAsc() {
        return ticketRepository.findAll(Sort.by(Sort.Direction.ASC, "updatedAt"))
                .stream().map(TicketDTO::fromEntity).toList();
    }

    public List<TicketDTO> getAllTicketsByResolvedAtDesc() {
        return ticketRepository.findAll(Sort.by(Sort.Direction.DESC, "resolvedAt"))
                .stream().map(TicketDTO::fromEntity).toList();
    }

    public List<TicketDTO> getAllTicketsByResolvedAtAsc() {
        return ticketRepository.findAll(Sort.by(Sort.Direction.ASC, "resolvedAt"))
                .stream().map(TicketDTO::fromEntity).toList();
    }

    public List<TicketDTO> getAllTicketsByStatusDesc() {
        return ticketRepository.findAll(Sort.by(Sort.Direction.DESC, "status"))
                .stream().map(TicketDTO::fromEntity).toList();
    }

    public List<TicketDTO> getAllTicketsByStatusAsc() {
        return ticketRepository.findAll(Sort.by(Sort.Direction.ASC, "status"))
                .stream().map(TicketDTO::fromEntity).toList();
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
}
