--liquibase formatted sql

--changeset jiin:07e7d6cf
--comment: jiin
--tagDatabase: jiin
SERT INTO TEST_TABLE (ID, NAME) VALUES (14, 'Test14');
