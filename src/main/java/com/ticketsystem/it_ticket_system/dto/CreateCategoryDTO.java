package com.ticketsystem.it_ticket_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateCategoryDTO {
    @NotBlank(message = "Name cannot be blank")
    @Size(max = 50, message = "Name must be at most 50 characters long")
    private String name;
    @Size(max = 255, message = "Description must be at most 255 characters long")
    private String description;
}
