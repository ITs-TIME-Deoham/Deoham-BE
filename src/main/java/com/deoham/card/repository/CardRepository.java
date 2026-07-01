package com.deoham.card.repository;

import com.deoham.card.entity.Card;
import com.deoham.card.entity.CardStatus;
import com.deoham.user.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID> {

    List<Card> findByRequesterAndStatusIn(User requester, List<CardStatus> statuses);

    Optional<Card> findFirstByRequesterIdAndStatusIn(UUID requesterId, List<CardStatus> statuses);

    @Query(value = """
            SELECT c.id,
                   c.category::text,
                   c.status::text,
                   c.expires_at,
                   c.preferred_gender::text,
                   c.preferred_age_min,
                   c.preferred_age_max,
                   ST_Distance(c.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography) AS distance_meters,
                   c.created_at
            FROM cards c
            WHERE c.status = 'OPEN'
              AND c.expires_at > now()
              AND ST_DWithin(c.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusMeters)
            ORDER BY distance_meters
            """, nativeQuery = true)
    List<Object[]> findNearbyCards(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusMeters") double radiusMeters
    );
}
