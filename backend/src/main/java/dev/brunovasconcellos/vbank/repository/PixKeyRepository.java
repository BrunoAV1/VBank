package dev.brunovasconcellos.vbank.repository;

import dev.brunovasconcellos.vbank.domain.Enums;
import dev.brunovasconcellos.vbank.domain.PixKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PixKeyRepository extends JpaRepository<PixKey, UUID> {
    @Query("select k from PixKey k join fetch k.account a join fetch a.user where k.normalizedValue = :value and k.status = :status")
    Optional<PixKey> findByNormalizedValueAndStatus(@Param("value") String value,
                                                     @Param("status") Enums.PixKeyStatus status);

    @Query("select k from PixKey k where k.account.id = :accountId and k.status = :status order by k.createdAt")
    List<PixKey> findByAccountAndStatus(@Param("accountId") UUID accountId,
                                        @Param("status") Enums.PixKeyStatus status);
}

