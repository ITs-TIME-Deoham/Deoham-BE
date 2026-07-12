package com.deoham.global.logging;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SensitiveDataMaskingEncoder extends PatternLayoutEncoder {

  private static final String MASKED = "****";
  private boolean shouldMask = false;

  @Override
  public void start() {
    super.start();
    String profile = System.getProperty("spring.profiles.active", "local");
    this.shouldMask = "prod".equalsIgnoreCase(profile);
  }

  @Override
  public byte[] encode(ILoggingEvent event) {
    if (shouldMask) {
      String message = event.getMessage();
      if (message != null) {
        event.setMessage(maskSensitiveData(message));
      }
    }
    return super.encode(event);
  }

  private String maskSensitiveData(String message) {
    String masked = message;

    // JWT 토큰 마스킹 (Bearer token)
    masked = masked.replaceAll(
        "(?i)(bearer|authorization)\\s+([\\w.-]+)",
        "$1 " + MASKED
    );

    // API 키 마스킹
    masked = masked.replaceAll(
        "(?i)(api[_-]?key|apikey|api-key)\\s*[=:]\\s*([\\w.-]+)",
        "$1=****"
    );

    // 비밀번호 마스킹
    masked = masked.replaceAll(
        "(?i)(password|passwd|pwd)\\s*[=:]\\s*([^\\s,}\"]+)",
        "$1=****"
    );

    // X-Auth-Token 마스킹
    masked = masked.replaceAll(
        "(?i)(x-auth-token|authorization-token)\\s*[=:]\\s*([\\w.-]+)",
        "$1:****"
    );

    // 신용카드 번호 마스킹
    masked = masked.replaceAll(
        "\\b(?:\\d{4}[-\\s]?){3}\\d{4}\\b",
        "****-****-****-****"
    );

    // 사용자 ID (UUID) → 부분 마스킹 (첫 8자-마지막 4자)
    masked = partialMaskUUID(masked, "userId");
    masked = partialMaskUUID(masked, "sub");
    masked = partialMaskUUID(masked, "principalName");
    masked = partialMaskUUID(masked, "user_id");

    // 이메일 → 부분 표시 (user****@domain.com)
    masked = maskEmail(masked);

    // 전화번호 마스킹 (한국)
    masked = masked.replaceAll(
        "\\b0\\d{1,2}-?\\d{3,4}-?\\d{4}\\b",
        "***-****-****"
    );

    return masked;
  }

  private String partialMaskUUID(String text, String fieldName) {
    Pattern pattern = Pattern.compile(
        "\"" + fieldName + "\"\\s*:\\s*\"([a-f0-9-]{36})\"",
        Pattern.CASE_INSENSITIVE
    );
    Matcher matcher = pattern.matcher(text);
    StringBuffer sb = new StringBuffer();
    while (matcher.find()) {
      String uuid = matcher.group(1);
      // UUID 첫 8자-마지막 4자만 표시 (예: 550e8400-****-****-****-8136)
      String masked = uuid.substring(0, 8) + "-****-****-****-" + uuid.substring(32);
      matcher.appendReplacement(sb, "\"" + fieldName + "\":\"" + masked + "\"");
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  private String maskEmail(String text) {
    Pattern pattern = Pattern.compile(
        "\\b([A-Za-z0-9._%+-]{1,3})[A-Za-z0-9._%+-]*@([A-Za-z0-9.-]+\\.[A-Z|a-z]{2,})\\b"
    );
    Matcher matcher = pattern.matcher(text);
    StringBuffer sb = new StringBuffer();
    while (matcher.find()) {
      String prefix = matcher.group(1);
      String domain = matcher.group(2);
      matcher.appendReplacement(sb, prefix + "****@" + domain);
    }
    matcher.appendTail(sb);
    return sb.toString();
  }
}
