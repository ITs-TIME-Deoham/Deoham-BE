package com.deoham.global.metrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class MetricsAspect {

  private final MetricsRegistry metricsRegistry;

  // ============ PointCut: 모든 컨트롤러 메서드 ============


  @AfterReturning(pointcut = "execution(* com.deoham.*.controller.*Controller.*(..))", returning = "result")
  public void afterControllerSuccess(JoinPoint joinPoint, Object result) {
    try {
      String endpoint = extractEndpointName(joinPoint);
      String status = "2xx";

      metricsRegistry.incrementApiCallCount(endpoint, status);

      logMetric("Controller Success", endpoint, 200);
    } catch (Exception e) {
      log.debug("Failed to process success metrics", e);
    }
  }

  @AfterThrowing(pointcut = "execution(* com.deoham.*.controller.*Controller.*(..))", throwing = "e")
  public void afterControllerFailure(JoinPoint joinPoint, Exception e) {
    try {
      String endpoint = extractEndpointName(joinPoint);
      String errorCode = e.getClass().getSimpleName();

      metricsRegistry.incrementApiErrorCount(endpoint, errorCode);

      logMetric("Controller Failure", endpoint, e);
    } catch (Exception ex) {
      log.debug("Failed to process failure metrics", ex);
    }
  }

  // ============ Helper Methods ============

  private int extractStatusCode(Object result) {
    if (result instanceof ResponseEntity<?> responseEntity) {
      return responseEntity.getStatusCode().value();
    }
    return 200;
  }

  private String extractEndpointName(JoinPoint joinPoint) {
    String methodName = joinPoint.getSignature().getName();
    String className = joinPoint.getTarget().getClass().getSimpleName();

    // CardController.getCardDetail → card.detail
    if (className.contains("Card")) {
      if (methodName.toLowerCase().contains("search")) return "card.search";
      if (methodName.toLowerCase().contains("detail")) return "card.detail";
      if (methodName.toLowerCase().contains("create")) return "card.create";
      if (methodName.toLowerCase().contains("update")) return "card.update";
      return "card.other";
    }

    // UserController.getProfile → user.profile
    if (className.contains("User")) {
      if (methodName.toLowerCase().contains("profile")) return "user.profile";
      if (methodName.toLowerCase().contains("delete")) return "user.delete";
      return "user.other";
    }

    // AuthController.kakaoCallback → auth.kakao
    if (className.contains("Auth")) {
      if (methodName.toLowerCase().contains("kakao")) return "auth.kakao";
      if (methodName.toLowerCase().contains("refresh")) return "auth.refresh";
      if (methodName.toLowerCase().contains("logout")) return "auth.logout";
      return "auth.other";
    }

    // ChatMessageController.sendMessage → chat.message
    if (className.contains("ChatMessage")) {
      if (methodName.toLowerCase().contains("send")) return "chat.message.send";
      if (methodName.toLowerCase().contains("get")) return "chat.message.get";
      return "chat.message.other";
    }

    // ChatRoomController → chat.room
    if (className.contains("ChatRoom")) {
      if (methodName.toLowerCase().contains("create")) return "chat.room.create";
      if (methodName.toLowerCase().contains("get")) return "chat.room.get";
      return "chat.room.other";
    }

    // NotificationController → notification
    if (className.contains("Notification")) {
      return "notification";
    }

    return methodName.toLowerCase();
  }

  private void logMetric(String event, String endpoint, int statusCode) {
    log.debug("Metric recorded: {} - Endpoint: {} - Status: {}", event, endpoint, statusCode);
  }

  private void logMetric(String event, String endpoint, Exception e) {
    log.debug("Metric recorded: {} - Endpoint: {} - Error: {}", event, endpoint, e.getClass().getSimpleName());
  }
}
