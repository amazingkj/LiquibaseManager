package com.dbtest.liquibase.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.dbtest.liquibase.database.entity.DatabaseConnection;
import com.dbtest.liquibase.database.repository.DatabaseConnectionRepository;
import com.dbtest.liquibase.service.ChangelogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChangelogServiceImpl implements ChangelogService {

	private final DatabaseConnectionRepository connectionRepository;
	@Override
	public List<Map<String, Object>> getChangelogEntries(String projectKey) {
		// 프로젝트 키로 연결 정보 직접 조회
		Optional<DatabaseConnection> connectionOpt = connectionRepository.findByProjectKey(projectKey);

		if (connectionOpt.isEmpty()) {
			log.error("프로젝트를 연결할 수 없습니다: {}", projectKey);
			return Collections.emptyList();
		}

		DatabaseConnection connection = connectionOpt.get();
		log.info("프로젝트를 위한 새 DataSource 생성 중: {}", projectKey);

		// DataSource 직접 생성
		DataSource dataSource = DataSourceBuilder.create()
			.url(connection.getUrl())
			.username(connection.getUsername())
			.password(connection.getPassword())
			.driverClassName(connection.getDriverClassName())
			.build();

		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

		try {
			// Liquibase 테이블 이름 사용
			String changelogTable = connection.getChangelogTable();
			log.info("changelog 테이블 쿼리 중: {}", changelogTable);

			// 테이블 이름 동적으로 사용
			String query = String.format("SELECT * FROM %s ORDER BY DATEEXECUTED DESC", changelogTable);
			return jdbcTemplate.queryForList(query);
		} catch (Exception e) {
			log.error("프로젝트 {}의 변경 로그 쿼리 실패: {}", projectKey, e.getMessage(), e);
			return Collections.emptyList();
		}
	}
}
