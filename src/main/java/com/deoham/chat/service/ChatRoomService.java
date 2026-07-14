package com.deoham.chat.service;

import com.deoham.card.entity.Card;
import com.deoham.card.entity.CardApply;
import com.deoham.card.entity.CardApplyStatus;
import com.deoham.card.repository.CardApplyRepository;
import com.deoham.card.repository.CardRepository;
import com.deoham.chat.dto.ChatRoomLocationResponse;
import com.deoham.chat.dto.ChatRoomResponse;
import com.deoham.chat.entity.ChatMessage;
import com.deoham.chat.entity.ChatRoom;
import com.deoham.chat.entity.ChatRoomStatus;
import com.deoham.chat.repository.ChatMessageRepository;
import com.deoham.chat.repository.ChatRoomRepository;
import com.deoham.chat.repository.LastMessageProjection;
import com.deoham.chat.repository.UnreadCountProjection;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.user.entity.User;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageService chatMessageService;
    private final ChatRoomAccessService chatRoomAccessService;
    private final CardRepository cardRepository;
    private final CardApplyRepository cardApplyRepository;

    @Transactional
    public ChatRoomResponse getOrCreateRoom(UUID cardId, UUID userId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> {
                    log.warn("채팅방 생성 실패 [cardId={}, userId={}]: 카드를 찾을 수 없음", cardId, userId);
                    return new BusinessException(ErrorCode.NOT_FOUND, "카드를 찾을 수 없습니다");
                });
        chatRoomAccessService.requireParticipant(card, userId);

        var existingRoom = chatRoomRepository.findByCardId(cardId);
        ChatRoom room = existingRoom
                .orElseGet(() -> chatRoomRepository.save(ChatRoom.builder().card(card).build()));

        if (existingRoom.isEmpty()) {
            log.info("채팅방 최초 생성 [roomId={}, cardId={}, userId={}]", room.getId(), cardId, userId);
        } else {
            log.debug("기존 채팅방 반환 [roomId={}, cardId={}, userId={}]", room.getId(), cardId, userId);
        }

        return toResponse(room, unreadCountOf(room, userId), resolveOpponent(card, userId), lastMessageOf(room));
    }

    public ChatRoomResponse getRoom(UUID roomId, UUID userId) {
        ChatRoom room = chatRoomAccessService.findRoomOrThrow(roomId);
        Card card = room.getCard();
        chatRoomAccessService.requireParticipant(card, userId);
        return toResponse(room, unreadCountOf(room, userId), resolveOpponent(card, userId), lastMessageOf(room));
    }

    public Page<ChatRoomResponse> getMyRooms(UUID userId, Pageable pageable) {
        Page<ChatRoom> rooms = chatRoomRepository.findMyRooms(userId, CardApplyStatus.ACCEPTED, pageable);

        List<UUID> roomIds = rooms.getContent().stream().map(ChatRoom::getId).toList();
        List<UUID> cardIds = rooms.getContent().stream().map(r -> r.getCard().getId()).toList();

        Map<UUID, Long> unreadCounts = roomIds.isEmpty()
                ? Map.of()
                : chatMessageRepository.countUnreadGroupedByRoom(roomIds, userId).stream()
                        .collect(Collectors.toMap(UnreadCountProjection::getRoomId, UnreadCountProjection::getUnreadCount));

        Map<UUID, String> lastMessages = roomIds.isEmpty()
                ? Map.of()
                : chatMessageRepository.findLatestMessagesByRoomIds(roomIds).stream()
                        .collect(Collectors.toMap(LastMessageProjection::getRoomId, LastMessageProjection::getContent));

        Map<UUID, User> acceptedApplicants = cardIds.isEmpty()
                ? Map.of()
                : cardApplyRepository.findByCardIdInAndStatus(cardIds, CardApplyStatus.ACCEPTED).stream()
                        .collect(Collectors.toMap(a -> a.getCard().getId(), CardApply::getApplicant, (left, right) -> left));

        return rooms.map(room -> toResponse(
                room,
                unreadCounts.getOrDefault(room.getId(), 0L),
                resolveOpponent(room.getCard(), userId, acceptedApplicants),
                lastMessages.get(room.getId())));
    }

    private long unreadCountOf(ChatRoom room, UUID userId) {
        return chatMessageRepository.countByChatRoomIdAndSenderIdNotAndReadAtIsNull(room.getId(), userId);
    }

    private String lastMessageOf(ChatRoom room) {
        return chatMessageRepository.findFirstByChatRoomIdOrderBySentAtDesc(room.getId())
                .map(ChatMessage::getContent)
                .orElse(null);
    }

    private User resolveOpponent(Card card, UUID userId) {
        if (card.getRequester().getId().equals(userId)) {
            return cardApplyRepository.findByCardAndStatus(card, CardApplyStatus.ACCEPTED)
                    .map(CardApply::getApplicant)
                    .orElse(null);
        }
        return card.getRequester();
    }

    private User resolveOpponent(Card card, UUID userId, Map<UUID, User> acceptedApplicantsByCardId) {
        return card.getRequester().getId().equals(userId)
                ? acceptedApplicantsByCardId.get(card.getId())
                : card.getRequester();
    }

    public ChatRoomLocationResponse getCardLocation(UUID roomId, UUID userId) {
        ChatRoom room = chatRoomAccessService.findRoomOrThrow(roomId);
        chatRoomAccessService.requireParticipant(room.getCard(), userId);
        var point = room.getCard().getLocation();
        return new ChatRoomLocationResponse(point.getY(), point.getX(), room.getCard().getCity());
    }

    @Transactional
    public void closeRoom(UUID roomId, UUID userId) {
        ChatRoom room = chatRoomAccessService.findRoomOrThrow(roomId);
        chatRoomAccessService.requireParticipant(room.getCard(), userId);
        if (room.getStatus() == ChatRoomStatus.CLOSED) {
            log.debug("채팅방 종료 요청 무시 [roomId={}, actorId={}]: 이미 종료된 방", roomId, userId);
            return;
        }
        chatMessageService.sendRoomClosedMessage(room, userId);
        room.close();
        log.info("채팅방 종료 완료 [roomId={}, actorId={}]", roomId, userId);
    }

    private ChatRoomResponse toResponse(ChatRoom room, long unreadCount, User opponent, String lastMessage) {
        return new ChatRoomResponse(
                room.getId(),
                room.getCard().getId(),
                room.getStatus().name(),
                room.getCreatedAt(),
                room.getClosedAt(),
                unreadCount,
                opponent != null ? opponent.getNickname() : null,
                opponent != null ? opponent.getProfileImageUrl() : null,
                lastMessage);
    }
}
