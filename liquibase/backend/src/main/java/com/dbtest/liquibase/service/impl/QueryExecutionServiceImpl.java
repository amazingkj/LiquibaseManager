package com.dbtest.liquibase.service.impl;

import static com.dbtest.liquibase.utils.DatabaseType.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Service;

import com.dbtest.liquibase.database.entity.DatabaseConnection;
import com.dbtest.liquibase.database.repository.DatabaseConnectionRepository;
import com.dbtest.liquibase.service.QueryExecutionService;
import com.dbtest.liquibase.vo.QueryExecutionResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryExecutionServiceImpl implements QueryExecutionService {
	private final DatabaseConnectionRepository connectionRepository;
	@Override
	public QueryExecutionResult executeQuery(String projectKey, String query, boolean generateChangeset, String tag) {
		QueryExecutionResult result = new QueryExecutionResult();
		long startTime = System.currentTimeMillis();

		Optional<DatabaseConnection> connectionOpt = connectionRepository.findByProjectKey(projectKey);
		if (connectionOpt.isEmpty()) {
			result.setSuccess(false);
			result.setMessage("Database connection not found for project: " + projectKey);
			return result;
		}

		DatabaseConnection connection = connectionOpt.get();
		DataSource dataSource = createDataSource(connection);
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

		try {
			// 세미콜론 제거
			String cleanQuery = removeSemicolon(query);

			// DML 또는 DDL 쿼리인지 확인
			if (isDML(cleanQuery) || isDDL(cleanQuery)) {
				int rowsAffected = jdbcTemplate.update(cleanQuery);
				result.setSuccess(true);
				result.setRowsAffected(rowsAffected);
				result.setMessage("Query executed successfully. Rows affected: " + rowsAffected);

				String changesetId = generateChangesetId();

				// Changeset 생성 요청이 있는 경우
				if (generateChangeset) {
					String changeset = generateSqlChangesetWithTag(query, "system", changesetId,"Auto-generated changeset", tag);
					result.setGeneratedChangeset(changeset);
				}
			} else {
				// SELECT 쿼리인 경우
				List<Map<String, Object>> queryResult = jdbcTemplate.queryForList(cleanQuery);
				result.setSuccess(true);
				result.setResult(queryResult);
				result.setMessage("Query executed successfully. Returned " + queryResult.size() + " rows.");
			}
		} catch (Exception e) {
			result.setSuccess(false);
			result.setMessage("Error executing query: " + e.getMessage());
			log.error("Error executing query for project {}: {}", projectKey, e.getMessage(), e);
		} finally {
			result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
			try {
				((SingleConnectionDataSource)dataSource).destroy();
			} catch (Exception e) {
				log.warn("Error closing datasource", e);
			}
		}

		return result;
	}

	@Override
	public Map<String, Object> saveAsChangeset(String projectKey, String query, String author, String description,
		String tag) {
		Optional<DatabaseConnection> connectionOpt = connectionRepository.findByProjectKey(projectKey);
		if (connectionOpt.isEmpty()) {
			throw new RuntimeException("Database connection not found for project: " + projectKey);
		}

		try {
			DatabaseConnection connection = connectionOpt.get();
			String dbType = extractDbTypeFromConnection(connection);

			// 고유한 changeset ID 생성
			String changesetId = generateChangesetId();

			// SQL 형식의 Liquibase changeset 생성 (태그 포함)
			String changesetContent = generateSqlChangesetWithTag(query, author, changesetId, description, tag);

			// 파일 경로 생성
			String fileName = projectKey.toLowerCase() + "-" +
				LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".sql";


			// 물리적 파일시스템 경로 (실제 파일 저장 위치)
			String physicalPath = "liquibase/backend/src/main/resources/db/changelog/" +
				projectKey.toLowerCase() + "/" +
				dbType + "/";

			// 디렉토리가 존재하지 않으면 생성
			File directory = new File(physicalPath);
			if (!directory.exists()) {
				boolean created = directory.mkdirs();
				if (!created) {
					log.warn("Failed to create directory: {}", physicalPath);
				}
			}

			String fullPath = physicalPath + fileName;

			// 파일에 changeset 저장
			try (FileWriter writer = new FileWriter(fullPath)) {
				writer.write(changesetContent);
				log.info("Changeset saved to file: {}", fullPath);

				// 파일이 실제로 존재하는지 즉시 확인
				File savedFile = new File(fullPath);
				log.info("File exists after save: {}", savedFile.exists());
			}


			// 마스터 changelog 확인
			updateMasterChangelogForSql(projectKey, "");

			// API 응답에 필요한 경로는 리소스 기준 상대 경로
			String apiResponsePath = "db/changelog/" +
				projectKey.toLowerCase() + "/" +
				dbType + "/" +
				fileName;

			Map<String, Object> result = new HashMap<>();
			result.put("changesetId", changesetId);
			result.put("changelogPath", apiResponsePath);
			result.put("tag", tag);
			result.put("message", "Changeset 저장 성공");

			return result;
		} catch (Exception e) {
			log.error("Changeset 저장 실패 {}: {}", projectKey, e.getMessage(), e);
			throw new RuntimeException("Changeset 저장 실패: " + e.getMessage(), e);
		}
	}
	/**
	 * SQL 형식의 Liquibase changeset 생성
	 */
	// 태그를 포함한 SQL Changeset 생성
	private String generateSqlChangesetWithTag(String query, String author, String changesetId,
		String description, String tag) {
		StringBuilder sb = new StringBuilder();

		// Liquibase SQL 형식의 헤더
		sb.append("--liquibase formatted sql\n\n");

		// changeset 정의
		sb.append("--changeset ").append(author).append(":").append(changesetId).append("\n");

		// 설명(코멘트) 추가
		sb.append("--comment: ").append(description).append("\n");

		if (tag != null) {
			// 태그 추가
			sb.append("--tagDatabase: ").append(tag).append("\n");

		}

		// 쿼리 추가 (세미콜론이 없으면 추가)
		sb.append(query);
		if (!query.trim().endsWith(";")) {
			sb.append(";");
		}

		sb.append("\n");

		return sb.toString();
	}

	/**
	 * SQL 파일을 위한 마스터 changelog 업데이트
	 */
	private void updateMasterChangelogForSql(String projectKey, String changelogPath) {
		// 마스터 changelog 경로
		String masterChangelogPath = "liquibase/backend/src/main/resources/db/changelog/db.changelog-master.yaml";
		File masterChangelogFile = new File(masterChangelogPath);

		// 마스터 파일이 없으면 생성
		if (!masterChangelogFile.exists()) {
			// 마스터 파일 디렉토리 생성
			File masterDir = masterChangelogFile.getParentFile();
			if (!masterDir.exists()) {
				boolean created = masterDir.mkdirs();
				if (!created) {
					log.warn("Failed to create directory for master changelog: {}", masterDir.getPath());
				}
			}

			try (FileWriter writer = new FileWriter(masterChangelogFile)) {
				writer.write("databaseChangeLog:\n");
				writer.write("  - includeAll:\n");
				writer.write("      path: db/changelog/${project}/${db.type}\n");
			} catch (IOException e) {
				log.error("Error creating master changelog file: {}", e.getMessage(), e);
			}

			log.info("Created YAML master changelog file with includeAll path pattern");
		} else {
			log.info("Master changelog file already exists, no update needed because of includeAll pattern");
		}
	}

	// 세미콜론 제거하는 메서드
	private String removeSemicolon(String query) {
		if (query == null)
			return "";
		String trimmed = query.trim();
		if (trimmed.endsWith(";")) {
			return trimmed.substring(0, trimmed.length() - 1);
		}
		return trimmed;
	}

	private DataSource createDataSource(DatabaseConnection connection) {
		SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
		dataSource.setUrl(connection.getUrl());
		dataSource.setUsername(connection.getUsername());
		dataSource.setPassword(connection.getPassword());
		dataSource.setDriverClassName(connection.getDriverClassName());
		return dataSource;
	}

	private boolean isDML(String query) {
		String upperQuery = query.trim().toUpperCase();
		return upperQuery.startsWith("INSERT") ||
			upperQuery.startsWith("UPDATE") ||
			upperQuery.startsWith("DELETE") ||
			upperQuery.startsWith("MERGE");
	}

	private boolean isDDL(String query) {
		String upperQuery = query.trim().toUpperCase();
		return upperQuery.startsWith("CREATE") ||
			upperQuery.startsWith("ALTER") ||
			upperQuery.startsWith("DROP") ||
			upperQuery.startsWith("TRUNCATE");
	}

	private String generateChangesetId() {
		return UUID.randomUUID().toString().substring(0, 8);
	}
}
