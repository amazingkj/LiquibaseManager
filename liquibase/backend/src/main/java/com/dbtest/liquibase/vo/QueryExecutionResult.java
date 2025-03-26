package com.dbtest.liquibase.vo;

import lombok.Data;

@Data
public class QueryExecutionResult {
	private boolean success;
	private String message;
	private Object result;
	private String generatedChangeset;
	private int rowsAffected;
	private long executionTimeMs;
}