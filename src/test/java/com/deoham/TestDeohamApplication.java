package com.deoham;

import org.springframework.boot.SpringApplication;

public class TestDeohamApplication {

	public static void main(String[] args) {
		SpringApplication.from(DeohamApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
