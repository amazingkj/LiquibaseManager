package com.dbtest.liquibase.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.stereotype.Service;

import com.dbtest.liquibase.database.entity.DatabaseConnection;
import com.dbtest.liquibase.database.repository.DatabaseConnectionRepository;
import com.dbtest.liquibase.service.DataSourceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSourceServiceImpl implements DataSourceService {

	private final DatabaseConnectionRepository connectionRepository;
	private final Map<String, DataSource> dataSources = new HashMap<>();

	/**
	 * 모든 활성화된 DB 연결에 대한 DataSource 초기화
	 */
	@Override
	public void initializeDataSources() {
		List<DatabaseConnection> connections = connectionRepository.findByActive(true);

		for (DatabaseConnection connection : connections) {
			createDataSource(connection);
		}

		log.info("Initialized {} DataSources for projects", dataSources.size());
	}

	/**
	 * 특정 프로젝트의 DataSource 생성
	 */
	@Override
	public DataSource createDataSource(DatabaseConnection connection) {
		DataSource dataSource = DataSourceBuilder.create()
			.url(connection.getUrl())
			.username(connection.getUsername())
			.password(connection.getPassword())
			.driverClassName(connection.getDriverClassName())
			.build();

		dataSources.put(connection.getProjectKey(), dataSource);
		log.info("Created DataSource for project: {}", connection.getName());

		return dataSource;
	}

	/**
	 * 프로젝트 키로 DataSource 조회
	 */
	@Override
	public Optional<DataSource> getDataSource(String projectKey) {
		// 이미 생성된 DataSource가 있는지 확인
		if (dataSources.containsKey(projectKey)) {
			return Optional.of(dataSources.get(projectKey));
		}

		// DB에서 연결 정보 조회
		return connectionRepository.findByProjectKey(projectKey)
			.map(this::createDataSource);
	}

	/**
	 * 모든 DataSource 목록 조회
	 */
	@Override
	public Map<String, DataSource> getAllDataSources() {
		return dataSources;
	}

	/**
	 * DataSource 제거
	 */
	@Override
	public void removeDataSource(String projectKey) {
		dataSources.remove(projectKey);
		log.info("Removed DataSource for project key: {}", projectKey);
	}
}
