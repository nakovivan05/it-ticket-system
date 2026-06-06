package com.ticketsystem.it_ticket_system.dto;

import com.ticketsystem.it_ticket_system.model.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserDTO {
    private Long id;
    private String email;
    private String username;
    private LocalDateTime createdAt;
    private String role;

    public static UserDTO fromEntity(User user)
    {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .createdAt(user.getCreatedAt())
                .role(user.getRole().name())
                .build();
    }

}
