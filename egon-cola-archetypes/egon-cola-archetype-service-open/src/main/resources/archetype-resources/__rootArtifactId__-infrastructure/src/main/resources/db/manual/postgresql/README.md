# Evaluation manual schema

These scripts are operator-owned PostgreSQL DDL. The application never executes them,
does not create a migration history table, and starts only after the target databases
have been prepared.

Apply them in this order:

1. `master-data/001__create_evaluation_master_data_schema.sql` on the `master_data`
   primary.
2. `shard/002__create_evaluation_sharded_schema.sql` on every `shard_N` primary.
3. `master-data/003__migrate_evaluation_master_data_to_egon_model.sql` on the
   `master_data` primary after reviewing the preflight and tenant mapping.
4. `shard/004__migrate_evaluation_sharded_to_tenant_model.sql` with the same bytes on
   every `shard_N` primary after reviewing the preflight and tenant mapping.
5. Verify that the five logical tables (`evaluation_course`,
   `evaluation_course_schedule`, `evaluation_exam`, `evaluation_exam_paper`,
   `evaluation_score`) and every physical suffix are present, all ID/reference columns
   are `BIGINT`, every routed table has positive `tenant_id`, and the constraint/index
   names match the scripts before enabling traffic.

Run the scripts with the database owner's reviewed change process. Replicas receive
the schema through the database replication or replica provisioning workflow; they
are not application migration targets. On a partial failure, stop rollout, restore
the affected database from the operator backup, and apply a reviewed forward fix.
Never point Spring SQL initialization, Flyway, Liquibase, or another migration tool at
this directory.
