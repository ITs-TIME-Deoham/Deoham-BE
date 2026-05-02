package com.deoham.card.repository;

import com.deoham.card.entity.AiAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, UUID> {

    Optional<AiAnalysis> findByCardIdAndIsLatestTrue(UUID cardId);

    List<AiAnalysis> findByCardIdOrderByAnalyzedAtDesc(UUID cardId);
}
