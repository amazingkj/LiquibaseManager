--liquibase formatted sql

--changeset test:99a940e0
--comment: test
INSERT INTO TEST_TABLE (ID, NAME) VALUES (6, 'Test6');

