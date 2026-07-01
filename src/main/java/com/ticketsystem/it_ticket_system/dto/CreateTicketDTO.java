package com.ticketsystem.it_ticket_system.dto;

import com.ticketsystem.it_ticket_system.model.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateTicketDTO {
    @NotBlank(message = "Title must not be blank")
    @Size(max = 50, message = "Title must be at most 100 characters long")
    private String title;

    @NotBlank(message = "Description must not be blank")
    @Size(max = 255, message = "Description must be at most 255 characters long")
    private String description;

    @NotNull(message = "Category ID must not be null")
    private Long categoryId;
}
