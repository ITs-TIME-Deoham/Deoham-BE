package com.deoham.global.response;

import com.deoham.global.exception.ErrorCode;

public record ApiResponse<T>(
		boolean success,
		T data,
		ErrorBody error
) {
	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>(true, data, null);
	}

	public static ApiResponse<Void> ok() {
		return new ApiResponse<>(true, null, null);
	}

	public static ApiResponse<Void> fail(ErrorCode code, String message) {
		return new ApiResponse<>(false, null, new ErrorBody(code.name(), message));
	}

	public record ErrorBody(String code, String message) {
	}
}
