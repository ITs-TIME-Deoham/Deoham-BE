package com.deoham.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	private static final String SECURITY_SCHEME_NAME = "bearerAuth";

	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Deoham API")
						.description("Deoham backend REST API\n\n" +
								"**인증 방식**\n" +
								"카카오 로그인(`/api/auth/kakao` → `/api/auth/kakao/callback`) 후 서버가 발급한 " +
									"자체 JWT(HS256)를 `Authorization: Bearer <access token>` 헤더로 전달해야 합니다.\n" +
									"인증 없이 호출 가능한 엔드포인트: `/api/auth/kakao`, `/api/auth/kakao/callback`, `/api/auth/refresh`.\n" +
								"우측 상단 **Authorize** 버튼에서 토큰을 입력하세요.\n\n" +
								"**보안 헤더**\n" +
								"모든 응답에는 다음 보안 헤더가 포함됩니다:\n" +
								"- `Content-Security-Policy: default-src 'self'` — 같은 도메인의 리소스만 로드\n" +
								"- `X-Frame-Options: DENY` — clickjacking 공격 방지\n" +
								"프론트엔드가 외부 CDN이나 S3 리소스를 사용하려면 사전 협의 필요\n\n" +
								"**토큰 갱신 정책**\n" +
								"`/api/auth/refresh` 요청 시 매번 새로운 RefreshToken을 발급합니다 (토큰 탈취 시 재사용 불가).\n" +
								"응답 헤더의 Authorization 토큰과 쿠키의 RefreshToken을 항상 업데이트하세요.")
						.version("v0.0.1")
						.contact(new Contact()
								.name("Deoham Team")
								.email("npmtart1224@naver.com")))
				.tags(List.of(
						new Tag().name("Card").description("도움 요청 카드 생성·조회·상태 변경"),
						new Tag().name("CardApply").description("매칭 신청·수락·거절")
				))
				.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
				.components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
						new SecurityScheme()
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")
								.description("서버가 발급한 액세스 토큰(자체 HS256 JWT) — `Authorization: Bearer <token>`")));
	}
}
