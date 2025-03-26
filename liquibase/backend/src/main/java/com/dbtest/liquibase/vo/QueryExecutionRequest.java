package com.dbtest.liquibase.vo;


import lombok.Data;

@Data
public class QueryExecutionRequest {
	private String query;
	private boolean generateChangeset;
	private String author;
	private String description;
	private String tag;
}


