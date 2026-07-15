package com.deoham.card.repository;

import com.deoham.card.entity.Card;
import com.deoham.card.entity.CardApply;
import com.deoham.card.entity.CardApplyStatus;
import com.deoham.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardApplyRepository extends JpaRepository<CardApply, UUID> {

    boolean existsByCardAndApplicant(Card card, User applicant);

    @Query("""
            SELECT a FROM CardApply a
            JOIN FETCH a.applicant
            WHERE a.card = :card
            """)
    List<CardApply> findByCard(@Param("card") Card card);

    Optional<CardApply> findByCardAndApplicant(Card card, User applicant);

    @Query("""
            SELECT a FROM CardApply a
            JOIN FETCH a.applicant
            WHERE a.card = :card AND a.status = :status
            """)
    Optional<CardApply> findByCardAndStatus(@Param("card") Card card, @Param("status") CardApplyStatus status);

    long countByApplicantId(UUID applicantId);

    long countByApplicantIdAndStatus(UUID applicantId, CardApplyStatus status);

    @Query("""
            SELECT a FROM CardApply a
            JOIN FETCH a.applicant
            WHERE a.card.id IN :cardIds AND a.status = :status
            """)
    List<CardApply> findByCardIdInAndStatus(
            @Param("cardIds") List<UUID> cardIds, @Param("status") CardApplyStatus status);

    @Query("""
            SELECT a FROM CardApply a
            WHERE a.status = :status
              AND ((a.card.requester.id = :userA AND a.applicant.id = :userB)
                OR (a.card.requester.id = :userB AND a.applicant.id = :userA))
            """)
    List<CardApply> findByStatusAndUserPair(
            @Param("status") CardApplyStatus status,
            @Param("userA") UUID userA,
            @Param("userB") UUID userB);
}
