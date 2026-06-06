package com.ticketsystem.it_ticket_system.dto;

import com.ticketsystem.it_ticket_system.model.Ticket;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TicketDTO {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private String status;

    private CategoryDTO category;
    private UserDTO reporter;
    private UserDTO assignee;

    public static TicketDTO fromEntity(Ticket ticket) {
        return TicketDTO.builder()
                .id(ticket.getId())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .resolvedAt(ticket.getResolvedAt())
                .status(ticket.getStatus() != null ? ticket.getStatus().name() : null)
                .category(ticket.getCategory() != null ? CategoryDTO.fromEntity(ticket.getCategory()) : null)
                .reporter(ticket.getReporter() != null ? UserDTO.fromEntity(ticket.getReporter()) : null)
                .assignee(ticket.getAssignee() != null ? UserDTO.fromEntity(ticket.getAssignee()) : null)
                .build();
    }
}
