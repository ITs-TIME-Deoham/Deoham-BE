package com.deoham.project.service.impl;

import com.deoham.project.repository.ContactRepository;
import com.deoham.project.repository.ProjectRepository;
import com.deoham.project.service.ProjectReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectReadServiceImpl implements ProjectReadService {

    private final ProjectRepository projectRepository;
    private final ContactRepository contactRepository;
}
