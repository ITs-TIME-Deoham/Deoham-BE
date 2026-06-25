package com.deoham.chat.service;

import com.deoham.chat.dto.ChatRoomCreateRequest;
import com.deoham.chat.dto.ChatRoomMemberResponse;
import com.deoham.chat.dto.ChatRoomResponse;
import com.deoham.chat.entity.ChatRoom;
import com.deoham.chat.entity.ChatRoomMember;
import com.deoham.chat.entity.ChatRoomMemberRole;
import com.deoham.chat.repository.ChatRoomMemberRepository;
import com.deoham.chat.repository.ChatRoomRepository;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.project.entity.Project;
import com.deoham.project.repository.ProjectRepository;
import com.deoham.user.entity.User;
import com.deoham.user.repository.UserRepository;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
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
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChatRoomResponse createRoom(UUID requesterId, ChatRoomCreateRequest request) {
        LinkedHashSet<UUID> memberIds = new LinkedHashSet<>();
        memberIds.add(requesterId);
        memberIds.addAll(request.memberUserIds());

        boolean isDirect = memberIds.size() == 2;
        if (isDirect) {
            List<UUID> ids = List.copyOf(memberIds);
            var existing = chatRoomRepository.findDirectRoomBetween(ids.get(0), ids.get(1));
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }

        Project project = request.projectId() != null
                ? projectRepository.findById(request.projectId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "프로젝트를 찾을 수 없습니다"))
                : null;

        ChatRoom room = chatRoomRepository.save(ChatRoom.builder()
                .project(project)
                .name(isDirect ? null : request.name())
                .isDirect(isDirect)
                .build());

        for (UUID memberId : memberIds) {
            User user = userRepository.findById(memberId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다"));
            ChatRoomMemberRole role = memberId.equals(requesterId) ? ChatRoomMemberRole.OWNER : ChatRoomMemberRole.MEMBER;
            chatRoomMemberRepository.save(ChatRoomMember.builder()
                    .chatRoom(room)
                    .user(user)
                    .role(role)
                    .build());
        }

        return toResponse(room);
    }

    public Page<ChatRoomResponse> getMyRooms(UUID userId, Pageable pageable) {
        return chatRoomRepository.findMyRooms(userId, pageable).map(this::toResponse);
    }

    public ChatRoomResponse getRoom(UUID userId, UUID roomId) {
        requireActiveMembership(roomId, userId);
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "채팅방을 찾을 수 없습니다"));
        return toResponse(room);
    }

    @Transactional
    public void leaveRoom(UUID userId, UUID roomId) {
        ChatRoomMember member = requireActiveMembership(roomId, userId);
        member.leave(Instant.now());
    }

    private ChatRoomMember requireActiveMembership(UUID roomId, UUID userId) {
        return chatRoomMemberRepository.findByChatRoomIdAndUserIdAndLeftAtIsNull(roomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "채팅방 멤버가 아닙니다"));
    }

    private ChatRoomResponse toResponse(ChatRoom room) {
        List<ChatRoomMemberResponse> members = chatRoomMemberRepository.findByChatRoomIdAndLeftAtIsNull(room.getId())
                .stream()
                .map(m -> new ChatRoomMemberResponse(
                        m.getUser().getId(),
                        m.getUser().getName(),
                        m.getRole().name(),
                        m.getCreatedAt()))
                .toList();

        return new ChatRoomResponse(
                room.getId(),
                room.getProject() != null ? room.getProject().getId() : null,
                room.getName(),
                room.isDirect(),
                members,
                room.getLastMessageAt(),
                room.getCreatedAt());
    }
}
