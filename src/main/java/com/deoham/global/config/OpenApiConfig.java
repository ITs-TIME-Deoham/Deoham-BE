package com.deoham.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

	private static final String SECURITY_SCHEME_NAME = "bearerAuth";

	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Deoham API")
						.description("Deoham backend REST API\n\n" +
								"인증이 필요한 API는 `Authorization: Bearer <access_token>` 헤더가 필요합니다.\n" +
								"우측 상단 **Authorize** 버튼에서 토큰을 입력하세요.")
						.version("v0.0.1")
						.contact(new Contact()
								.name("Deoham Team")
								.email("npmtart1224@naver.com")))
				.servers(List.of(
						new Server().url("http://localhost:8080").description("Local"),
						new Server().url("https://api.deoham.com").description("Production")
				))
				.tags(List.of(
						new Tag().name("Card").description("도움 요청 카드 생성·조회·상태 변경"),
						new Tag().name("CardApply").description("카드 신청서 제출·수락·거절")
				))
				.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
				.components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
						new SecurityScheme()
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")
								.description("자체 발급 access token — `Authorization: Bearer <token>`")));
	}
}
