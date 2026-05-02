package com.deoham.card.service;

import com.deoham.card.repository.AiAnalysisRepository;
import com.deoham.card.repository.CardImpactRepository;
import com.deoham.card.repository.CardRepository;
import com.deoham.card.repository.CardShareRepository;
import com.deoham.card.repository.CardViewLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardService {

    private final CardRepository cardRepository;
    private final AiAnalysisRepository aiAnalysisRepository;
    private final CardImpactRepository cardImpactRepository;
    private final CardShareRepository cardShareRepository;
    private final CardViewLogRepository cardViewLogRepository;
}
