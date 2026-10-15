package com.universalshield.repository;

import com.universalshield.domain.DetectorMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetectorMetadataRepository extends JpaRepository<DetectorMetadata, String> {
}
