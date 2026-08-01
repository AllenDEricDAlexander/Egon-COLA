# RBAC3 Verification Evidence Template

Do not include plaintext secrets, Authorization headers, cookies, passwords,
private keys, raw sensitive fields or full production payloads.

## 1. Run identity

| Field | Value |
| --- | --- |
| Run ID | |
| Operator | |
| Started/finished UTC | |
| Repository SHA | |
| RBAC3 artifact version/build IDs | |
| Environment/namespace | |
| Dedicated tenant/application IDs | |
| Gateway group/release IDs | |

## 2. Topology identity

| Process | Host | Port | Instance ID | Snowflake machine ID | Build ID |
| --- | --- | --- | --- | --- | --- |
| RBAC3 Admin 1 | | | | | |
| RBAC3 Admin 2 | | | | | |
| DDC Admin | | | n/a | n/a | |
| Gateway Admin | | | n/a | n/a | |
| Gateway Engine | | | | | |

Record whether PostgreSQL and the DDC/Gateway/RBAC3 Redis roles are distinct,
including logical database numbers without recording credentials.

## 3. Commands and exit codes

| Evidence class | Exact command | Exit code | Report/artifact path |
| --- | --- | --- | --- |
| Java clean verify | | | |
| Admin local profile | | | |
| Frontend typecheck/tests/lint/build | | | |
| E2E scenario listing | | | |
| Static script | | | |
| Local dependencies | | | |
| Live topology | | | |
| Fixture cleanup | | | |

## 4. Independent control-plane observations

| Observation | Expected | Actual | Timestamp/trace |
| --- | --- | --- | --- |
| Admin 1 Definition status/set ID | accepted | | |
| Admin 2 Definition status/set ID | accepted | | |
| DDC lease Admin 1/expiry | registered | | |
| DDC lease Admin 2/expiry | registered | | |
| Gateway Release ID/status | explicit/success | | |
| Gateway engine-observed version | matches release | | |
| Runtime consistency | true | | |
| Five routed requests with two instances | success | | |
| Five routed requests after instance 1 stopped | success | | |
| Route after both instances stopped | configured fail-closed status | | |
| Route after both instances restored | success | | |

## 5. Data and cleanup isolation

| Field | Value |
| --- | --- |
| PostgreSQL fixture schema (`rbac3_it_<runId>`) | |
| Redis fixture prefix (`rbac3:it:<runId>:`) | |
| Fixture state file | |
| Exact Redis keys recorded | |
| Cleanup evidence file | |
| Schema absent after cleanup | yes/no |
| All recorded keys absent after cleanup | yes/no |
| Unrelated database/key count unchanged | evidence/reference |

## 6. Security/failure cases

Record status/error code and trace ID for: missing token, invalid signature,
Tenant mismatch, same-APP mutually exclusive activation, stale Session version,
Refresh replay, closed Fence, stale snapshot, forbidden field, unknown Gateway
operation, DDC lease loss and no-provider Gateway route.

## 7. Evidence classification and limits

Check every class actually executed:

- [ ] Source/static evidence
- [ ] Unit/property/metamorphic evidence
- [ ] Module integration evidence
- [ ] Host-local PostgreSQL/Redis evidence
- [ ] Real two-process DDC/Gateway routed topology evidence

State skipped checks and why. A lower evidence class must not be described as a
higher class. Note clock/NTP, TLS, multi-host network, production ACL, load and
long-duration behavior that this run did not prove.

## 8. Final result

- Overall result: PASS / FAIL / BLOCKED
- Failed checkpoint and stable error:
- Cleanup result:
- Residual risk:
- Required follow-up:
