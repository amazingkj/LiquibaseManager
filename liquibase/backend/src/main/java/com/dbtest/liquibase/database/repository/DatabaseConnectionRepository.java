package com.dbtest.liquibase.database.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dbtest.liquibase.database.entity.DatabaseConnection;

@Repository
public interface DatabaseConnectionRepository extends JpaRepository<DatabaseConnection, Long> {

	List<DatabaseConnection> findByActive(boolean active);

	Optional<DatabaseConnection> findByProjectKey(String projectKey);
}