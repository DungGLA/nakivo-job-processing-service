package com.nakivo.job_processing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class JobProcessingApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobProcessingApplication.class, args);
	}

}
