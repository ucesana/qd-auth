package com.qdauth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class QdAuthApplication {

	public static void main(String[] args) {
		SpringApplication.run(QdAuthApplication.class, args);
	}

}
