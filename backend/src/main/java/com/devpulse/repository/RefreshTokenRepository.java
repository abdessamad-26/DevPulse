package com.devpulse.repository;

import com.devpulse.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying(flushAutomatically = true)
    @Query("delete from RefreshToken token where token.expiresAt <= :expiresAt")
    int deleteExpiredBefore(@Param("expiresAt") LocalDateTime expiresAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RefreshToken token
            set token.revokedAt = :revokedAt
            where token.tokenHash = :tokenHash and token.revokedAt is null
            """)
    int revokeIfActive(@Param("tokenHash") String tokenHash,
                       @Param("revokedAt") LocalDateTime revokedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RefreshToken token
            set token.revokedAt = :revokedAt
            where token.familyId = :familyId and token.revokedAt is null
            """)
    int revokeActiveFamily(@Param("familyId") UUID familyId,
                           @Param("revokedAt") LocalDateTime revokedAt);
}
