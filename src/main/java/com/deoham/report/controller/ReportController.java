package com.deoham.report.controller;

import com.deoham.report.controller.docs.ReportControllerDocs;
import com.deoham.report.dto.ChatRoomReportCreateRequest;
import com.deoham.report.dto.ReportCreateRequest;
import com.deoham.report.dto.ReportResponse;
import com.deoham.report.service.ReportService;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.global.response.ApiResponse;
import com.deoham.global.security.AuthenticationUtils;
import com.deoham.global.security.AuthPrincipal;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController implements ReportControllerDocs {

    private final ReportService reportService;

    @Override
    @PostMapping
    public ResponseEntity<ReportResponse> createReport(@Valid @RequestBody ReportCreateRequest request) {
        UUID reporterId = currentUserId();
        ReportResponse response = reportService.createReport(reporterId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @PostMapping("/chat-rooms/{roomId}")
    public ResponseEntity<ReportResponse> createReportFromChatRoom(
            @PathVariable UUID roomId,
            @Valid @RequestBody ChatRoomReportCreateRequest request) {
        UUID reporterId = currentUserId();
        ReportResponse response = reportService.createReportFromChatRoom(roomId, reporterId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private UUID currentUserId() {
        AuthPrincipal principal = AuthenticationUtils.currentPrincipal()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        return principal.userId();
    }
}
