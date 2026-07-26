package com.ticketsystem.it_ticket_system.service;

import com.ticketsystem.it_ticket_system.dto.CommentDTO;
import com.ticketsystem.it_ticket_system.dto.CreateCommentDTO;
import com.ticketsystem.it_ticket_system.dto.UpdateCommentDTO;
import com.ticketsystem.it_ticket_system.exception.CommentNotFoundException;
import com.ticketsystem.it_ticket_system.exception.TicketNotFoundException;
import com.ticketsystem.it_ticket_system.exception.UserNotFoundException;
import com.ticketsystem.it_ticket_system.model.*;
import com.ticketsystem.it_ticket_system.repository.CommentRepository;
import com.ticketsystem.it_ticket_system.repository.TicketRepository;
import com.ticketsystem.it_ticket_system.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public CommentService(CommentRepository commentRepository, TicketRepository ticketRepository, UserRepository userRepository, AuditLogService auditLogService) {
        this.commentRepository = commentRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<CommentDTO> getAllComments()
    {
        return commentRepository.findAll().stream()
                .map(CommentDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE','TECHNICIAN')")
    public CommentDTO getCommentById(Long id)
    {
        String user = getCurrentUser();
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new CommentNotFoundException("Comment not found with id: " + id));
        User currentUser = userRepository.findByUsername(user)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + user));
        boolean isAssignee = comment.getTicket().getAssignee() != null && currentUser.equals(comment.getTicket().getAssignee());

        if(!currentUser.equals(comment.getTicket().getReporter())&&!isAssignee&&!hasRole("ADMIN"))
        {
            throw new SecurityException("User is not authorized to view this comment");
        }
        return CommentDTO.fromEntity(comment);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'TECHNICIAN')")
    public List<CommentDTO> getCommentsByTicketId(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + ticketId));

        String user = getCurrentUser();
        User currentUser = userRepository.findByUsername(user)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + user));
        boolean isAssignee = ticket.getAssignee() != null && currentUser.equals(ticket.getAssignee());

        if(!currentUser.equals(ticket.getReporter())&&!isAssignee&&!hasRole("ADMIN"))
        {
            throw new SecurityException("User is not authorized to view comments for this ticket");
        }

        return commentRepository.findByTicket(ticket)
                .stream()
                .map(CommentDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'TECHNICIAN')")
    public CommentDTO createComment(Long ticketId, CreateCommentDTO createCommentDTO)
    {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + ticketId));
        String user = getCurrentUser();
        User currentUser = userRepository.findByUsername(user)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + user));
        boolean isAssignee = ticket.getAssignee() != null && currentUser.equals(ticket.getAssignee());
        if(!currentUser.equals(ticket.getReporter())&&!isAssignee&&!hasRole("ADMIN"))
        {
            throw new SecurityException("User is not authorized to create comments for this ticket");
        }
        Comment comment = Comment.builder()
                .content(createCommentDTO.getContent())
                .ticket(ticket)
                .user(currentUser)
                .build();
        Comment savedComment = commentRepository.save(comment);
        auditLogService.auditLog(EntityType.COMMENT, Operation.CREATE, "Comment created", savedComment.getId(), getCurrentUser());
        return CommentDTO.fromEntity(savedComment);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'TECHNICIAN')")
    public CommentDTO updateComment(Long ticketId, Long commentId, UpdateCommentDTO updateCommentDTO)
    {
        Ticket ticket = ticketRepository
                .findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + ticketId));

        Comment comment = commentRepository
                .findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException("Comment not found with id: " + commentId));

        if(!ticket.equals(comment.getTicket())) {
            throw new SecurityException("Comment does not belong to this ticket");
        }

        String user = getCurrentUser();
        User currentUser = userRepository.findByUsername(user)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + user));

        if (!currentUser.equals(comment.getUser()) && !hasRole("ADMIN")) {
            throw new SecurityException("User is not authorized to update this comment");
        }
        comment.setContent(updateCommentDTO.getContent());
        Comment savedComment = commentRepository.save(comment);
        auditLogService.auditLog(EntityType.COMMENT, Operation.UPDATE, "Comment updated by: " + currentUser.getUsername(), savedComment.getId(), getCurrentUser());
        return CommentDTO.fromEntity(savedComment);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'TECHNICIAN')")
    public void deleteComment(Long commentId)
    {
        Comment comment = commentRepository
                .findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException("Comment not found with id: " + commentId));

        String user = getCurrentUser();
        User currentUser = userRepository.findByUsername(user)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + user));
        if (!currentUser.equals(comment.getUser()) && !hasRole("ADMIN")) {
            throw new SecurityException("User is not authorized to delete this comment");
        }
        commentRepository.delete(comment);
        auditLogService.auditLog(EntityType.COMMENT, Operation.DELETE, "Comment deleted by: " + currentUser.getUsername(), commentId, getCurrentUser());
    }

    public String getCurrentUser()
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return username;
    }

    public Boolean hasRole(String role)
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }

}
