package com.dbtest.liquibase.controller;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dbtest.liquibase.database.entity.DatabaseConnection;
import com.dbtest.liquibase.database.repository.DatabaseConnectionRepository;
import com.dbtest.liquibase.service.DataSourceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/connections")
@RequiredArgsConstructor
public class DatabaseConnectionController {

	private final DatabaseConnectionRepository connectionRepository;
	private final DataSourceService dataSourceService;

	@GetMapping
	public List<DatabaseConnection> getAllConnections() {
		return connectionRepository.findAll();
	}

	@GetMapping("/{id}")
	public ResponseEntity<DatabaseConnection> getConnection(@PathVariable Long id) {
		return connectionRepository.findById(id)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	@PostMapping
	public ResponseEntity<DatabaseConnection> createConnection(@RequestBody DatabaseConnection connection) {
		DatabaseConnection savedConnection = connectionRepository.save(connection);

		// 활성화된 연결이면 즉시 DataSource 생성
		if (connection.isActive()) {
			dataSourceService.createDataSource(savedConnection);
		}

		return ResponseEntity.ok(savedConnection);
	}

	@PutMapping("/{id}")
	public ResponseEntity<DatabaseConnection> updateConnection(
		@PathVariable Long id,
		@RequestBody DatabaseConnection connection) {

		return connectionRepository.findById(id)
			.map(existingConnection -> {
				connection.setId(id);
				DatabaseConnection updatedConnection = connectionRepository.save(connection);

				// DataSource 업데이트 (제거 후 재생성)
				dataSourceService.removeDataSource(updatedConnection.getProjectKey());
				if (updatedConnection.isActive()) {
					dataSourceService.createDataSource(updatedConnection);
				}

				return ResponseEntity.ok(updatedConnection);
			})
			.orElse(ResponseEntity.notFound().build());
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteConnection(@PathVariable Long id) {
		return connectionRepository.findById(id)
			.map(connection -> {
				connectionRepository.delete(connection);
				dataSourceService.removeDataSource(connection.getProjectKey());
				return ResponseEntity.ok().<Void>build();
			})
			.orElse(ResponseEntity.notFound().build());
	}

	@PostMapping("/{id}/test")
	public ResponseEntity<String> testConnection(@PathVariable Long id) {
		return connectionRepository.findById(id)
			.map(connection -> {
				try {
					DataSource dataSource = DataSourceBuilder.create()
						.url(connection.getUrl())
						.username(connection.getUsername())
						.password(connection.getPassword())
						.driverClassName(connection.getDriverClassName())
						.build();

					JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

					try {
						//기본 쿼리 시도
						jdbcTemplate.queryForObject("SELECT 1 FROM DUAL", Integer.class);

						//Liquibase 테이블 존재 여부 확인
						try {
							String query = String.format("SELECT COUNT(*) FROM %s",
								connection.getChangelogTable());
							int count = jdbcTemplate.queryForObject(query, Integer.class);
							return ResponseEntity.ok("연결 성공! " + count +
								"개의 Changelog를 찾았습니다.");
						} catch (Exception e) {
							// Liquibase 테이블이 없을 수 있음
							return ResponseEntity.ok("연결에 성공했지만, Liquibase Changelog 테이블을 찾을 수 없거나 비어 있습니다.");
						}
					} catch (Exception e) {
						return ResponseEntity.ok("연결에 성공했지만, Liquibase 테이블을 확인할 수 없습니다: " + e.getMessage());
					}
				} catch (Exception e) {
					return ResponseEntity.badRequest().body("접속 실패: " + e.getMessage());
				}
			})
			.orElse(ResponseEntity.notFound().build());
	}
}