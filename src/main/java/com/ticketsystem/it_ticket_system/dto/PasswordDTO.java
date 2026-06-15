package com.ticketsystem.it_ticket_system.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PasswordDTO {
    private String password;
}
