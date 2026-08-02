package com.tuning.oasystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OaSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(OaSystemApplication.class, args);
		System.out.println("oa-system 启动成功");
		System.out.println("http://localhost:8080");
		System.out.println("http://localhost:8080/swagger-ui.html");
	}

}
