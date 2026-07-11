package com.deoham.report.controller;

import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.report.dto.ReportCreateRequest;
import com.deoham.report.dto.ReportResponse;
import com.deoham.report.entity.ReportReason;
import com.deoham.report.service.ReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@DisplayName("신고 컨트롤러 테스트")
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReportService reportService;

    @Test
    @DisplayName("신고 접수 - 성공 (201 Created)")
    void testCreateReport_Success_201() throws Exception {
        UUID reporterId = UUID.randomUUID();
        UUID reportedUserId = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();

        ReportCreateRequest request = new ReportCreateRequest(
                reportedUserId,
                ReportReason.HARASSMENT,
                "욕설과 협박 메시지를 보냈습니다"
        );

        ReportResponse response = new ReportResponse(
                reportId,
                reportedUserId,
                ReportReason.HARASSMENT,
                "욕설과 협박 메시지를 보냈습니다",
                Instant.now()
        );

        when(reportService.createReport(eq(reporterId), any(ReportCreateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/reports")
                .with(jwt().jwt(jwt -> jwt
                        .subject(reporterId.toString())
                        .claim("role", "USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportId").exists())
                .andExpect(jsonPath("$.reportedUserId").value(reportedUserId.toString()))
                .andExpect(jsonPath("$.reason").value("HARASSMENT"))
                .andExpect(jsonPath("$.description").value("욕설과 협박 메시지를 보냈습니다"));
    }

    @Test
    @DisplayName("신고 접수 - 필수 필드 누락 (400 Bad Request)")
    void testCreateReport_MissingDescription_400() throws Exception {
        UUID reporterId = UUID.randomUUID();
        UUID reportedUserId = UUID.randomUUID();

        String requestBody = objectMapper.writeValueAsString(new ReportCreateRequest(
                reportedUserId,
                ReportReason.HARASSMENT,
                null
        ));

        mockMvc.perform(post("/api/reports")
                .with(jwt().jwt(jwt -> jwt
                        .subject(reporterId.toString())
                        .claim("role", "USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("신고 접수 - 인증 없음")
    void testCreateReport_NoAuth() throws Exception {
        UUID reportedUserId = UUID.randomUUID();

        ReportCreateRequest request = new ReportCreateRequest(
                reportedUserId,
                ReportReason.HARASSMENT,
                "욕설"
        );

        mockMvc.perform(post("/api/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("신고 접수 - 자신을 신고 (400 Bad Request)")
    void testCreateReport_ReportSelf_400() throws Exception {
        UUID userId = UUID.randomUUID();

        ReportCreateRequest request = new ReportCreateRequest(
                userId,
                ReportReason.HARASSMENT,
                "자신 신고"
        );

        when(reportService.createReport(eq(userId), any(ReportCreateRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.INVALID_REQUEST, "자신을 신고할 수 없습니다"));

        mockMvc.perform(post("/api/reports")
                .with(jwt().jwt(jwt -> jwt
                        .subject(userId.toString())
                        .claim("role", "USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.message").value("자신을 신고할 수 없습니다"));
    }

    @Test
    @DisplayName("신고 접수 - 중복 신고 (409 Conflict)")
    void testCreateReport_Duplicate_409() throws Exception {
        UUID reporterId = UUID.randomUUID();
        UUID reportedUserId = UUID.randomUUID();

        ReportCreateRequest request = new ReportCreateRequest(
                reportedUserId,
                ReportReason.HARASSMENT,
                "신고"
        );

        when(reportService.createReport(eq(reporterId), any(ReportCreateRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.CONFLICT, "이미 신고한 사용자입니다"));

        mockMvc.perform(post("/api/reports")
                .with(jwt().jwt(jwt -> jwt
                        .subject(reporterId.toString())
                        .claim("role", "USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                .andExpect(jsonPath("$.error.message").value("이미 신고한 사용자입니다"));
    }

    @Test
    @DisplayName("신고 접수 - 사용자 없음 (404 Not Found)")
    void testCreateReport_UserNotFound_404() throws Exception {
        UUID reporterId = UUID.randomUUID();
        UUID reportedUserId = UUID.randomUUID();

        ReportCreateRequest request = new ReportCreateRequest(
                reportedUserId,
                ReportReason.HARASSMENT,
                "신고"
        );

        when(reportService.createReport(eq(reporterId), any(ReportCreateRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "신고당한 사용자를 찾을 수 없습니다"));

        mockMvc.perform(post("/api/reports")
                .with(jwt().jwt(jwt -> jwt
                        .subject(reporterId.toString())
                        .claim("role", "USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("신고당한 사용자를 찾을 수 없습니다"));
    }

    @Test
    @DisplayName("신고 접수 - 설명 길이 초과 (400 Bad Request)")
    void testCreateReport_DescriptionTooLong_400() throws Exception {
        UUID reporterId = UUID.randomUUID();
        UUID reportedUserId = UUID.randomUUID();

        String longDescription = "a".repeat(501);

        ReportCreateRequest request = new ReportCreateRequest(
                reportedUserId,
                ReportReason.HARASSMENT,
                longDescription
        );

        mockMvc.perform(post("/api/reports")
                .with(jwt().jwt(jwt -> jwt
                        .subject(reporterId.toString())
                        .claim("role", "USER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
