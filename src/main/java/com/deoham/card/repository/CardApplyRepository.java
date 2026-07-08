package com.deoham.card.repository;

import com.deoham.card.entity.Card;
import com.deoham.card.entity.CardApply;
import com.deoham.card.entity.CardApplyStatus;
import com.deoham.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardApplyRepository extends JpaRepository<CardApply, UUID> {

    boolean existsByCardAndApplicant(Card card, User applicant);

    List<CardApply> findByCard(Card card);

    Optional<CardApply> findByCardAndApplicant(Card card, User applicant);

    Optional<CardApply> findByCardAndStatus(Card card, CardApplyStatus status);

    long countByApplicantId(UUID applicantId);

    long countByApplicantIdAndStatus(UUID applicantId, CardApplyStatus status);
}
