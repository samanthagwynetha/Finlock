package com.finlock.finlock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FinlockApplication {
	public static void main(String[] args) {
		SpringApplication.run(FinlockApplication.class, args);
	}
}
