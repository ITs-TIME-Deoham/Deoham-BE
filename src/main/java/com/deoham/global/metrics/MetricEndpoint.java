package com.deoham.global.metrics;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Explicit metric label for a controller method, read by {@link MetricsAspect}.
 * Methods without this annotation fall back to their lowercased method name.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MetricEndpoint {
  String value();
}