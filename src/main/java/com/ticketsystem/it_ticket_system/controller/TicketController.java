package com.ticketsystem.it_ticket_system.controller;

import com.ticketsystem.it_ticket_system.dto.TicketDTO;
import com.ticketsystem.it_ticket_system.service.TicketService;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE','TECHNICIAN')")
@RequestMapping("/api/tickets")
public class TicketController {
    private final TicketService ticketService;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "title", "status", "createdAt", "updatedAt", "resolvedAt"
    );


    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE','TECHNICIAN')")
    public ResponseEntity<List<TicketDTO>> getAllTickets(
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String order) {

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new ValidationException("Invalid sort field: " + sortBy);
        }

        List<TicketDTO> tickets = ticketService.getAllTickets(sortBy, order);
        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE','TECHNICIAN')")
    public ResponseEntity<TicketDTO> getTicketById(@PathVariable Long id) {
        TicketDTO ticket = ticketService.getTicketById(id);
        return ResponseEntity.ok(ticket);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<TicketDTO> createTicket(@Valid @RequestBody TicketDTO ticketDTO) {
        TicketDTO createdTicket = ticketService.createTicket(ticketDTO);
        return new ResponseEntity<>(createdTicket, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE','TECHNICIAN')")
    public ResponseEntity<TicketDTO> updateTicket(@PathVariable Long id, @Valid @RequestBody TicketDTO ticketDTO) {
        TicketDTO updatedTicket = ticketService.updateTicket(id, ticketDTO);
        return ResponseEntity.ok(updatedTicket);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }
}
