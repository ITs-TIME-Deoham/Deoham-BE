package com.deoham.user.scheduler;

import com.deoham.user.service.UserWriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserScheduler {

	private final UserWriteService userWriteService;

	@Scheduled(cron = "0 0 2 * * ?")  // 매일 새벽 2시
	public void permanentlyDeleteExpiredUsers() {
		try {
			log.debug("Starting permanent user deletion for users deleted 30+ days ago...");
			userWriteService.deleteExpiredDeletedUsers();
			log.debug("Permanent user deletion completed");
		} catch (Exception e) {
			log.error("Error during permanent user deletion", e);
		}
	}
}
