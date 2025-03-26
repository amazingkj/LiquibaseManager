package com.dbtest.liquibase.service.impl;

import static com.dbtest.liquibase.utils.DatabaseType.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.dbtest.liquibase.database.entity.DatabaseConnection;
import com.dbtest.liquibase.database.repository.DatabaseConnectionRepository;
import com.dbtest.liquibase.service.LiquibaseService;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.DirectoryResourceAccessor;
import liquibase.resource.ResourceAccessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class LiquibaseServiceImpl implements LiquibaseService {
	private final DatabaseConnectionRepository connectionRepository;

	private Connection getConnection(DatabaseConnection connection) throws SQLException {
		return DriverManager.getConnection(
			connection.getUrl(),
			connection.getUsername(),
			connection.getPassword());
	}
	@Override
	public Map<String, Object> executeChangelogByTag(String projectKey, String changelogPath, String tag) throws
		SQLException,
		FileNotFoundException,
		LiquibaseException {
		Optional<DatabaseConnection> connectionOpt = connectionRepository.findByProjectKey(projectKey);
		if (connectionOpt.isEmpty()) {
			throw new RuntimeException("프로젝트에 대한 데이터베이스 연결을 찾을 수 없습니다: " + projectKey);
		}

		DatabaseConnection connection = connectionOpt.get();
		String dbType = extractDbTypeFromConnection(connection);

		// 실제 물리적 경로 구성
		String fullPath = "liquibase/backend/src/main/resources/" + changelogPath;
		File changelogFile = new File(fullPath);

		if (!changelogFile.exists()) {
			throw new RuntimeException("Changelog 파일을 찾을 수 없습니다 : " + fullPath);
		}
			Map<String, Object> result = new HashMap<>();
			try (Connection conn = getConnection(connection)) {
				Database database = DatabaseFactory.getInstance()
					.findCorrectDatabaseImplementation(new JdbcConnection(conn));

				ResourceAccessor resourceAccessor = new DirectoryResourceAccessor(
					Paths.get("liquibase/backend/src/main/resources"));

			Liquibase liquibase = new Liquibase(
				changelogPath,
				resourceAccessor,
				database);

			// 속성 설정
			liquibase.setChangeLogParameter("project", projectKey.toLowerCase());
			liquibase.setChangeLogParameter("db.type", dbType);

			long startTime = System.currentTimeMillis();

				// 먼저 변경사항 적용
				liquibase.update("");

				// 태그가 제공된 경우, 실행 후 태그 적용
				if (tag != null && !tag.isEmpty()) {
					liquibase.tag(tag);
					result.put("tagApplied", tag);
				}

			long endTime = System.currentTimeMillis();

			result.put("success", true);
			result.put("message", tag != null ?
				"Changelog를 성공적으로 태그를 적용했습니다: " + tag :
				"Changelog를 성공적으로 실행했습니다");
			result.put("executionTimeMs", endTime - startTime);

			return result;
		} catch (Exception e) {
			log.error("Error executing changelog: {}", e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * 특정 changelog에 태그를 적용하거나 제거하는 메서드
	 *
	 * @param projectKey 프로젝트 키
	 * @param changelogId Changelog ID
	 * @param tag 적용할 태그 (null인 경우 태그 제거)
	 * @return 처리 결과 정보
	 * @throws Exception 데이터베이스 오류 등이 발생한 경우
	 */
	@Override
	public Map<String, Object> applyTagToChangelog(String projectKey, String changelogId, String tag) throws Exception {
		Optional<DatabaseConnection> connectionOpt = connectionRepository.findByProjectKey(projectKey);
		if (connectionOpt.isEmpty()) {
			throw new RuntimeException("프로젝트에 대한 데이터베이스 연결을 찾을 수 없습니다: " + projectKey);
		}

		DatabaseConnection connection = connectionOpt.get();
		Map<String, Object> result = new HashMap<>();

		try (Connection conn = getConnection(connection)) {
			// 기존 태그 확인
			String existingTag = getExistingTag(conn, changelogId);

			// changelog 존재 여부 확인
			if (!isChangelogExists(conn, changelogId)) {
				result.put("success", false);
				result.put("message", "해당 Changelog를 찾을 수 없습니다.");
				return result;
			}

			// 태그 적용 또는 제거
			boolean updateSuccess = updateTag(conn, changelogId, tag);

			if (updateSuccess) {
				result.put("success", true);
				result.put("message", getSuccessMessage(existingTag, tag));
				result.put("tag", tag);
			} else {
				result.put("success", false);
				result.put("message", "태그 업데이트에 실패했습니다.");
			}

			return result;
		} catch (Exception e) {
			log.error("태그 적용 중 오류 발생: {}", e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * DB에서 해당 changelog의 기존 태그를 조회
	 */
	private String getExistingTag(Connection conn, String changelogId) throws SQLException {
		String existingTag = null;
		String checkQuery = "SELECT TAG FROM DATABASECHANGELOG WHERE ID = ?";

		try (PreparedStatement ps = conn.prepareStatement(checkQuery)) {
			ps.setString(1, changelogId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					existingTag = rs.getString("TAG");
				}
			}
		}

		return existingTag;
	}

	/**
	 * changelog가 존재하는지 확인
	 */
	private boolean isChangelogExists(Connection conn, String changelogId) throws SQLException {
		String checkQuery = "SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID = ?";

		try (PreparedStatement ps = conn.prepareStatement(checkQuery)) {
			ps.setString(1, changelogId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getInt(1) > 0;
				}
			}
		}

		return false;
	}

	/**
	 * 태그 업데이트 실행
	 */
	private boolean updateTag(Connection conn, String changelogId, String tag) throws SQLException {
		String updateQuery = tag == null
			? "UPDATE DATABASECHANGELOG SET TAG = NULL WHERE ID = ?"
			: "UPDATE DATABASECHANGELOG SET TAG = ? WHERE ID = ?";

		try (PreparedStatement ps = conn.prepareStatement(updateQuery)) {
			if (tag == null) {
				ps.setString(1, changelogId);
			} else {
				ps.setString(1, tag);
				ps.setString(2, changelogId);
			}

			int rowsAffected = ps.executeUpdate();
			return rowsAffected > 0;
		}
	}

	/**
	 * 태그 적용/수정/제거 결과에 따른 성공 메시지 생성
	 */
	private String getSuccessMessage(String existingTag, String newTag) {
		if (newTag == null) {
			return "태그가 성공적으로 제거되었습니다.";
		} else if (existingTag != null && !existingTag.isEmpty()) {
			return String.format("태그가 수정되었습니다: %s → %s", existingTag, newTag);
		} else {
			return String.format("태그가 성공적으로 적용되었습니다: %s", newTag);
		}
	}
	// @Override
	// public Map<String, Object> applyTagToChangelog(String projectKey, String changelogId, String tag) throws Exception {
	// 	Optional<DatabaseConnection> connectionOpt = connectionRepository.findByProjectKey(projectKey);
	// 	if (connectionOpt.isEmpty()) {
	// 		throw new RuntimeException("프로젝트에 대한 데이터베이스 연결을 찾을 수 없습니다: " + projectKey);
	// 	}
	//
	// 	DatabaseConnection connection = connectionOpt.get();
	//
	// 	Map<String, Object> result = new HashMap<>();
	// 	try (Connection conn = getConnection(connection)) {
	// 		Database database = DatabaseFactory.getInstance()
	// 			.findCorrectDatabaseImplementation(new JdbcConnection(conn));
	//
	// 		// 태그 적용 전에 해당 changeset에 태그가 있는지 확인 (정보 확인용)
	// 		String existingTag = null;
	// 		String checkQuery = "SELECT TAG FROM DATABASECHANGELOG WHERE ID = ?";
	// 		try (PreparedStatement ps = conn.prepareStatement(checkQuery)) {
	// 			ps.setString(1, changelogId);
	// 			try (ResultSet rs = ps.executeQuery()) {
	// 				if (rs.next()) {
	// 					existingTag = rs.getString("TAG");
	// 				}
	// 			}
	// 		}
	// 		String updateQuery = tag == null
	// 			? "UPDATE DATABASECHANGELOG SET TAG = NULL WHERE ID = ?"
	// 			: "UPDATE DATABASECHANGELOG SET TAG = ? WHERE ID = ?";
	//
	// 		try (PreparedStatement ps = conn.prepareStatement(updateQuery)) {
	// 			if (tag == null) {
	// 				ps.setString(1, changelogId);
	// 			} else {
	// 				ps.setString(1, tag);
	// 				ps.setString(2, changelogId);
	// 			}
	//
	// 			int rowsAffected = ps.executeUpdate();
	//
	// 			if (rowsAffected > 0) {
	// 				result.put("success", true);
	// 				if (tag == null) {
	// 					result.put("message", "태그가 성공적으로 제거되었습니다.");
	// 				} else if (existingTag != null && !existingTag.isEmpty()) {
	// 					result.put("message", "태그가 수정되었습니다: " + existingTag + " → " + tag);
	// 				} else {
	// 					result.put("message", "태그가 성공적으로 적용되었습니다: " + tag);
	// 				}
	// 				result.put("tag", tag);
	// 			} else {
	//
	// 				// 태그 적용 쿼리 실행 (기존 태그가 있더라도 수정 가능)
	// 				updateQuery = "UPDATE DATABASECHANGELOG SET TAG = ? WHERE ID = ?";
	// 				try (PreparedStatement ps = conn.prepareStatement(updateQuery)) {
	// 					ps.setString(1, tag);
	// 					ps.setString(2, changelogId);
	// 					rowsAffected = ps.executeUpdate();
	//
	// 					if (rowsAffected > 0) {
	// 						result.put("success", true);
	// 						if (existingTag != null && !existingTag.isEmpty()) {
	// 							result.put("message", "태그가 수정되었습니다: " + existingTag + " → " + tag);
	// 						} else {
	// 							result.put("message", "태그가 성공적으로 적용되었습니다: " + tag);
	// 						}
	// 						result.put("tag", tag);
	// 					} else {
	// 						result.put("success", false);
	// 						result.put("message", "해당 Changelog를 찾을 수 없습니다.");
	// 					}
	// 				}
	//
	// 			}
	//
	// 			return result;
	// 		} catch (Exception e) {
	// 			log.error("태그 적용 중 오류 발생: {}", e.getMessage(), e);
	// 			throw e;
	// 		}
	// 	}
	// }
}
