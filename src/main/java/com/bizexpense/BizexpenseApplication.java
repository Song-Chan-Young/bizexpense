package com.bizexpense;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BizexpenseApplication {

	public static void main(String[] args) {
		SpringApplication.run(BizexpenseApplication.class, args);
	}

}
