package com.company.shop.aspect;

import com.company.shop.entity.AuditLog;
import com.company.shop.entity.Order;
import com.company.shop.entity.Product;
import com.company.shop.repository.AuditLogRepository;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Aspect
@Component
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;

    @Autowired
    public AuditAspect(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @AfterReturning(pointcut = "@annotation(auditOperation)", returning = "result")
    public void auditMethodExecution(JoinPoint joinPoint, AuditOperation auditOperation, Object result) {
        String operation = auditOperation.value();
        Long referenceId = null;

        // Try extracting Reference ID from return value or method arguments
        if ("PRODUCT_DELETE".equals(operation) || "ORDER_STATUS_CHANGED".equals(operation)) {
            Object[] args = joinPoint.getArgs();
            if (args.length > 0 && args[0] instanceof Long) {
                referenceId = (Long) args[0];
            }
        } else {
            if (result instanceof Product) {
                referenceId = ((Product) result).getId();
            } else if (result instanceof Order) {
                referenceId = ((Order) result).getId();
            }
        }

        // Retrieve the logged-in username
        String username = "anonymous";
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            username = authentication.getName();
        }

        // Save audit log
        AuditLog auditLog = new AuditLog(null, username, operation, referenceId, LocalDateTime.now());
        auditLogRepository.save(auditLog);
    }
}
