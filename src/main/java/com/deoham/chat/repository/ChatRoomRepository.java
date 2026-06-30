package com.deoham.chat.repository;

import com.deoham.card.entity.CardApplyStatus;
import com.deoham.chat.entity.ChatRoom;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {

    Optional<ChatRoom> findByCardId(UUID cardId);

    @Query("""
            SELECT r FROM ChatRoom r
            WHERE r.card.requester.id = :userId
               OR EXISTS (
                   SELECT a FROM CardApply a
                   WHERE a.card = r.card
                     AND a.applicant.id = :userId
                     AND a.status = :accepted
               )
            ORDER BY r.createdAt DESC
            """)
    Page<ChatRoom> findMyRooms(
            @Param("userId") UUID userId,
            @Param("accepted") CardApplyStatus accepted,
            Pageable pageable);
}
