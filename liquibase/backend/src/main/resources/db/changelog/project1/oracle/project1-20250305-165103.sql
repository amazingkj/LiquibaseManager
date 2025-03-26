--liquibase formatted sql

--changeset jiin:4e75bdcd
--comment: jiin test
INSERT INTO TEST_TABLE (ID, NAME) VALUES (4, 'Test4');
