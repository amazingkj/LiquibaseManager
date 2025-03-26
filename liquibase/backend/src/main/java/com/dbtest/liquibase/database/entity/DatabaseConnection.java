package com.dbtest.liquibase.database.entity;

import lombok.*;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseConnection {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;
	private String projectKey;
	private String url;
	private String username;
	private String password;
	private String driverClassName;
	private boolean active = true;

	// Liquibase 테이블 정보
	private String changelogTable = "DATABASECHANGELOG";
	private String changelogLockTable = "DATABASECHANGELOGLOCK";
}