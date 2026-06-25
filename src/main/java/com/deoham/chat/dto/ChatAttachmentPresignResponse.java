package com.deoham.chat.dto;

public record ChatAttachmentPresignResponse(
        String uploadUrl,
        String attachmentUrl
) {
}
