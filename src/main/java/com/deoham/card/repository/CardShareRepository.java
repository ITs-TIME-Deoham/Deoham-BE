package com.deoham.card.repository;

import com.deoham.card.entity.CardShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardShareRepository extends JpaRepository<CardShare, UUID> {

    Optional<CardShare> findByToken(String token);

    List<CardShare> findByCardIdAndRevokedAtIsNull(UUID cardId);
}
