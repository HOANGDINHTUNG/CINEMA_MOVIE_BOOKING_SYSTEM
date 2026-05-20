package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.entity.AuditLog;
import com.re.cinemamoviebookingsystem.repository.AuditLogRepository;
import com.re.cinemamoviebookingsystem.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(rollbackFor = Exception.class)
    public void log(String action, String entityType, String entityId, String detail) {
        Long userId = null;
        String username = "system";
        try {
            userId = SecurityUtils.currentUserId();
            username = SecurityUtils.currentUser().getUsername();
        } catch (Exception ignored) {
        }
        auditLogRepository.save(AuditLog.builder()
                .userId(userId)
                .username(username)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .detail(detail)
                .build());
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> list(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
}
