package dev.brunovasconcellos.vbank.repository;

import dev.brunovasconcellos.vbank.domain.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    @Query("select u from User u where u.email = :identifier or u.username = :identifier")
    Optional<User> findByIdentifier(@Param("identifier") String identifier);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findLockedById(@Param("id") UUID id);

    @Query("select u from User u where :search is null or lower(u.fullName) like lower(concat('%', :search, '%')) or lower(u.email) like lower(concat('%', :search, '%')) or lower(u.username) like lower(concat('%', :search, '%'))")
    Page<User> search(@Param("search") String search, Pageable pageable);
}

