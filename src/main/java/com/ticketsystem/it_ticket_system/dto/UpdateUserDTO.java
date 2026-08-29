package com.ticketsystem.it_ticket_system.dto;

import com.ticketsystem.it_ticket_system.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
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
    @Email(message = "Email is not valid")
    private String email;

    @Pattern(regexp = "^[a-zA-Z0-9_]{5,50}$", message = "Username must be 5-50 alphanumeric characters")
    private String username;
    private UserRole role;
    private Boolean accountNonExpired;
    private Boolean accountNonLocked;
    private Boolean credentialsNonExpired;
    private Boolean enabled;
}
