package com.dbtest.liquibase.service;

import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.Map;

import liquibase.exception.LiquibaseException;

public interface LiquibaseService {

	Map<String, Object> executeChangelogByTag(String projectKey, String changelogPath, String tag) throws
		SQLException,
		FileNotFoundException,
		LiquibaseException;

	Map<String, Object> applyTagToChangelog(String projectKey, String changelogId, String tag) throws Exception;
}
