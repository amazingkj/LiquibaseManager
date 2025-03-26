package com.dbtest.liquibase.utils;

import java.sql.Connection;

import com.dbtest.liquibase.database.entity.DatabaseConnection;

public class DatabaseType {

	public static final String DB_TYPE_GENERIC = "generic";
	public static final String DB_TYPE_ORACLE = "oracle";
	public static final String DB_TYPE_MYSQL = "mysql";
	public static final String DB_TYPE_POSTGRESQL = "postgresql";
	public static final String DB_TYPE_TIBERO = "tibero";
	public static final String DB_TYPE_H2 = "h2";


	public static String extractDbTypeFromDriverClassName(String driverClassName) {
		if (driverClassName == null || driverClassName.isEmpty()) {
			return DB_TYPE_GENERIC;
		}

		String lowerCaseDriver = driverClassName.toLowerCase();

		return getString(lowerCaseDriver);
	}


	public static String extractDbTypeFromJdbcConnection(Connection connection) {
		if (connection == null) {
			return DB_TYPE_GENERIC;
		}

		try {
			String productName = connection.getMetaData().getDatabaseProductName().toLowerCase();

			return getString(productName);
		} catch (Exception e) {
			return DB_TYPE_GENERIC;
		}
	}


	public static String extractDbTypeFromConnection(DatabaseConnection connection) {
		if (connection == null) {
			return DB_TYPE_GENERIC;
		}

		try {
			String driverClassName = connection.getDriverClassName();

			return getString(driverClassName);
		} catch (Exception e) {
			return DB_TYPE_GENERIC;
		}
	}


	private static String getString(String driverClassName) {
		if (driverClassName.contains("oracle")) {
			return DB_TYPE_ORACLE;
		} else if (driverClassName.contains("mysql")) {
			return DB_TYPE_MYSQL;
		} else if (driverClassName.contains("postgresql")) {
			return DB_TYPE_POSTGRESQL;
		} else if (driverClassName.contains("tibero")) {
			return DB_TYPE_TIBERO;
		} else if (driverClassName.contains("h2")) {
			return DB_TYPE_H2;
		}

		return DB_TYPE_GENERIC;
	}
}
