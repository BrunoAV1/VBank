package dev.brunovasconcellos.vbank.repository;

import dev.brunovasconcellos.vbank.domain.Transfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TransferRepository extends JpaRepository<Transfer, UUID>, JpaSpecificationExecutor<Transfer> {
    Optional<Transfer> findBySourceAccountIdAndIdempotencyKey(UUID sourceAccountId, String idempotencyKey);

    @Query("select t from Transfer t join fetch t.sourceAccount s join fetch s.user join fetch t.destinationAccount d join fetch d.user where t.id = :id")
    Optional<Transfer> findDetailedById(@Param("id") UUID id);

    @Query(value = "select t from Transfer t where t.sourceAccount.id = :accountId or t.destinationAccount.id = :accountId",
           countQuery = "select count(t) from Transfer t where t.sourceAccount.id = :accountId or t.destinationAccount.id = :accountId")
    Page<Transfer> findForAccount(@Param("accountId") UUID accountId, Pageable pageable);

}
