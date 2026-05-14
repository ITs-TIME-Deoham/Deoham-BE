package com.deoham.project.service.impl;

import com.deoham.project.repository.ContactRepository;
import com.deoham.project.repository.ProjectRepository;
import com.deoham.project.service.ProjectWriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectWriteServiceImpl implements ProjectWriteService {

    private final ProjectRepository projectRepository;
    private final ContactRepository contactRepository;
}
