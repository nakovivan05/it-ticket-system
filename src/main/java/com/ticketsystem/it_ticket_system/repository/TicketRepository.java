package com.ticketsystem.it_ticket_system.repository;

import com.ticketsystem.it_ticket_system.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface TicketRepository extends JpaRepository<Ticket,Long> {

}
