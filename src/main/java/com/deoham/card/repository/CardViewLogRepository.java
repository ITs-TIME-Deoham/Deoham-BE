package com.deoham.card.repository;

import com.deoham.card.entity.CardViewLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CardViewLogRepository extends JpaRepository<CardViewLog, UUID> {

    List<CardViewLog> findByCardShareIdOrderByViewedAtDesc(UUID cardShareId);
}
