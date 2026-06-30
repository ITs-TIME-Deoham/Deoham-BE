package com.deoham.report.repository;

import com.deoham.report.entity.UserBlock;
import com.deoham.report.entity.UserBlockId;
import com.deoham.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserBlockRepository extends JpaRepository<UserBlock, UserBlockId> {

    List<UserBlock> findByBlocker(User blocker);

    boolean existsByBlockerAndBlocked(User blocker, User blocked);
}
