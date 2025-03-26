--liquibase formatted sql

--changeset test:7b4a0a4f
--comment: test
INSERT INTO TEST_TABLE (ID, NAME) VALUES (6, 'Test6');

