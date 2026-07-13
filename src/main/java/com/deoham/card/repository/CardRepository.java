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

    @Query("""
            SELECT c FROM Card c
            JOIN FETCH c.requester
            WHERE c.id = :cardId
            """)
    Optional<Card> findByIdWithRequester(@Param("cardId") UUID cardId);

    long countByRequesterId(UUID requesterId);

    long countByRequesterIdAndStatus(UUID requesterId, CardStatus status);

    @Query(value = """
            SELECT c.id,
                   c.requester_id,
                   u.nickname,
                   u.profile_image_url,
                   c.category::text,
                   c.description,
                   c.expires_at,
                   c.status::text,
                   c.preferred_gender::text,
                   c.preferred_age_min,
                   c.preferred_age_max,
                   c.retry_count,
                   c.created_at,
                   c.updated_at,
                   ST_Distance(c.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography) AS distance_meters
            FROM cards c
            JOIN users u ON c.requester_id = u.id
            WHERE c.status = 'OPEN'
              AND c.expires_at > now()
              AND ST_DWithin(c.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, COALESCE(c.radius_m, 50000))
              AND ((:cursorDistance IS NULL)
                   OR (ST_Distance(c.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography) > :cursorDistance)
                   OR (ST_Distance(c.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography) = :cursorDistance AND c.id::text > :cursorCardId))
            ORDER BY distance_meters ASC, c.id ASC
            LIMIT 21
            """, nativeQuery = true)
    List<Object[]> findNearbyCards(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("cursorDistance") Double cursorDistance,
            @Param("cursorCardId") String cursorCardId
    );
}
