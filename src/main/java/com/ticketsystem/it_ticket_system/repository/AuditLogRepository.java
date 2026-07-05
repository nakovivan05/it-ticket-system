package com.ticketsystem.it_ticket_system.repository;

import com.ticketsystem.it_ticket_system.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
