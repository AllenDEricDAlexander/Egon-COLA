# RBAC3 Security Boundaries

## 1. Trust zones

| Boundary                                  | Trusted input                                                                                                            | Untrusted input and treatment                                                           |
|-------------------------------------------|--------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------|
| Public/API client -> Gateway              | USER Access Token cookie/Bearer over TLS; Gateway may use the IdP Refresh Token only for one expired/missing-AT recovery | All headers/body/path; validate schema, size, identity and permission                   |
| Gateway -> RBAC3 Admin                    | Gateway-owned routing plus the original USER Access Token after allow                                                    | Client-supplied identity headers are removed/replaced; Refresh Token is never forwarded |
| Business service -> Starter PEP           | Locally verified IdP USER Access Token and named runtime Redis                                                           | Missing/invalid snapshots, mappings or fences deny                                      |
| Trusted provider -> Manifest/internal API | Scoped service identity and tenant/APP contract                                                                          | Manifest content remains strictly validated and immutable                               |
| RBAC3 Admin -> Gateway Admin              | HMAC Definition report credential                                                                                        | Separate OAuth read token; credentials are not interchangeable                          |
| RBAC3 Admin -> DDC                        | Separate runtime/registry signing credentials and explicit service identity                                              | Direct RPC target, env, namespace, host, port and version require local configuration   |
| Admin -> PostgreSQL/Redis                 | Named deployment-owned clients                                                                                           | No implicit localhost, primary bean or cross-role database reuse                        |

## 2. Tenant and APP isolation

The authenticated principal owns the normal tenant context. A target tenant is
accepted only on platform administration routes, through
`X-RBAC3-Target-Tenant`, and only with the platform permission. Path tenant,
target header and effective tenant must agree. Repositories include tenant in
every lookup and mutation key.

Service identities are additionally scoped to tenant and APP. Internal APIs do
not allow a trusted service for APP A to obtain decisions or snapshots for APP B.

## 3. Credential handling

- Access Tokens are five-minute IdP-signed JWTs with identity claims only. The
  browser uses an HttpOnly cookie; the React SDK never reads or persists them.
- Refresh Tokens are stable, signed IdP JWTs with an absolute expiry. IdP stores
  only a digest and subject/status index in Redis; deleting that record blocks
  future refresh. RBAC3 never stores or processes them.
- IdP owns private JWT keys and JWKS. RBAC3 only performs local public-key
  verification; its audit cursor secret is a separate file-backed secret.
- Gateway HMAC write keys, Gateway OAuth read tokens and DDC signing keys have
  separate scopes and lifecycles.
- Logs, UI errors, audit details and verification evidence redact Authorization,
  cookies, passwords, key material and sensitive field values recursively.

## 4. Fail-closed matrix

Access is denied for malformed/multiple Bearer credentials, unknown `kid`, bad
signature, issuer/audience/time mismatch, expired Access Token, disabled User/Tenant,
auth or policy version drift, missing/expired Redis snapshot, closed Fence,
unknown Gateway operation mapping, unavailable non-covered key source, invalid
Data/Field rule, Participation conflict, or internal dependency error.

A cached public key can be last-known-good only for its bounded validity window.
It never authorizes a missing authorization snapshot or bypasses a Fence.

## 5. Privilege change protections

- Assignment is not activation.
- Self-assignment and privileged-role assignment require explicit policy.
- One complete Management Policy must cover subject, target scope, role root and
  operation; policy fragments are never composed.
- SSD constrains qualification. DSD and same-APP mutex constrain active role sets.
- Optimistic versions, PostgreSQL locks and stable capacity keys serialize
  concurrent changes.
- Idempotency keys are stored with request hashes; reuse with changed content is
  rejected.
- Audit facts are append-only and read access is itself audited.

## 6. Data and field enforcement

Clients cannot enforce Data or Field security. The decision supplies canonical
scope/filter evidence, and the server applies it before returning results.
Sensitive fields default to no access. Masked responses never include the raw
value in hidden DOM state or alternate JSON fields.

## 7. Network and operational controls

Use TLS for all cross-process links and restrict management endpoints by network
policy plus scoped credentials. Do not expose internal authorization, Manifest
reporting, the DDC gRPC control plane, or Gateway report endpoints publicly. Set bounded
connect/read timeouts, payload limits, worker concurrency and retry backoff.

Verification scripts accept only explicit URLs and secret files. Fixture cleanup
accepts a strict schema/prefix and exact keys from a non-symbolic-link state
file. Scripts do not control service processes.

## 8. Residual evidence limits

Unit and integration tests do not demonstrate the user's TLS, firewall, secret
permissions, PostgreSQL isolation, Redis ACL, clock synchronization, multi-node
Snowflake allocation, DDC lease expiry or Gateway failover. Those controls need
deployment review and recorded live evidence.
