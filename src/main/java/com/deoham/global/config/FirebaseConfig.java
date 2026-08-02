package com.deoham.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

/**
 * Firebase(FCM) 초기화 설정.
 *
 * <p>서비스 계정 키 파일이 존재하고 읽을 수 있을 때만 {@link FirebaseMessaging} 빈을 등록한다.
 * 키가 없는 로컬/CI 환경에서는 빈을 등록하지 않고(그래서 {@code null}) 부팅을 그대로 진행하며,
 * 발송은 {@code FcmSender}가 {@code ObjectProvider}로 부재를 감지해 스킵한다.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(FcmProperties.class)
@RequiredArgsConstructor
public class FirebaseConfig {

	private final FcmProperties properties;

	@Bean
	@Nullable
	public FirebaseMessaging firebaseMessaging() {
		String keyPath = properties.serviceAccountKeyPath();
		if (keyPath == null || keyPath.isBlank()) {
			log.warn("FCM 서비스 계정 키 경로가 설정되지 않아 푸시 발송을 비활성화합니다");
			return null;
		}
		Path path = Path.of(keyPath);
		if (!Files.isReadable(path)) {
			log.warn("FCM 서비스 계정 키를 찾을 수 없어 푸시 발송을 비활성화합니다 [path={}]", keyPath);
			return null;
		}

		try (InputStream credentialStream = Files.newInputStream(path)) {
			FirebaseApp app = firebaseApp(credentialStream);
			log.info("FCM 초기화 완료 [app={}]", app.getName());
			return FirebaseMessaging.getInstance(app);
		} catch (IOException exception) {
			log.error("FCM 초기화 실패 - 푸시 발송을 비활성화합니다 [path={}]", keyPath, exception);
			return null;
		}
	}

	private FirebaseApp firebaseApp(InputStream credentialStream) throws IOException {
		if (!FirebaseApp.getApps().isEmpty()) {
			return FirebaseApp.getInstance();
		}
		FirebaseOptions options = FirebaseOptions.builder()
				.setCredentials(GoogleCredentials.fromStream(credentialStream))
				.build();
		return FirebaseApp.initializeApp(options);
	}
}
