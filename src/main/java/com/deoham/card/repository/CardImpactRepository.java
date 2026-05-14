package com.deoham.card.repository;

import com.deoham.card.entity.CardImpact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CardImpactRepository extends JpaRepository<CardImpact, UUID> {

    List<CardImpact> findByCardId(UUID cardId);
}
