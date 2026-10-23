-- src/main/resources/db/migration/V5__entries_side_varchar.sql
ALTER TABLE entries ALTER COLUMN side TYPE VARCHAR(6);
