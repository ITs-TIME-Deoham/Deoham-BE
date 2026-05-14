package com.deoham.card.service.impl;

import com.deoham.card.repository.AiAnalysisRepository;
import com.deoham.card.repository.CardImpactRepository;
import com.deoham.card.repository.CardRepository;
import com.deoham.card.repository.CardShareRepository;
import com.deoham.card.repository.CardViewLogRepository;
import com.deoham.card.service.CardReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardReadServiceImpl implements CardReadService {

    private final CardRepository cardRepository;
    private final AiAnalysisRepository aiAnalysisRepository;
    private final CardImpactRepository cardImpactRepository;
    private final CardShareRepository cardShareRepository;
    private final CardViewLogRepository cardViewLogRepository;
}
