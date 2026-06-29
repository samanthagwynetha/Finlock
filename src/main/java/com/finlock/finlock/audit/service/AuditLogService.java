package com.finlock.finlock.audit.service;

import com.finlock.finlock.audit.entity.AuditLog;
import com.finlock.finlock.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void log(UUID userId, String action, String description, String ipAddress) {
        try {
            AuditLog entry = AuditLog.builder()
                    .userId(userId)
                    .action(action)
                    .description(description)
                    .ipAddress(ipAddress)
                    .build();

            auditLogRepository.save(entry);
        } catch (Exception e){
            System.err.println("Failed to write audit log: " + e.getMessage());
        }
    }
}
