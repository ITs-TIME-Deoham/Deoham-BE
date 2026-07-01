package com.deoham.report.repository;

import com.deoham.report.entity.Report;
import com.deoham.report.entity.ReportTarget;
import com.deoham.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Report r " +
           "WHERE r.reporter = :reporter AND r.targetType = :targetType " +
           "AND (r.reportedUser.id = :targetId OR r.reportedCard.id = :targetId)")
    boolean existsByReporterAndTargetTypeAndTargetId(
            @Param("reporter") User reporter,
            @Param("targetType") ReportTarget targetType,
            @Param("targetId") UUID targetId);
}
