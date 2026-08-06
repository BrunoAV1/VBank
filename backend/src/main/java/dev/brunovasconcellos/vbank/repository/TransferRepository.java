package dev.brunovasconcellos.vbank.repository;

import dev.brunovasconcellos.vbank.domain.Enums;
import dev.brunovasconcellos.vbank.domain.Transfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {
    Optional<Transfer> findBySourceAccountIdAndIdempotencyKey(UUID sourceAccountId, String idempotencyKey);

    @Query("select t from Transfer t join fetch t.sourceAccount s join fetch s.user join fetch t.destinationAccount d join fetch d.user where t.id = :id")
    Optional<Transfer> findDetailedById(@Param("id") UUID id);

    @Query(value = "select t from Transfer t where t.sourceAccount.id = :accountId or t.destinationAccount.id = :accountId",
           countQuery = "select count(t) from Transfer t where t.sourceAccount.id = :accountId or t.destinationAccount.id = :accountId")
    Page<Transfer> findForAccount(@Param("accountId") UUID accountId, Pageable pageable);

    @Query(value = "select t from Transfer t join t.sourceAccount s join s.user su join t.destinationAccount d join d.user du where " +
            "(:search is null or lower(t.publicId) like lower(concat('%', :search, '%')) " +
            "or lower(t.endToEndId) like lower(concat('%', :search, '%')) or lower(su.fullName) like lower(concat('%', :search, '%')) " +
            "or lower(du.fullName) like lower(concat('%', :search, '%'))) and (:status is null or t.status = :status) " +
            "and (:minAmount is null or t.amount >= :minAmount) and (:maxAmount is null or t.amount <= :maxAmount) " +
            "and (:from is null or t.createdAt >= :from) and (:to is null or t.createdAt < :to)",
           countQuery = "select count(t) from Transfer t join t.sourceAccount s join s.user su join t.destinationAccount d join d.user du where " +
            "(:search is null or lower(t.publicId) like lower(concat('%', :search, '%')) " +
            "or lower(t.endToEndId) like lower(concat('%', :search, '%')) or lower(su.fullName) like lower(concat('%', :search, '%')) " +
            "or lower(du.fullName) like lower(concat('%', :search, '%'))) and (:status is null or t.status = :status) " +
            "and (:minAmount is null or t.amount >= :minAmount) and (:maxAmount is null or t.amount <= :maxAmount) " +
            "and (:from is null or t.createdAt >= :from) and (:to is null or t.createdAt < :to)")
    Page<Transfer> search(@Param("search") String search, @Param("status") Enums.TransferStatus status,
                          @Param("minAmount") BigDecimal minAmount, @Param("maxAmount") BigDecimal maxAmount,
                          @Param("from") Instant from, @Param("to") Instant to, Pageable pageable);
}
