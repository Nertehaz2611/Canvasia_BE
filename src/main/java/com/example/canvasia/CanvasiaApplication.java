package com.example.canvasia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CanvasiaApplication {

	public static void main(String[] args) {
		SpringApplication.run(CanvasiaApplication.class, args);
	}

}
