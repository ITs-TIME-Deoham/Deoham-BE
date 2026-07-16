package com.deoham.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * STOMP(@MessageMapping) 처리 중 발생한 예외의 공통 핸들러.
 * HTTP와 달리 응답 봉투를 돌려줄 수 없으므로 로깅만 수행하고,
 * 예외가 브로커 스레드로 전파되어 세션이 끊기는 것을 막는다.
 * (개별 STOMP 컨트롤러마다 중복 선언하던 @MessageExceptionHandler를 이곳으로 통합)
 */
@Slf4j
@ControllerAdvice
public class StompExceptionHandler {

	@MessageExceptionHandler(BusinessException.class)
	public void handleBusiness(BusinessException ex) {
		log.warn("STOMP BusinessException [{}]: {}", ex.getErrorCode().name(), ex.getMessage());
	}

	@MessageExceptionHandler(Exception.class)
	public void handleUnexpected(Exception ex) {
		log.error("STOMP 처리 중 예기치 않은 예외 발생", ex);
	}
}
