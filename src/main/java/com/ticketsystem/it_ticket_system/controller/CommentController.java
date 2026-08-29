package com.ticketsystem.it_ticket_system.controller;

import com.ticketsystem.it_ticket_system.dto.CommentDTO;
import com.ticketsystem.it_ticket_system.dto.CreateCommentDTO;
import com.ticketsystem.it_ticket_system.dto.UpdateCommentDTO;
import com.ticketsystem.it_ticket_system.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CommentDTO>> getAllComments() {
        List<CommentDTO> comments = commentService.getAllComments();
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'TECHNICIAN')")
    public ResponseEntity<CommentDTO> getCommentById(@PathVariable Long id) {
        CommentDTO comment = commentService.getCommentById(id);
        return ResponseEntity.ok(comment);
    }

    @PostMapping("/{ticketId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'TECHNICIAN')")
    public ResponseEntity<CommentDTO> createComment(@PathVariable Long ticketId, @Valid @RequestBody CreateCommentDTO commentDTO) {
        CommentDTO createdComment = commentService.createComment(ticketId,commentDTO);
        return new ResponseEntity<>(createdComment,HttpStatus.CREATED);
    }

    @PutMapping("/{ticketId}/{commentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'TECHNICIAN')")
    public ResponseEntity<CommentDTO> updateComment(@PathVariable Long ticketId,@PathVariable Long commentId, @Valid @RequestBody UpdateCommentDTO commentDTO) {
        CommentDTO updatedComment = commentService.updateComment(ticketId, commentId, commentDTO);
        return ResponseEntity.ok(updatedComment);
    }

    @DeleteMapping("/{commentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'TECHNICIAN')")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}
