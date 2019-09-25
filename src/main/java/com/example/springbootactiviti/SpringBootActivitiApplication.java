package com.example.springbootactiviti;

import org.activiti.spring.boot.SecurityAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = SecurityAutoConfiguration.class)// 兼容activiti
public class SpringBootActivitiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringBootActivitiApplication.class, args);
	}

}
