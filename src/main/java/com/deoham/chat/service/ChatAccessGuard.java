package com.deoham.chat.service;

import com.deoham.card.entity.Card;
import com.deoham.card.entity.CardApplyStatus;
import com.deoham.card.repository.CardApplyRepository;
import com.deoham.chat.entity.ChatMessage;
import com.deoham.chat.entity.ChatRoom;
import com.deoham.chat.repository.ChatMessageRepository;
import com.deoham.chat.repository.ChatRoomRepository;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatAccessGuard {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final CardApplyRepository cardApplyRepository;

    public ChatMessage findMessageOrThrow(UUID messageId) {
        return chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "메시지를 찾을 수 없습니다"));
    }

    public ChatRoom findRoomOrThrow(UUID roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "채팅방을 찾을 수 없습니다"));
    }

    public void requireParticipant(Card card, UUID userId) {
        if (card.getRequester().getId().equals(userId)) return;
        boolean isAcceptedApplicant = cardApplyRepository.findByCard(card).stream()
                .anyMatch(a -> a.getStatus() == CardApplyStatus.ACCEPTED
                            && a.getApplicant().getId().equals(userId));
        if (!isAcceptedApplicant) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "채팅방 참여자가 아닙니다");
        }
    }
}
