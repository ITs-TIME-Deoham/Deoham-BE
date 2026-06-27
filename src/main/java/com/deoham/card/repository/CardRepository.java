package com.deoham.card.repository;

import com.deoham.card.entity.Card;
import com.deoham.card.entity.CardStatus;
import com.deoham.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID> {

    List<Card> findByAuthorAndStatusIn(User author, List<CardStatus> statuses);
}
