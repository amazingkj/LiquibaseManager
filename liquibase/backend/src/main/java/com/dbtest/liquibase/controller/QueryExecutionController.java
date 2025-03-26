package com.dbtest.liquibase.controller;

import java.util.HashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.dbtest.liquibase.service.QueryExecutionService;
import com.dbtest.liquibase.vo.QueryExecutionRequest;
import com.dbtest.liquibase.vo.QueryExecutionResult;

@RestController
@RequestMapping("/api/query")
@RequiredArgsConstructor
public class QueryExecutionController {

	private final QueryExecutionService queryExecutionService;

	@PostMapping("/execute/{projectKey}")
	public ResponseEntity<QueryExecutionResult> executeQuery(
		@PathVariable String projectKey,
		@RequestBody QueryExecutionRequest request) {

		QueryExecutionResult result = queryExecutionService.executeQuery(
			projectKey,
			request.getQuery(),
			request.isGenerateChangeset(),
			request.getTag()
		);

		return ResponseEntity.ok(result);
	}

	@PostMapping("/save-changeset/{projectKey}")
	public ResponseEntity<Map<String, Object>> saveAsChangeset(
		@PathVariable String projectKey,
		@RequestBody QueryExecutionRequest request) {

		try {
			Map<String, Object> changesetId = queryExecutionService.saveAsChangeset(
				projectKey,
				request.getQuery(),
				request.getAuthor(),
				request.getDescription(),
				request.getTag()
			);

			return ResponseEntity.ok(changesetId);
		} catch (Exception e) {
			Map<String, Object> error = new HashMap<>();
			error.put("error", e.getMessage());
			return ResponseEntity.status(500).body(error);
		}
	}


}