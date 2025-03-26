package com.dbtest.liquibase.controller;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dbtest.liquibase.service.ChangelogService;
import com.dbtest.liquibase.service.LiquibaseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/changelogs")
@RequiredArgsConstructor
public class ChangelogController {

	private final ChangelogService changelogService;
	private final LiquibaseService liquibaseService;

	@GetMapping("/{projectKey}")
	public ResponseEntity<List<Map<String, Object>>> getChangelogs(@PathVariable String projectKey) {
		List<Map<String, Object>> changelogs = changelogService.getChangelogEntries(projectKey);
		return ResponseEntity.ok(changelogs);
	}

	@PostMapping("/execute-changelog/{projectKey}")
	public ResponseEntity<Map<String, Object>> executeChangelog(
		@PathVariable String projectKey,
		@RequestBody Map<String, String> request) {

		String changelogPath = request.get("changelogPath");
		String tag = request.get("tag");  // 태그 추가

		try {
			Map<String, Object> result = liquibaseService.executeChangelogByTag(projectKey, changelogPath, tag);
			return ResponseEntity.ok(result);
		} catch (Exception e) {
			Map<String, Object> error = new HashMap<>();
			error.put("success", false);
			error.put("message", "Changelog 실행 실패: " + e.getMessage());
			return ResponseEntity.status(500).body(error);
		}
	}
	@GetMapping("/changelog-content")
	public ResponseEntity<Map<String, Object>> getChangelogContent(@RequestParam String filePath) {
		try {
			String fullPath = "liquibase/backend/src/main/resources/" + filePath;
			File file = new File(fullPath);
			log.info("Attempting to access changelog file at: {}", fullPath);
			log.info("File exists: {}", file.exists());
			log.info("Absolute path: {}", file.getAbsolutePath());

			if (!file.exists()) {
				Map<String, Object> error = new HashMap<>();
				error.put("error", "Changelog 파일을 찾을 수 없습니다: " + filePath);
				return ResponseEntity.status(404).body(error);
			}

			String content = Files.readString(file.toPath());

			Map<String, Object> response = new HashMap<>();
			response.put("content", content);
			response.put("path", filePath);

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			Map<String, Object> error = new HashMap<>();
			error.put("error", "파일 읽기 실패: " + e.getMessage());
			return ResponseEntity.status(500).body(error);
		}
	}

	@PostMapping("/apply-tag/{projectKey}")
	public ResponseEntity<Map<String, Object>> applyTag(
		@PathVariable String projectKey,
		@RequestBody Map<String, String> request) {

		String changelogId = request.get("changelogId");
		String tag = request.get("tag");
		boolean isRemoveTag = request.containsKey("removeTag") && Boolean.parseBoolean(request.get("removeTag"));

		// 태그 제거 요청이 아닌데 태그가 비어있는 경우만 검증
		if (!isRemoveTag && (tag == null || tag.trim().isEmpty())) {
			Map<String, Object> error = new HashMap<>();
			error.put("success", false);
			error.put("message", "태그를 입력해주세요.");
			return ResponseEntity.badRequest().body(error);
		}

		try {
			// 태그 제거 요청인 경우 tag를 null로 설정
			if (isRemoveTag) {
				tag = null;
			}

			Map<String, Object> result = liquibaseService.applyTagToChangelog(projectKey, changelogId, tag);
			return ResponseEntity.ok(result);
		} catch (Exception e) {
			Map<String, Object> error = new HashMap<>();
			error.put("success", false);
			error.put("message", isRemoveTag ?
				"태그 제거 실패: " + e.getMessage() :
				"태그 적용 실패: " + e.getMessage());
			return ResponseEntity.status(500).body(error);
		}
	}
}