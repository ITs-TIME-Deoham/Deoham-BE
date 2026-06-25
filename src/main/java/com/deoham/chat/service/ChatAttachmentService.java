package com.deoham.chat.service;

import com.deoham.chat.dto.ChatAttachmentPresignRequest;
import com.deoham.chat.dto.ChatAttachmentPresignResponse;
import com.deoham.chat.repository.ChatRoomMemberRepository;
import com.deoham.global.config.S3Properties;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@Service
@RequiredArgsConstructor
public class ChatAttachmentService {

    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    public ChatAttachmentPresignResponse createUploadUrl(UUID roomId, UUID userId, ChatAttachmentPresignRequest request) {
        boolean isMember = chatRoomMemberRepository.existsByChatRoomIdAndUserIdAndLeftAtIsNull(roomId, userId);
        if (!isMember) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "채팅방 멤버가 아닙니다");
        }

        String key = "chat/%s/%s_%s".formatted(roomId, UUID.randomUUID(), request.fileName());

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(s3Properties.bucket())
                .key(key)
                .contentType(request.contentType())
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(r -> r
                .signatureDuration(Duration.ofSeconds(s3Properties.presignedUrlTtlSeconds()))
                .putObjectRequest(putRequest));

        return new ChatAttachmentPresignResponse(presigned.url().toString(), key);
    }
}
