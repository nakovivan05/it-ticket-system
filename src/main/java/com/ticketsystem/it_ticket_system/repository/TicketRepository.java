package com.ticketsystem.it_ticket_system.repository;

import com.ticketsystem.it_ticket_system.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface TicketRepository extends JpaRepository<Ticket,Long> {
    List<Ticket> findAllByOrderByCreatedAtDesc();
    List<Ticket> findAllByOrderByCreatedAtAsc();
    List<Ticket> findAllByOrderByUpdatedAtDesc();
    List<Ticket> findAllByOrderByUpdatedAtAsc();
    List<Ticket> findAllByOrderByResolvedAtDesc();
    List<Ticket> findAllByOrderByResolvedAtAsc();
    List<Ticket> findAllByOrderByStatusDesc();
    List<Ticket> findAllByOrderByStatusAsc();
}
