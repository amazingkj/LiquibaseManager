--liquibase formatted sql

--changeset 11:5f68a276
--comment: 11
--tagDatabase: 11
INSERT INTO TEST_TABLE (ID, NAME) VALUES (15, 'Test15');
