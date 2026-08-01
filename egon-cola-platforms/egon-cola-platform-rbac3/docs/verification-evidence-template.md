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
| DDC configuration scope (`biz/app/env/namespace`) | |
| DDC HTTP Provider service scope (`kind/protocol/name/group/version`) | |

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
| Admin 1 DDC Config Client state/session | READY / CONFIG_CLIENT | | |
| Admin 2 DDC Config Client state/session | READY / CONFIG_CLIENT | | |
| Admin 1 five config versions / last apply error code | recorded / none | | |
| Admin 2 five config versions / last apply error code | recorded / none | | |
| Admin 1 Definition status/set ID | accepted | | |
| Admin 2 Definition status/set ID | accepted | | |
| DDC HTTP Provider lease Admin 1/expiry | registered/unexpired | | |
| DDC HTTP Provider lease Admin 2/expiry | registered/unexpired | | |
| Gateway Release ID/status | explicit/success | | |
| Gateway engine-observed version | matches release | | |
| Runtime consistency | true | | |
| Five routed requests with two instances | success | | |
| Five routed requests after instance 1 stopped | success | | |
| Route after both instances stopped | configured fail-closed status | | |
| Route after both instances restored | success | | |

The five facts are DDC Config Client, Gateway Definition, DDC HTTP Provider
lease, explicit Gateway Release/Engine consistency, and routed request evidence.
Do not collapse them into one health result. Record only lease fingerprints and
bounded error codes; never copy complete lease IDs or configuration values.

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

For DDC LKG evidence, record the config key, previous/current/target versions,
checksum result, fixed error code, failed ACK status and later higher successful
version. Do not record the raw value. Explicitly confirm that an invalid update
did not change the effective snapshot or repository metadata.

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
