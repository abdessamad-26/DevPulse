package com.devpulse.repository;

import com.devpulse.entity.IngestionApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IngestionApiKeyRepository extends JpaRepository<IngestionApiKey, Long> {

    Optional<IngestionApiKey> findByKeyHash(String keyHash);

    List<IngestionApiKey> findByProject_IdOrderByCreatedAtDesc(Long projectId);

    Optional<IngestionApiKey> findByIdAndProject_Id(Long id, Long projectId);
}
