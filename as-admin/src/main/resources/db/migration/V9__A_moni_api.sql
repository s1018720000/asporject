

ALTER TABLE MONI_API ADD EXPECTED_CODE VARCHAR(10) AFTER CONTENT_TYPE;

ALTER TABLE MONI_API_LOG ADD EXPECTED_CODE VARCHAR(10) AFTER END_TIME;