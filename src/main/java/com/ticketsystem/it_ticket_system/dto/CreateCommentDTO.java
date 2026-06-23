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
public class CreateCommentDTO {
    @NotBlank(message = "Content cannot be blank")
    @Size(min=1,max=1000,message = "Content must be between 1 and 1000 characters")
    private String content;
}
