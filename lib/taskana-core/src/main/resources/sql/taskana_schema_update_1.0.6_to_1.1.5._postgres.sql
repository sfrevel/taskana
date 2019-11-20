-- this script updates the tables TASKANA_SCHEMA_VERSION and TASK.

SET search_path =  %schemaName%;

INSERT INTO TASKANA_SCHEMA_VERSION (VERSION, CREATED) VALUES ('1.1.5', CURRENT_TIMESTAMP);

ALTER TABLE TASK ADD COLUMN CALLBACK_STATE VARCHAR(30) NOT NULL DEFAULT 'NONE';
UPDATE TASK T SET CALLBACK_STATE = 'NONE';

