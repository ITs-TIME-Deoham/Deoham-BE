package com.deoham.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Invalid request"),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication required"),
	FORBIDDEN(HttpStatus.FORBIDDEN, "Access denied"),
	NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
	CONFLICT(HttpStatus.CONFLICT, "Resource conflict"),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");

	private final HttpStatus status;
	private final String defaultMessage;
}
