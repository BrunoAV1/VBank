package dev.brunovasconcellos.vbank.repository;

import dev.brunovasconcellos.vbank.domain.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    @Query("select a from Account a join fetch a.user where a.user.id = :userId")
    Optional<Account> findByUserId(@Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a join fetch a.user where a.id in :ids order by a.id")
    List<Account> findAllLockedById(@Param("ids") Collection<UUID> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a join fetch a.user where a.id = :id")
    Optional<Account> findLockedById(@Param("id") UUID id);

    @Query("select a from Account a join fetch a.user where a.status = :status")
    Optional<Account> findByStatus(@Param("status") dev.brunovasconcellos.vbank.domain.Enums.AccountStatus status);
}
