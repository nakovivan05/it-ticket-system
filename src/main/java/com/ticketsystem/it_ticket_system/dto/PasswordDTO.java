package com.ticketsystem.it_ticket_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PasswordDTO {
    @NotBlank(message = "Password cannot be blank")
    @Size(min = 6, max = 20)
    private String password;

    @NotBlank(message = "Current password cannot be blank")
    @Size(min = 6, max = 20)
    private String currentPassword;
}
