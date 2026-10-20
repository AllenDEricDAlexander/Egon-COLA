/*
 * ShardingSphere's PostgreSQL grammar cannot parse SKIP LOCKED directly.
 * Keep queue row locks in PostgreSQL and in the caller's existing transaction.
 * Freeze Flyway's schema search path; both functions retain invoker privileges.
 */

CREATE FUNCTION rbac3_claim_due_assignments(
    p_status VARCHAR,
    p_now TIMESTAMP WITH TIME ZONE,
    p_batch_size INTEGER
) RETURNS SETOF BIGINT
    LANGUAGE sql
    VOLATILE
    SET search_path FROM CURRENT
    AS $$
SELECT id
  FROM rbac3_user_role_assignment
 WHERE status = p_status
   AND CASE WHEN p_status = 'PENDING'
            THEN valid_from <= p_now
            ELSE valid_to IS NOT NULL AND valid_to <= p_now
       END
 ORDER BY coalesce(valid_to, valid_from), id
 LIMIT p_batch_size
 FOR UPDATE SKIP LOCKED;
$$;

CREATE FUNCTION rbac3_claim_recoverable_mutations(
    p_batch_size INTEGER
) RETURNS SETOF BIGINT
    LANGUAGE sql
    VOLATILE
    SET search_path FROM CURRENT
    AS $$
SELECT mutation_id
  FROM rbac3_authorization_mutation
 WHERE status IN ('COMMITTED', 'PROJECTED', 'RECOVERY_REQUIRED')
 ORDER BY updated_at, mutation_id
 LIMIT p_batch_size
 FOR UPDATE SKIP LOCKED;
$$;
