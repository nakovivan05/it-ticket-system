package com.ticketsystem.it_ticket_system.dto;

import com.ticketsystem.it_ticket_system.model.Comment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentDTO {
    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long ticketId;
    private Long userId;

    public static CommentDTO fromEntity(Comment comment)
    {
        return CommentDTO.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .ticketId(comment.getTicket().getId())
                .userId(comment.getUser().getId())
                .build();

    }
}
