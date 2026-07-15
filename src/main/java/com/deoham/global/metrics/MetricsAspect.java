package com.deoham.global.metrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
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
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    MetricEndpoint metricEndpoint = signature.getMethod().getAnnotation(MetricEndpoint.class);
    if (metricEndpoint != null) {
      return metricEndpoint.value();
    }
    return signature.getName().toLowerCase();
  }

  private void logMetric(String event, String endpoint, int statusCode) {
    log.debug("Metric recorded: {} - Endpoint: {} - Status: {}", event, endpoint, statusCode);
  }

  private void logMetric(String event, String endpoint, Exception e) {
    log.debug("Metric recorded: {} - Endpoint: {} - Error: {}", event, endpoint, e.getClass().getSimpleName());
  }
}
