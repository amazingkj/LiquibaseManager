package com.dbtest.liquibase.service;

import java.util.Map;

import com.dbtest.liquibase.vo.QueryExecutionResult;

public interface QueryExecutionService {

	QueryExecutionResult executeQuery(String projectKey, String query, boolean generateChangeset, String tag);
	Map<String, Object> saveAsChangeset(String projectKey, String query, String author, String description, String tag);
}
