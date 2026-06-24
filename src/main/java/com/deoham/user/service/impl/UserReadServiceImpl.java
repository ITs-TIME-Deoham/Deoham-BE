package com.deoham.user.service.impl;

import com.deoham.user.repository.UserRepository;
import com.deoham.user.service.UserReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserReadServiceImpl implements UserReadService {

    private final UserRepository userRepository;
}
