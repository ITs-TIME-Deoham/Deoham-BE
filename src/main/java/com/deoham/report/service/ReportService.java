package com.deoham.report.service;

import com.deoham.card.entity.CardApplyStatus;
import com.deoham.card.repository.CardApplyRepository;
import com.deoham.chat.repository.ChatRoomRepository;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.report.dto.ReportCreateRequest;
import com.deoham.report.dto.ReportResponse;
import com.deoham.report.entity.Report;
import com.deoham.report.entity.ReportTarget;
import com.deoham.report.repository.ReportRepository;
import com.deoham.user.entity.User;
import com.deoham.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final UserBlockService userBlockService;
    private final ChatRoomRepository chatRoomRepository;
    private final CardApplyRepository cardApplyRepository;

    @Transactional
    public ReportResponse createReport(UUID reporterId, ReportCreateRequest request) {
        User reporter = findUserOrThrow(reporterId, "신고자를 찾을 수 없습니다");
        User reportedUser = findUserOrThrow(request.reportedUserId(), "신고당한 사용자를 찾을 수 없습니다");

        validateNotSelfReport(reporterId, request.reportedUserId());
        validateNotDuplicateReport(reporter, reportedUser);

        Report report = Report.builder()
                .reporter(reporter)
                .reportedUser(reportedUser)
                .targetType(ReportTarget.CHAT)
                .reason(request.reason())
                .description(request.description())
                .build();

        reportRepository.save(report);

        userBlockService.blockUsers(reporter, reportedUser);

        closeChatRoomBetweenUsers(reporterId, request.reportedUserId());

        autoSuspendIfReportsExceed(reportedUser);

        return ReportResponse.from(report);
    }

    private User findUserOrThrow(UUID userId, String errorMessage) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, errorMessage));
    }

    private void validateNotSelfReport(UUID reporterId, UUID reportedUserId) {
        if (reporterId.equals(reportedUserId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "자신을 신고할 수 없습니다");
        }
    }

    private void validateNotDuplicateReport(User reporter, User reportedUser) {
        if (reportRepository.existsByReporterAndReportedUser(reporter, reportedUser)) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 신고한 사용자입니다");
        }
    }

    private void closeChatRoomBetweenUsers(UUID userId1, UUID userId2) {
        cardApplyRepository.findByStatusAndUserPair(CardApplyStatus.ACCEPTED, userId1, userId2)
                .forEach(cardApply -> chatRoomRepository.findByCardId(cardApply.getCard().getId())
                        .ifPresent(chatRoom -> {
                            chatRoom.close();
                            chatRoomRepository.save(chatRoom);
                        }));
    }

    private void autoSuspendIfReportsExceed(User reportedUser) {
        long reportCount = reportRepository.countByReportedUser(reportedUser);
        if (reportCount >= 5) {
            reportedUser.suspend();
            userRepository.save(reportedUser);
        }
    }
}
