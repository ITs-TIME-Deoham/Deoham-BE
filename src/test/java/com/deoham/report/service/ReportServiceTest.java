package com.deoham.report.service;

import com.deoham.TestcontainersConfiguration;
import com.deoham.global.exception.BusinessException;
import com.deoham.report.dto.ReportCreateRequest;
import com.deoham.report.dto.ReportResponse;
import com.deoham.report.entity.Report;
import com.deoham.report.entity.ReportReason;
import com.deoham.report.repository.ReportRepository;
import com.deoham.report.repository.UserBlockRepository;
import com.deoham.user.entity.User;
import com.deoham.user.entity.UserStatus;
import com.deoham.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class ReportServiceTest {

    @Autowired ReportService reportService;
    @Autowired UserRepository userRepository;
    @Autowired ReportRepository reportRepository;
    @Autowired UserBlockRepository userBlockRepository;

    @Test
    void testCreateReport_Success() {
        User reporter = savedUser("reporter@test.com", "reporter-uid");
        User reportedUser = savedUser("reported@test.com", "reported-uid");

        ReportCreateRequest request = new ReportCreateRequest(
                reportedUser.getId(),
                ReportReason.HARASSMENT,
                "욕설과 협박 메시지를 보냈습니다"
        );

        ReportResponse response = reportService.createReport(reporter.getId(), request);

        assertThat(response.reportId()).isNotNull();
        assertThat(response.reportedUserId()).isEqualTo(reportedUser.getId());
        assertThat(response.reason()).isEqualTo(ReportReason.HARASSMENT);
    }

    @Test
    void testCreateReport_CreatesUserBlocks() {
        User reporter = savedUser("reporter2@test.com", "reporter2-uid");
        User reportedUser = savedUser("reported2@test.com", "reported2-uid");

        ReportCreateRequest request = new ReportCreateRequest(
                reportedUser.getId(),
                ReportReason.OBSCENE_CONTENT,
                "부적절한 사진"
        );

        reportService.createReport(reporter.getId(), request);

        boolean blockedByReporter = userBlockRepository.existsByBlockerAndBlocked(reporter, reportedUser);
        boolean blockedByReported = userBlockRepository.existsByBlockerAndBlocked(reportedUser, reporter);

        assertThat(blockedByReporter).isTrue();
        assertThat(blockedByReported).isTrue();
    }

    @Test
    void testCreateReport_AutoSuspend_When5Reports() {
        User targetUser = savedUser("target@test.com", "target-uid");

        for (int i = 0; i < 5; i++) {
            User tempReporter = savedUser("temp" + i + "@test.com", "temp-uid-" + i);
            ReportCreateRequest request = new ReportCreateRequest(
                    targetUser.getId(),
                    ReportReason.HARASSMENT,
                    "신고 " + (i + 1)
            );
            reportService.createReport(tempReporter.getId(), request);
        }

        User updated = userRepository.findById(targetUser.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(UserStatus.SUSPENDED);
    }

    @Test
    void testCreateReport_NoSuspend_When4Reports() {
        User targetUser = savedUser("target4@test.com", "target4-uid");

        for (int i = 0; i < 4; i++) {
            User tempReporter = savedUser("temp4_" + i + "@test.com", "temp4-uid-" + i);
            ReportCreateRequest request = new ReportCreateRequest(
                    targetUser.getId(),
                    ReportReason.HARASSMENT,
                    "신고 " + (i + 1)
            );
            reportService.createReport(tempReporter.getId(), request);
        }

        User updated = userRepository.findById(targetUser.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void testCreateReport_CannotReportSelf() {
        User reporter = savedUser("reporter_self@test.com", "reporter_self-uid");

        ReportCreateRequest request = new ReportCreateRequest(
                reporter.getId(),
                ReportReason.HARASSMENT,
                "자신 신고"
        );

        assertThatThrownBy(() -> reportService.createReport(reporter.getId(), request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void testCreateReport_AlreadyReported() {
        User reporter = savedUser("reporter_dup@test.com", "reporter_dup-uid");
        User reportedUser = savedUser("reported_dup@test.com", "reported_dup-uid");

        ReportCreateRequest request = new ReportCreateRequest(
                reportedUser.getId(),
                ReportReason.HARASSMENT,
                "첫 번째 신고"
        );

        reportService.createReport(reporter.getId(), request);

        ReportCreateRequest duplicateRequest = new ReportCreateRequest(
                reportedUser.getId(),
                ReportReason.OBSCENE_CONTENT,
                "두 번째 신고"
        );

        assertThatThrownBy(() -> reportService.createReport(reporter.getId(), duplicateRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessage("이미 신고한 사용자입니다");
    }

    @Test
    void testCreateReport_UserNotFound() {
        User reporter = savedUser("reporter_notfound@test.com", "reporter_notfound-uid");
        UUID nonExistentUserId = UUID.randomUUID();

        ReportCreateRequest request = new ReportCreateRequest(
                nonExistentUserId,
                ReportReason.HARASSMENT,
                "없는 사용자 신고"
        );

        assertThatThrownBy(() -> reportService.createReport(reporter.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("신고당한 사용자를 찾을 수 없습니다");
    }

    private User savedUser(String email, String uid) {
        return userRepository.save(User.builder()
                .firebaseUid(uid)
                .nickname(uid + "_nickname")
                .build());
    }
}
