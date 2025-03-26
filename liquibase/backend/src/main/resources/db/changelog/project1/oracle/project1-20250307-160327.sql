--liquibase formatted sql

--changeset jiin:7b88ffe7
--comment: jiin
--tagDatabase: jiin
SERT INTO TEST_TABLE (ID, NAME) VALUES (15, 'Test15');
