package com.deoham.report.service;

import com.deoham.report.entity.UserBlock;
import com.deoham.report.repository.UserBlockRepository;
import com.deoham.user.entity.User;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserBlockService {

    private final UserBlockRepository userBlockRepository;

    @Transactional
    public void blockUsers(User user1, User user2) {
        userBlockRepository.save(UserBlock.builder().blocker(user1).blocked(user2).build());
        userBlockRepository.save(UserBlock.builder().blocker(user2).blocked(user1).build());
    }
}
