package com.deoham.user.service.impl;

import com.deoham.user.repository.UserRepository;
import com.deoham.user.service.UserWriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserWriteServiceImpl implements UserWriteService {

    private final UserRepository userRepository;
}
