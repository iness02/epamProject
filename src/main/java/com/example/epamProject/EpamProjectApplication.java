package com.example.epamProject;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
@OpenAPIDefinition(
		info = @Info(
			title= "Pharmacy Management",
				version = "1.0.0"
		)
)
public class EpamProjectApplication {

	public static void main(String[] args) {

		SpringApplication.run(EpamProjectApplication.class, args);
	}

}
