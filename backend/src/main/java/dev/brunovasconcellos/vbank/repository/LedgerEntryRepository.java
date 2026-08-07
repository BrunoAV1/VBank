package dev.brunovasconcellos.vbank.repository;

import dev.brunovasconcellos.vbank.domain.Enums;
import dev.brunovasconcellos.vbank.domain.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
    @Query(value = "select e from LedgerEntry e where e.account.id = :accountId " +
            "and (:type is null or e.type = :type) and (:from is null or e.createdAt >= :from) " +
            "and (:to is null or e.createdAt < :to) and (:minAmount is null or e.amount >= :minAmount) " +
            "and (:maxAmount is null or e.amount <= :maxAmount) " +
            "and (:search = '' or lower(e.description) like lower(concat('%', :search, '%')))",
            countQuery = "select count(e) from LedgerEntry e where e.account.id = :accountId " +
            "and (:type is null or e.type = :type) and (:from is null or e.createdAt >= :from) " +
            "and (:to is null or e.createdAt < :to) and (:minAmount is null or e.amount >= :minAmount) " +
            "and (:maxAmount is null or e.amount <= :maxAmount) " +
            "and (:search = '' or lower(e.description) like lower(concat('%', :search, '%')))")
    Page<LedgerEntry> search(@Param("accountId") UUID accountId,
                             @Param("type") Enums.LedgerType type,
                             @Param("from") Instant from,
                             @Param("to") Instant to,
                             @Param("minAmount") BigDecimal minAmount,
                             @Param("maxAmount") BigDecimal maxAmount,
                             @Param("search") String search,
                             Pageable pageable);
}
