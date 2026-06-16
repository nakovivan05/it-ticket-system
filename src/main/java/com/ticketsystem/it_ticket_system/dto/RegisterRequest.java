package com.ticketsystem.it_ticket_system.dto;

import com.ticketsystem.it_ticket_system.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @NotNull
    private String username;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 6, max = 20)
    private String password;

    @Email
    @NotBlank(message = "Email cannot be blank")
    private String email;
}
