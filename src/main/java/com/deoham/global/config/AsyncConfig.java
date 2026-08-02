package com.deoham.global.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 비동기 실행 인프라. 커밋 후 FCM 발송처럼 비즈니스 트랜잭션을 블로킹하면 안 되는 작업을
 * 별도 스레드 풀에서 처리하기 위한 전용 {@link Executor}를 등록한다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

	@Bean(name = "fcmTaskExecutor")
	public Executor fcmTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(4);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix("fcm-");
		executor.initialize();
		return executor;
	}
}
