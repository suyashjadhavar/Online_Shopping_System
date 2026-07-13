package com.company.shop.service;

import com.company.shop.entity.AuditLog;
import java.util.List;

public interface AuditLogService {
    List<AuditLog> getAllAuditLogs();
    List<AuditLog> getAuditLogsByUsername(String username);
    List<AuditLog> getAuditLogsByOperation(String operation);
    List<AuditLog> getAuditLogsByReferenceId(Long id);
}
