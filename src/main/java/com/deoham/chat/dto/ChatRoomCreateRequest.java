package com.deoham.chat.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record ChatRoomCreateRequest(
        UUID projectId,
        @NotEmpty List<UUID> memberUserIds,
        String name
) {
}
