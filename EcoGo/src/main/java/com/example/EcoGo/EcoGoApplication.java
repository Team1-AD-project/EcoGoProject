package com.example.EcoGo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class EcoGoApplication {

	public static void main(String[] args) {
		SpringApplication.run(EcoGoApplication.class, args);
	}

}
