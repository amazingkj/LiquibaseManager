package com.dbtest.liquibase.service;

import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import com.dbtest.liquibase.database.entity.DatabaseConnection;

public interface DataSourceService {


	/**
	 * 특정 프로젝트의 DataSource 생성
	 */
	DataSource createDataSource(DatabaseConnection connection);

	/**
	 * 프로젝트 키로 DataSource 조회
	 */
	Optional<DataSource> getDataSource(String projectKey);

	/**
	 * 모든 DataSource 목록 조회
	 */
	Map<String, DataSource> getAllDataSources();

	/**
	 * DataSource 제거
	 */
	void removeDataSource(String projectKey);

	/**
	 * 모든 활성화된 DB 연결에 대한 DataSource 초기화
	 */
	void initializeDataSources();
}