package com.company.shop.repository;

import com.company.shop.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByUsername(String username);
    List<AuditLog> findByOperation(String operation);
    List<AuditLog> findByReferenceId(Long referenceId);
}
