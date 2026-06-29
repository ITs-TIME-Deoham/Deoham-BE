package com.deoham.chat.service;

import com.deoham.card.entity.Card;
import com.deoham.card.entity.CardApplyStatus;
import com.deoham.card.repository.CardApplyRepository;
import com.deoham.card.repository.CardRepository;
import com.deoham.chat.dto.ChatRoomLocationResponse;
import com.deoham.chat.dto.ChatRoomResponse;
import com.deoham.chat.entity.ChatRoom;
import com.deoham.chat.repository.ChatRoomRepository;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final CardRepository cardRepository;
    private final CardApplyRepository cardApplyRepository;

    @Transactional
    public ChatRoomResponse getOrCreateRoom(UUID cardId, UUID userId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "카드를 찾을 수 없습니다"));
        requireParticipant(card, userId);

        ChatRoom room = chatRoomRepository.findByCardId(cardId)
                .orElseGet(() -> chatRoomRepository.save(ChatRoom.builder().card(card).build()));

        return toResponse(room);
    }

    public ChatRoomResponse getRoom(UUID roomId, UUID userId) {
        ChatRoom room = findRoomOrThrow(roomId);
        requireParticipant(room.getCard(), userId);
        return toResponse(room);
    }

    public Page<ChatRoomResponse> getMyRooms(UUID userId, Pageable pageable) {
        return chatRoomRepository.findMyRooms(userId, CardApplyStatus.ACCEPTED, pageable)
                .map(this::toResponse);
    }

    public ChatRoomLocationResponse getCardLocation(UUID roomId, UUID userId) {
        ChatRoom room = findRoomOrThrow(roomId);
        requireParticipant(room.getCard(), userId);
        var point = room.getCard().getLocation();
        return new ChatRoomLocationResponse(point.getY(), point.getX(), room.getCard().getCity());
    }

    @Transactional
    public void closeRoom(UUID roomId, UUID userId) {
        ChatRoom room = findRoomOrThrow(roomId);
        requireParticipant(room.getCard(), userId);
        room.close();
    }

    private void requireParticipant(Card card, UUID userId) {
        if (card.getRequester().getId().equals(userId)) return;
        boolean isAcceptedApplicant = cardApplyRepository.findByCard(card).stream()
                .anyMatch(a -> a.getStatus() == CardApplyStatus.ACCEPTED
                            && a.getApplicant().getId().equals(userId));
        if (!isAcceptedApplicant) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "채팅방 참여자가 아닙니다");
        }
    }

    private ChatRoom findRoomOrThrow(UUID roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "채팅방을 찾을 수 없습니다"));
    }

    private ChatRoomResponse toResponse(ChatRoom room) {
        return new ChatRoomResponse(
                room.getId(),
                room.getCard().getId(),
                room.getStatus().name(),
                room.getCreatedAt(),
                room.getClosedAt());
    }
}
