package com.ticketsystem.it_ticket_system.dto;

import com.ticketsystem.it_ticket_system.model.AuditLog;
import com.ticketsystem.it_ticket_system.model.EntityType;
import com.ticketsystem.it_ticket_system.model.Operation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditLogDTO {
    private Long id;
    private EntityType entityType;
    private Operation operation;
    private String details;
    private Long entityId;
    private String performedBy;
    private LocalDateTime timestamp;

    public static AuditLogDTO fromEntity(AuditLog auditLog) {
        return AuditLogDTO.builder()
                .id(auditLog.getId())
                .entityType(auditLog.getEntityType())
                .operation(auditLog.getOperation())
                .details(auditLog.getDetails())
                .entityId(auditLog.getEntityId())
                .performedBy(auditLog.getPerformedBy())
                .timestamp(auditLog.getTimestamp())
                .build();
    }
}