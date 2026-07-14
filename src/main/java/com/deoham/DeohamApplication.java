package com.deoham;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DeohamApplication {

	public static void main(String[] args) {
		SpringApplication.run(DeohamApplication.class, args);
	}

}
