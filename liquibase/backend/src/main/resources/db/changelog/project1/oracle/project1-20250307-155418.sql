--liquibase formatted sql

--changeset jiin:af5af5a7
--comment: jiin
--tagDatabase: 0.0.2
INSERT INTO TEST_TABLE (ID, NAME) VALUES (13, 'Test13');
