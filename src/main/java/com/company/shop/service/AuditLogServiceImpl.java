package com.company.shop.service;

import com.company.shop.entity.AuditLog;
import com.company.shop.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Autowired
    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findAll();
    }

    @Override
    public List<AuditLog> getAuditLogsByUsername(String username) {
        return auditLogRepository.findByUsername(username);
    }

    @Override
    public List<AuditLog> getAuditLogsByOperation(String operation) {
        return auditLogRepository.findByOperation(operation);
    }

    @Override
    public List<AuditLog> getAuditLogsByReferenceId(Long id) {
        return auditLogRepository.findByReferenceId(id);
    }
}
