package com.dbtest.liquibase.service;

import java.util.List;
import java.util.Map;

public interface ChangelogService {
	List<Map<String, Object>> getChangelogEntries(String projectKey);
}
