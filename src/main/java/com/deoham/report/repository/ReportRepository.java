package com.deoham.report.repository;

import com.deoham.report.entity.Report;
import com.deoham.report.entity.ReportTarget;
import com.deoham.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    boolean existsByReporterAndTargetTypeAndTargetId(User reporter, ReportTarget targetType, UUID targetId);
}
