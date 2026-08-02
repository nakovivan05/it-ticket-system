package com.ticketsystem.it_ticket_system.service;

import com.ticketsystem.it_ticket_system.dto.CommentDTO;
import com.ticketsystem.it_ticket_system.dto.CreateCommentDTO;
import com.ticketsystem.it_ticket_system.dto.UpdateCommentDTO;
import com.ticketsystem.it_ticket_system.exception.CommentNotFoundException;
import com.ticketsystem.it_ticket_system.exception.TicketNotFoundException;
import com.ticketsystem.it_ticket_system.exception.UserNotFoundException;
import com.ticketsystem.it_ticket_system.model.Comment;
import com.ticketsystem.it_ticket_system.model.EntityType;
import com.ticketsystem.it_ticket_system.model.Operation;
import com.ticketsystem.it_ticket_system.model.Ticket;
import com.ticketsystem.it_ticket_system.model.User;
import com.ticketsystem.it_ticket_system.repository.CommentRepository;
import com.ticketsystem.it_ticket_system.repository.TicketRepository;
import com.ticketsystem.it_ticket_system.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private CommentService commentService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllComments_ReturnsList() {
        Comment comment1 = new Comment();
        comment1.setId(1L);
        comment1.setContent("First comment");

        Comment comment2 = new Comment();
        comment2.setId(2L);
        comment2.setContent("Second comment");

        when(commentRepository.findAll()).thenReturn(List.of(comment1, comment2));

        List<CommentDTO> result = commentService.getAllComments();

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("First comment", result.get(0).getContent());
        assertEquals(2L, result.get(1).getId());
        assertEquals("Second comment", result.get(1).getContent());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void getCommentById_WhenCommentExists_ReturnsCommentDTO() {
        Long commentId = 1L;
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setContent("Test comment");

        Ticket ticket = new Ticket();
        ticket.setId(1L);
        comment.setTicket(ticket);

        User currentUser = new User();
        currentUser.setUsername("testuser");

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(currentUser));

        CommentDTO result = commentService.getCommentById(commentId);

        assertEquals(commentId, result.getId());
        assertEquals("Test comment", result.getContent());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void getCommentById_WhenCommentNotFound_ThrowsException() {
        Long commentId = 999L;
        User currentUser = new User();
        currentUser.setUsername("testuser");

        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(currentUser));

        assertThrows(CommentNotFoundException.class, () -> commentService.getCommentById(commentId));
    }

    @Test
    @WithMockUser(username = "otheruser", roles = "EMPLOYEE")
    void getCommentById_WhenNotAuthorized_ThrowsSecurityException() {
        Long commentId = 1L;
        Comment comment = new Comment();
        comment.setId(commentId);

        Ticket ticket = new Ticket();
        User reporter = new User();
        reporter.setUsername("reporter");
        ticket.setReporter(reporter);
        comment.setTicket(ticket);

        User currentUser = new User();
        currentUser.setUsername("otheruser");

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(userRepository.findByUsername("otheruser")).thenReturn(Optional.of(currentUser));

        assertThrows(SecurityException.class, () -> commentService.getCommentById(commentId));
    }

    @Test
    @WithMockUser(username = "reporter", roles = "EMPLOYEE")
    void getCommentsByTicketId_WhenAuthorized_ReturnsList() {
        Long ticketId = 1L;
        Ticket ticket = new Ticket();
        ticket.setId(ticketId);

        User reporter = new User();
        reporter.setUsername("reporter");
        ticket.setReporter(reporter);

        Comment comment1 = new Comment();
        comment1.setId(1L);
        comment1.setContent("First comment");
        comment1.setTicket(ticket);

        Comment comment2 = new Comment();
        comment2.setId(2L);
        comment2.setContent("Second comment");
        comment2.setTicket(ticket);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findByUsername("reporter")).thenReturn(Optional.of(reporter));
        when(commentRepository.findByTicket(ticket)).thenReturn(List.of(comment1, comment2));

        List<CommentDTO> result = commentService.getCommentsByTicketId(ticketId);

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("First comment", result.get(0).getContent());
    }

    @Test
    @WithMockUser(username = "otheruser", roles = "EMPLOYEE")
    void getCommentsByTicketId_WhenNotAuthorized_ThrowsSecurityException() {
        Long ticketId = 1L;
        Ticket ticket = new Ticket();

        User reporter = new User();
        reporter.setUsername("reporter");
        ticket.setReporter(reporter);

        User currentUser = new User();
        currentUser.setUsername("otheruser");

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findByUsername("otheruser")).thenReturn(Optional.of(currentUser));

        assertThrows(SecurityException.class, () -> commentService.getCommentsByTicketId(ticketId));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void getCommentsByTicketId_WhenTicketNotFound_ThrowsException() {
        Long ticketId = 999L;

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () -> commentService.getCommentsByTicketId(ticketId));
    }

    @Test
    @WithMockUser(username = "reporter", roles = "EMPLOYEE")
    void createComment_WhenAuthorized_ReturnsCommentDTO() {
        Long ticketId = 1L;
        CreateCommentDTO dto = CreateCommentDTO.builder()
                .content("New comment")
                .build();

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);

        User reporter = new User();
        reporter.setUsername("reporter");
        ticket.setReporter(reporter);

        Comment comment = new Comment();
        comment.setId(1L);
        comment.setContent("New comment");
        comment.setTicket(ticket);
        comment.setUser(reporter);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findByUsername("reporter")).thenReturn(Optional.of(reporter));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        CommentDTO result = commentService.createComment(ticketId, dto);

        assertEquals("New comment", result.getContent());
        verify(auditLogService).auditLog(EntityType.COMMENT, Operation.CREATE, "Comment created", 1L, "reporter");
    }

    @Test
    @WithMockUser(username = "otheruser", roles = "EMPLOYEE")
    void createComment_WhenNotAuthorized_ThrowsSecurityException() {
        Long ticketId = 1L;
        CreateCommentDTO dto = CreateCommentDTO.builder()
                .content("New comment")
                .build();

        Ticket ticket = new Ticket();
        User reporter = new User();
        reporter.setUsername("reporter");
        ticket.setReporter(reporter);

        User currentUser = new User();
        currentUser.setUsername("otheruser");

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(userRepository.findByUsername("otheruser")).thenReturn(Optional.of(currentUser));

        assertThrows(SecurityException.class, () -> commentService.createComment(ticketId, dto));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void createComment_WhenTicketNotFound_ThrowsException() {
        Long ticketId = 999L;
        CreateCommentDTO dto = CreateCommentDTO.builder()
                .content("New comment")
                .build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () -> commentService.createComment(ticketId, dto));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void updateComment_WhenAuthorized_ReturnsUpdatedCommentDTO() {
        Long ticketId = 1L;
        Long commentId = 1L;
        UpdateCommentDTO dto = UpdateCommentDTO.builder()
                .content("Updated content")
                .build();

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);

        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setContent("Old content");
        comment.setTicket(ticket);

        User currentUser = new User();
        currentUser.setUsername("testuser");
        comment.setUser(currentUser);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(currentUser));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        CommentDTO result = commentService.updateComment(ticketId, commentId, dto);

        assertEquals("Updated content", result.getContent());
        verify(auditLogService).auditLog(EntityType.COMMENT, Operation.UPDATE, "Comment updated by: testuser", commentId, "testuser");
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void updateComment_WhenTicketNotFound_ThrowsException() {
        Long ticketId = 999L;
        Long commentId = 1L;
        UpdateCommentDTO dto = UpdateCommentDTO.builder()
                .content("Updated content")
                .build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () -> commentService.updateComment(ticketId, commentId, dto));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void updateComment_WhenCommentNotFound_ThrowsException() {
        Long ticketId = 1L;
        Long commentId = 999L;
        UpdateCommentDTO dto = UpdateCommentDTO.builder()
                .content("Updated content")
                .build();

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        assertThrows(CommentNotFoundException.class, () -> commentService.updateComment(ticketId, commentId, dto));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void updateComment_WhenCommentNotBelongsToTicket_ThrowsSecurityException() {
        Long ticketId = 1L;
        Long commentId = 1L;
        UpdateCommentDTO dto = UpdateCommentDTO.builder()
                .content("Updated content")
                .build();

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);

        Ticket otherTicket = new Ticket();
        otherTicket.setId(2L);

        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setTicket(otherTicket);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        assertThrows(SecurityException.class, () -> commentService.updateComment(ticketId, commentId, dto));
    }

    @Test
    @WithMockUser(username = "otheruser", roles = "EMPLOYEE")
    void updateComment_WhenNotOwner_ThrowsSecurityException() {
        Long ticketId = 1L;
        Long commentId = 1L;
        UpdateCommentDTO dto = UpdateCommentDTO.builder()
                .content("Updated content")
                .build();

        Ticket ticket = new Ticket();
        ticket.setId(ticketId);

        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setTicket(ticket);

        User owner = new User();
        owner.setUsername("owner");
        comment.setUser(owner);

        User currentUser = new User();
        currentUser.setUsername("otheruser");

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(userRepository.findByUsername("otheruser")).thenReturn(Optional.of(currentUser));

        assertThrows(SecurityException.class, () -> commentService.updateComment(ticketId, commentId, dto));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void deleteComment_WhenAuthorized_DeletesComment() {
        Long commentId = 1L;
        Comment comment = new Comment();
        comment.setId(commentId);

        User currentUser = new User();
        currentUser.setUsername("testuser");
        comment.setUser(currentUser);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(currentUser));

        commentService.deleteComment(commentId);

        verify(commentRepository).delete(comment);
        verify(auditLogService).auditLog(EntityType.COMMENT, Operation.DELETE, "Comment deleted by: testuser", commentId, "testuser");
    }

    @Test
    @WithMockUser(username = "otheruser", roles = "EMPLOYEE")
    void deleteComment_WhenNotOwner_ThrowsSecurityException() {
        Long commentId = 1L;
        Comment comment = new Comment();
        comment.setId(commentId);

        User owner = new User();
        owner.setUsername("owner");
        comment.setUser(owner);

        User currentUser = new User();
        currentUser.setUsername("otheruser");

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(userRepository.findByUsername("otheruser")).thenReturn(Optional.of(currentUser));

        assertThrows(SecurityException.class, () -> commentService.deleteComment(commentId));
    }

    @Test
    @WithMockUser(username = "testuser", roles = "ADMIN")
    void deleteComment_WhenCommentNotFound_ThrowsException() {
        Long commentId = 999L;
        User currentUser = new User();
        currentUser.setUsername("testuser");

        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(currentUser));

        assertThrows(CommentNotFoundException.class, () -> commentService.deleteComment(commentId));
    }
}
