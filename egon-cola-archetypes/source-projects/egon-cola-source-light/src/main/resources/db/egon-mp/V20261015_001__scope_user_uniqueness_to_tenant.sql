-- Change: scope Light user business keys to their tenant and soft-delete lifecycle.
-- Scope: existing MASTER_DATA users only; original initialization and active-row indexes remain immutable.
-- Compatibility: current global keys imply the new keys; unexpected constraints/indexes abort before any change.
DO $egon$
DECLARE
    user_table REGCLASS;
    business_column TEXT;
    original_constraint TEXT;
    active_index TEXT;
    lifecycle_constraint TEXT;
    actual_columns TEXT[];
BEGIN
    IF current_setting('egon_migration.role') = 'MASTER_DATA' THEN
        user_table := to_regclass('users');
        IF user_table IS NULL THEN
            RAISE EXCEPTION 'Light users table is missing';
        END IF;
        LOCK TABLE users IN ACCESS EXCLUSIVE MODE;

        FOREACH business_column IN ARRAY ARRAY['external_id', 'email'] LOOP
            original_constraint := 'uk_users_' || business_column;
            active_index := CASE business_column
                WHEN 'external_id' THEN 'uk_users_tenant_external_active'
                ELSE 'uk_users_tenant_email_active'
            END;
            lifecycle_constraint := CASE business_column
                WHEN 'external_id' THEN 'uk_users_tenant_external_deleted'
                ELSE 'uk_users_tenant_email_deleted'
            END;

            SELECT array_agg(attribute.attname::TEXT ORDER BY member.ordinality)
              INTO actual_columns
              FROM pg_constraint constraint_row
              CROSS JOIN LATERAL unnest(constraint_row.conkey) WITH ORDINALITY AS member(attnum, ordinality)
              JOIN pg_attribute attribute ON attribute.attrelid = constraint_row.conrelid AND attribute.attnum = member.attnum
             WHERE constraint_row.conrelid = user_table AND constraint_row.conname = original_constraint
               AND constraint_row.contype = 'u' AND constraint_row.convalidated AND NOT constraint_row.condeferrable;
            IF actual_columns IS DISTINCT FROM ARRAY[business_column] THEN
                RAISE EXCEPTION 'Unexpected Light users constraint: %', original_constraint;
            END IF;

            SELECT array_agg(attribute.attname::TEXT ORDER BY member.ordinality)
              INTO actual_columns
              FROM pg_index index_row
              JOIN pg_class index_relation ON index_relation.oid = index_row.indexrelid
              CROSS JOIN LATERAL unnest(index_row.indkey::SMALLINT[]) WITH ORDINALITY AS member(attnum, ordinality)
              JOIN pg_attribute attribute ON attribute.attrelid = index_row.indrelid AND attribute.attnum = member.attnum
             WHERE index_row.indrelid = user_table AND index_relation.relname = active_index
               AND index_row.indisunique AND index_row.indisvalid AND index_row.indisready
               AND index_row.indnatts = 2 AND index_row.indnkeyatts = 2 AND index_row.indexprs IS NULL
               AND pg_get_expr(index_row.indpred, index_row.indrelid) = '(deleted_at IS NULL)';
            IF actual_columns IS DISTINCT FROM ARRAY['tenant_id', business_column] THEN
                RAISE EXCEPTION 'Unexpected Light users active index: %', active_index;
            END IF;
            IF EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = user_table AND conname = lifecycle_constraint) THEN
                RAISE EXCEPTION 'Light users lifecycle constraint already exists: %', lifecycle_constraint;
            END IF;
        END LOOP;

        ALTER TABLE users DROP CONSTRAINT uk_users_external_id, DROP CONSTRAINT uk_users_email;
        ALTER TABLE users
            ADD CONSTRAINT uk_users_tenant_external_deleted UNIQUE (tenant_id, external_id, deleted_at),
            ADD CONSTRAINT uk_users_tenant_email_deleted UNIQUE (tenant_id, email, deleted_at);
    END IF;
END
$egon$;
