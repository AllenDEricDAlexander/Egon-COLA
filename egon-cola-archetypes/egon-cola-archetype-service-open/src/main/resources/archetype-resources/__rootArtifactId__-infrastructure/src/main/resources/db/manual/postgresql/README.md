# Evaluation manual schema

These scripts are operator-owned PostgreSQL DDL. The application never executes them,
does not create a migration history table, and starts only after the target databases
have been prepared.

Apply them in this order:

1. `master-data/001__create_evaluation_master_data_schema.sql` on the `master_data`
   primary.
2. `shard/002__create_evaluation_sharded_schema.sql` on every `shard_N` primary.
3. Verify that every physical table is present, all ID/reference columns are `BIGINT`,
   and the constraint/index names match the script before enabling traffic.

Run the scripts with the database owner's reviewed change process. Replicas receive
the schema through the database replication or replica provisioning workflow; they
are not application migration targets. On a partial failure, stop rollout, restore
the affected database from the operator backup, and apply a reviewed forward fix.
Never point Spring SQL initialization, Flyway, Liquibase, or another migration tool at
this directory.
