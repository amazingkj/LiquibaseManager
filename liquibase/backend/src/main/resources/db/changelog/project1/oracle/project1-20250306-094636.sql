--liquibase formatted sql

--changeset jiinteset:bb3d43e7
--comment: test
INSERT INTO TEST_TABLE (ID, NAME) VALUES (5, 'Test5');
