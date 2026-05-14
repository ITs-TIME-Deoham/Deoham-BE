package com.deoham.card.repository;

import com.deoham.card.entity.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID> {

    Page<Card> findByProjectId(UUID projectId, Pageable pageable);
}
