package com.deoham.card.scheduler;

import com.deoham.card.service.CardWriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CardScheduler {

    private final CardWriteService cardWriteService;

    @Scheduled(fixedDelay = 60000)
    public void expireCards() {
        try {
            log.debug("Starting card expiration check...");
            cardWriteService.expireCards();
            log.debug("Card expiration check completed");
        } catch (Exception e) {
            log.error("Error during card expiration check", e);
        }
    }
}
