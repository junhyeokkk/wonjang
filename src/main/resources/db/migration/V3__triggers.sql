-- ① entries append-only: UPDATE/DELETE 차단
CREATE OR REPLACE FUNCTION reject_entry_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'entries is append-only (use reversal voucher)';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_entries_immutable
    BEFORE UPDATE OR DELETE ON entries
    FOR EACH ROW EXECUTE FUNCTION reject_entry_mutation();

-- ② 전표 균형 검증: 커밋 시점(deferred)에 voucher 단위 차변=대변 확인
CREATE OR REPLACE FUNCTION check_voucher_balanced() RETURNS trigger AS $$
DECLARE
    d BIGINT;
    c BIGINT;
BEGIN
    SELECT
        COALESCE(SUM(amount) FILTER (WHERE side = 'DEBIT'),  0),
        COALESCE(SUM(amount) FILTER (WHERE side = 'CREDIT'), 0)
    INTO d, c
    FROM entries
    WHERE voucher_id = NEW.voucher_id;

    IF d <> c THEN
        RAISE EXCEPTION 'voucher % unbalanced: debit=% credit=%', NEW.voucher_id, d, c;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_voucher_balanced
    AFTER INSERT ON entries
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION check_voucher_balanced();
