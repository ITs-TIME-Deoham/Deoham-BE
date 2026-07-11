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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageService chatMessageService;
    private final CardRepository cardRepository;
    private final CardApplyRepository cardApplyRepository;

    @Transactional
    public ChatRoomResponse getOrCreateRoom(UUID cardId, UUID userId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "카드를 찾을 수 없습니다"));
        requireParticipant(card, userId);

        ChatRoom room = chatRoomRepository.findByCardId(cardId)
                .orElseGet(() -> chatRoomRepository.save(ChatRoom.builder().card(card).build()));

        return toResponse(room, unreadCountOf(room, userId), resolveOpponent(card, userId), lastMessageOf(room));
    }

    public ChatRoomResponse getRoom(UUID roomId, UUID userId) {
        ChatRoom room = findRoomOrThrow(roomId);
        Card card = room.getCard();
        requireParticipant(card, userId);
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
        ChatRoom room = findRoomOrThrow(roomId);
        requireParticipant(room.getCard(), userId);
        var point = room.getCard().getLocation();
        return new ChatRoomLocationResponse(point.getY(), point.getX(), room.getCard().getCity());
    }

    @Transactional
    public void closeRoom(UUID roomId, UUID userId) {
        ChatRoom room = findRoomOrThrow(roomId);
        requireParticipant(room.getCard(), userId);
        if (room.getStatus() == ChatRoomStatus.CLOSED) {
            return;
        }
        chatMessageService.sendRoomClosedMessage(room, userId);
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
