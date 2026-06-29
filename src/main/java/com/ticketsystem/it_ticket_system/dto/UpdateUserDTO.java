package com.ticketsystem.it_ticket_system.dto;

import com.ticketsystem.it_ticket_system.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserDTO {
    @Size(min = 5, max = 20, message = "Username must be between 5 and 20 characters")
    private String username;
    @Email
    private String email;
    private UserRole role;
    private Boolean accountNonExpired;
    private Boolean accountNonLocked;
    private Boolean credentialsNonExpired;
    private Boolean enabled;
}
