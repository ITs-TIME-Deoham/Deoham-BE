package com.deoham.global.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "deoham.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
