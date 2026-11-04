package com.devpulse.repository;

import com.devpulse.entity.IngestionApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IngestionApiKeyRepository extends JpaRepository<IngestionApiKey, Long> {

    Optional<IngestionApiKey> findByKeyHash(String keyHash);

    @Modifying
    @Transactional
    @Query("""
            update IngestionApiKey apiKey
            set apiKey.lastUsedAt = :now
            where apiKey.id = :id
              and (apiKey.lastUsedAt is null or apiKey.lastUsedAt <= :cutoff)
            """)
    int updateLastUsedAtIfStale(@Param("id") Long id, @Param("cutoff") LocalDateTime cutoff,
                                @Param("now") LocalDateTime now);

    List<IngestionApiKey> findByProject_IdOrderByCreatedAtDesc(Long projectId);

    Optional<IngestionApiKey> findByIdAndProject_Id(Long id, Long projectId);
}
