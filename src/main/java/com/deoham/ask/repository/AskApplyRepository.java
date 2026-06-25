package com.deoham.ask.repository;

import com.deoham.ask.entity.AskApply;
import com.deoham.ask.entity.AskPost;
import com.deoham.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AskApplyRepository extends JpaRepository<AskApply, UUID> {

    boolean existsByAskAndApplicant(AskPost ask, User applicant);

    List<AskApply> findByAsk(AskPost ask);

    Optional<AskApply> findByAskAndApplicant(AskPost ask, User applicant);
}
