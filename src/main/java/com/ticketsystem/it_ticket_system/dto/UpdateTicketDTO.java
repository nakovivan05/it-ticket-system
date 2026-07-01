package com.ticketsystem.it_ticket_system.dto;

import com.ticketsystem.it_ticket_system.model.Category;
import com.ticketsystem.it_ticket_system.model.TicketStatus;
import com.ticketsystem.it_ticket_system.model.User;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateTicketDTO {
    @Size(max = 50, message = "Title must be at most 50 characters long")
    private String title;
    @Size(max = 255, message = "Description must be at most 255 characters long")
    private String description;
    private Long categoryId;
    private TicketStatus status;
    private Long assigneeId;
}
