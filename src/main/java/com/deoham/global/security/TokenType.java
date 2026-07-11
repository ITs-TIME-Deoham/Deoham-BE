package com.deoham.global.security;

import java.util.Locale;

public enum TokenType {
	ACCESS,
	REFRESH;

	public String claimValue() {
		return name().toLowerCase(Locale.ROOT);
	}
}
