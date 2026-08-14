package com.ticketsystem.it_ticket_system.service;

import com.ticketsystem.it_ticket_system.dto.AuditLogDTO;
import com.ticketsystem.it_ticket_system.model.AuditLog;
import com.ticketsystem.it_ticket_system.model.EntityType;
import com.ticketsystem.it_ticket_system.model.Operation;
import com.ticketsystem.it_ticket_system.repository.AuditLogRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void auditLog(EntityType entityType, Operation operation, String details, Long entityId, String performedBy)
    {
        AuditLog log = AuditLog.builder()
                .entityType(entityType)
                .operation(operation)
                .details(details)
                .entityId(entityId)
                .performedBy(performedBy)
                .build();
        auditLogRepository.save(log);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<AuditLogDTO> getAllAuditLogs() {
        return auditLogRepository.findAll().stream()
                .map(AuditLogDTO::fromEntity)
                .toList();
    }
}
