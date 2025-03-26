--liquibase formatted sql

--changeset test8:053e9579
--comment: test8
INSERT INTO TEST_TABLE (ID, NAME) VALUES (8, 'Test8');
