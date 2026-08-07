package dev.brunovasconcellos.vbank.repository;

import dev.brunovasconcellos.vbank.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    @Query(value = "select a from AuditLog a where :search = '' or lower(a.action) like lower(concat('%', :search, '%')) or lower(a.actorLabel) like lower(concat('%', :search, '%')) or lower(a.targetId) like lower(concat('%', :search, '%'))",
           countQuery = "select count(a) from AuditLog a where :search = '' or lower(a.action) like lower(concat('%', :search, '%')) or lower(a.actorLabel) like lower(concat('%', :search, '%')) or lower(a.targetId) like lower(concat('%', :search, '%'))")
    Page<AuditLog> search(@Param("search") String search, Pageable pageable);
}
