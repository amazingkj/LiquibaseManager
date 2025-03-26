--liquibase formatted sql

--changeset jiin:a4c9008c
--comment: jiin
--tag: 0.0.1
INSERT INTO TEST_TABLE (ID, NAME) VALUES (12, 'Test12');
