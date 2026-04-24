package com.cdweb.be.service;

import com.cdweb.be.entity.AuditLog;
import com.cdweb.be.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

  @Autowired private AuditLogRepository auditLogRepository;

  public void log(String action, String entityName, String entityId, String details) {
    String username =
        SecurityContextHolder.getContext().getAuthentication() != null
            ? SecurityContextHolder.getContext().getAuthentication().getName()
            : "SYSTEM";

    AuditLog auditLog = new AuditLog();
    auditLog.setAction(action);
    auditLog.setEntityName(entityName);
    auditLog.setEntityId(entityId);
    auditLog.setPerformedBy(username);
    auditLog.setDetails(details);

    auditLogRepository.save(auditLog);
  }
}
