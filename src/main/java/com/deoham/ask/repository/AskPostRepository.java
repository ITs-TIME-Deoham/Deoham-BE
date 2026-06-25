package com.deoham.ask.repository;

import com.deoham.ask.entity.AskPost;
import com.deoham.ask.entity.AskStatus;
import com.deoham.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AskPostRepository extends JpaRepository<AskPost, UUID> {

    List<AskPost> findByAuthorAndStatusIn(User author, List<AskStatus> statuses);
}
