--liquibase formatted sql

--changeset test:2cfe9a5e
--comment: test
INSERT INTO TEST_TABLE (ID, NAME) VALUES (6, 'Test6');

