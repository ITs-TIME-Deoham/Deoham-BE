package com.deoham.chat.service;

import com.deoham.card.entity.Card;
import com.deoham.card.entity.CardApplyStatus;
import com.deoham.card.repository.CardApplyRepository;
import com.deoham.chat.entity.ChatRoom;
import com.deoham.chat.repository.ChatRoomRepository;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * ChatMessageService(및 그 하위 SimpMessagingTemplate 의존성)를 거치지 않도록 분리한
 * 채팅방 조회/참여자 검증 컴포넌트. StompAuthChannelInterceptor가 ChatRoomService를
 * 직접 의존하면 WebSocket 메시지 브로커 빈 그래프와 순환 의존이 생기므로 여기서 끊는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRoomAccessService {

    private final ChatRoomRepository chatRoomRepository;
    private final CardApplyRepository cardApplyRepository;

    public ChatRoom findRoomOrThrow(UUID roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> {
                    log.warn("채팅방 조회 실패 [roomId={}]: 존재하지 않음", roomId);
                    return new BusinessException(ErrorCode.NOT_FOUND, "채팅방을 찾을 수 없습니다");
                });
    }

    public void requireParticipant(Card card, UUID userId) {
        if (card.getRequester().getId().equals(userId)) return;
        boolean isAcceptedApplicant = cardApplyRepository.findByCard(card).stream()
                .anyMatch(a -> a.getStatus() == CardApplyStatus.ACCEPTED
                            && a.getApplicant().getId().equals(userId));
        if (!isAcceptedApplicant) {
            log.warn("채팅방 접근 거부 [cardId={}, userId={}]: 참여자가 아님", card.getId(), userId);
            throw new BusinessException(ErrorCode.FORBIDDEN, "채팅방 참여자가 아닙니다");
        }
    }

    public void verifySubscribeAccess(UUID roomId, UUID userId) {
        ChatRoom room = findRoomOrThrow(roomId);
        requireParticipant(room.getCard(), userId);
    }
}
