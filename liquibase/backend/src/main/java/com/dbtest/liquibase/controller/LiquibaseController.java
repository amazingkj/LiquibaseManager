package com.dbtest.liquibase.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/liquibase")
public class LiquibaseController {

	@GetMapping("/history")
	public ResponseEntity<String> getLiquibaseHistory() {
		RestTemplate restTemplate = new RestTemplate();
		String liquibaseData = restTemplate.getForObject("http://localhost:18080/actuator/liquibase", String.class);
		return ResponseEntity.ok(liquibaseData);
	}
}