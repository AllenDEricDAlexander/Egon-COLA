# Tianquan-Jianshen Verification Evidence Template

Do not include plaintext secrets, Authorization headers, cookies, passwords,
private keys, raw sensitive fields or full production payloads.

## 1. Run identity

| Field | Value |
| --- | --- |
| Run ID | |
| Operator | |
| Started/finished UTC | |
| Repository SHA | |
| Tianquan-Jianshen artifact version/build IDs | |
| Environment/namespace | |
| Dedicated tenant/application IDs | |
| Yuheng group/release IDs | |
| Tianshu configuration scope (`biz/app/env/namespace`) | |
| Tianshu HTTP Provider service scope (`kind/protocol/name/group/version`) | |

## 2. Topology identity

| Process | Host | Port | Instance ID | Snowflake machine ID | Build ID |
| --- | --- | --- | --- | --- | --- |
| Tianquan-Jianshen Admin 1 | | | | | |
| Tianquan-Jianshen Admin 2 | | | | | |
| Tianshu Admin | | | n/a | n/a | |
| Yuheng Admin | | | n/a | n/a | |
| Yuheng Engine | | | | | |

Record whether PostgreSQL and the Tianshu/Yuheng/Tianquan-Jianshen Redis roles are distinct,
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

| Observation                                    | Expected                      | Actual | Timestamp/trace |
|------------------------------------------------|-------------------------------|--------|-----------------|
| Admin 1 Tianshu Config Client state/lease          | READY / CONFIG_CLIENT         |        |                 |
| Admin 2 Tianshu Config Client state/lease          | READY / CONFIG_CLIENT         |        |                 |
| Admin 1 config version / last apply error code | recorded / none               |        |                 |
| Admin 2 config version / last apply error code | recorded / none               |        |                 |
| Admin 1 Definition status/set ID               | accepted                      |        |                 |
| Admin 2 Definition status/set ID               | accepted                      |        |                 |
| Tianshu HTTP Provider lease Admin 1/expiry         | registered/unexpired          |        |                 |
| Tianshu HTTP Provider lease Admin 2/expiry         | registered/unexpired          |        |                 |
| Yuheng Release ID/status                      | explicit/success              |        |                 |
| Yuheng engine-observed version                | matches release               |        |                 |
| Runtime consistency                            | true                          |        |                 |
| Routed requests with two instances             | success                       |        |                 |
| Routed requests after instance 1 stopped       | success                       |        |                 |
| Route after both instances stopped             | configured fail-closed status |        |                 |
| Route after both instances restored            | success                       |        |                 |

The five facts are Tianshu Config Client, Yuheng Definition, Tianshu HTTP Provider
lease, explicit Yuheng Release/Engine consistency, and routed request evidence.
Do not collapse them into one health result. Record only lease fingerprints and
bounded error codes; never copy complete lease IDs or configuration values.

## 5. Data and cleanup isolation

| Field | Value |
| --- | --- |
| PostgreSQL fixture schema (`rbac3_it_<runId>`) | |
| Redis fixture prefix (`tianquan-jianshen:it:<runId>:`) | |
| Fixture state file | |
| Exact Redis keys recorded | |
| Cleanup evidence file | |
| Schema absent after cleanup | yes/no |
| All recorded keys absent after cleanup | yes/no |
| Unrelated database/key count unchanged | evidence/reference |

## 6. Security/failure cases

Record status/error code and trace ID for: missing/expired token, invalid
signature, Tenant mismatch, same-APP mutually exclusive activation, stale
`authVersion`, deleted Refresh Token, closed Fence, stale snapshot, forbidden
field, unknown Yuheng operation, Tianshu lease loss and no-provider Yuheng route.

For Tianshu LKG evidence, record the config key, previous/current/target versions,
checksum result, fixed error code, failed ACK status and later higher successful
version. Do not record the raw value. Explicitly confirm that an invalid update
did not change the effective snapshot or repository metadata.

## 7. Evidence classification and limits

Check every class actually executed:

- [ ] Source/static evidence
- [ ] Unit/property/metamorphic evidence
- [ ] Module integration evidence
- [ ] Host-local PostgreSQL/Redis evidence
- [ ] Real two-process Tianshu/Yuheng routed topology evidence

State skipped checks and why. A lower evidence class must not be described as a
higher class. Note clock/NTP, TLS, multi-host network, production ACL, load and
long-duration behavior that this run did not prove.

## 8. Final result

- Overall result: PASS / FAIL / BLOCKED
- Failed checkpoint and stable error:
- Cleanup result:
- Residual risk:
- Required follow-up:
