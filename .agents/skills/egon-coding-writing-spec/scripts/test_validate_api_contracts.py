#!/usr/bin/env python3
"""Regression tests for Template Version 7 external API contract validation."""

from __future__ import annotations

import unittest

from validate_spec import validate_api_contracts_v7


def api_gate_rows(rest: bool = True, graphql: bool = True) -> str:
    rows: list[str] = []
    for number in range(1, 10):
        gate_id = f"API-GATE-{number:03d}"
        protocol_absent = (number in {3, 7} and not rest) or (number == 4 and not graphql)
        if protocol_absent:
            rows.append(
                f"| `{gate_id}` | Not applicable | N/A | Inventory proves protocol absent | "
                "Protocol-specific rule is not applicable | None |"
            )
        else:
            rows.append(
                f"| `{gate_id}` | Applicable | PASS | Chapter 9 repository evidence | "
                "Design decision and feasible proof are complete | None |"
            )
    return "\n".join(rows)


def complete_api_chapter(rest: bool = True, graphql: bool = True) -> str:
    inventory_rows: list[str] = []
    details: list[str] = []
    if rest:
        inventory_rows.append(
            "| `API-001` | Existing/Keep | Read order | HTTP | REST Query | Web client | "
            "Order web module | `GET /api/v1/orders/{orderId}` | `getOrder` from Controller | "
            "`OrderQuery` | `OrderResponse` | Bearer and tenant context | Problem response | "
            "ETag version | `REQ-001` |"
        )
        details.append(
            """#### 9.2.1 API-001 — Read order

##### Necessity and interaction-cost decision

Repository route serves the independent order-detail consumer without a preflight call.

##### API style and CQRS semantics

| Concern | Decision/evidence |
| --- | --- |
| Protocol style | REST Query |
| CQRS role | Query is read-only and owned by the order Service |
| Resource/task semantics | Existing order resource is read with HTTP GET |
| Read/write and side effects | Reads the order projection and only records metrics |
| Consistency and idempotency | Safe idempotent read with ETag semantics |
| Why this style | Existing REST consumer requires a stable resource representation |

##### Identity and purpose

`GET /api/v1/orders/{orderId}` returns one tenant-visible order.

##### Request parameters

Path identifier, authentication header, and tenant context are fully specified in the contract.

##### Success response

The complete response wrapper and every field are defined in commented JSON.

##### Error responses

Validation, authentication, permission, absence, and dependency failures are mapped explicitly.

##### Interface logic for frontend and consumers

The seven ordered behavior categories define validation, read ownership, failures, and UI behavior.

##### Documentation contract

| Concern | Decision/evidence |
| --- | --- |
| Documentation authority | REST Code-first Controller contract |
| REST OpenAPI operation / GraphQL SDL operation | operationId getOrder for the verified GET route |
| Annotation/mapping ownership | Controller owns @Operation and Spring mapping |
| Generated schema elements | Parameters responses security and component schemas |
| Compatibility and drift proof | Generated OAS effect is asserted by the focused contract test |

| Target | Required annotation/configuration | Exact values/source | Generated OAS effect | Verification |
| --- | --- | --- | --- | --- |
| OrderController#getOrder | @Operation and @ApiResponse | Verified getOrder contract | GET path operation | Generated document assertion |

##### Compatibility and verification

Existing consumer compatibility and generated OAS contract tests are explicitly named.
"""
        )
    if graphql:
        detail_number = 2 if rest else 1
        inventory_rows.append(
            "| `API-002` | New/Add | Create order | GraphQL | GraphQL Mutation | Web client | "
            "Order GraphQL adapter | `POST /graphql :: Mutation.createOrder` | "
            "SDL order.graphqls and CreateOrder.graphql | `CreateOrderInput` | "
            "`CreateOrderPayload` | Bearer and field permission | GraphQL errors extensions | "
            "Command idempotency key | `REQ-002` |"
        )
        details.append(
            f"""#### 9.2.{detail_number} API-002 — Create order

##### Necessity and interaction-cost decision

The approved GraphQL client has an independent create-order goal and no parameter preflight.

##### API style and CQRS semantics

| Concern | Decision/evidence |
| --- | --- |
| Protocol style | GraphQL Mutation |
| CQRS role | Command owns one order-creation task |
| Resource/task semantics | Mutation.createOrder expresses the task explicitly |
| Read/write and side effects | Writes order state and emits the documented event |
| Consistency and idempotency | One transaction and stable command idempotency key |
| Why this style | Existing approved GraphQL consumer uses the shared application Command |

##### Identity and purpose

`POST /graphql :: Mutation.createOrder` maps to the exact resolver and Command Service.

##### Request parameters

```graphql
mutation CreateOrder($input: CreateOrderInput!) {{
  createOrder(input: $input) {{ orderId status }}
}}
```

##### Success response

The complete selected data response is defined in commented JSON.

##### Error responses

Transport, validation, authorization, execution, and partial-data errors are defined.

##### Interface logic for frontend and consumers

The seven ordered categories include transaction, idempotency, failure, and frontend behavior.

##### Documentation contract

| Concern | Decision/evidence |
| --- | --- |
| Documentation authority | GraphQL SDL in the repository is authoritative |
| REST OpenAPI operation / GraphQL SDL operation | Mutation.createOrder and named CreateOrder operation |
| Annotation/mapping ownership | Spring mapping uses @MutationMapping on the resolver |
| Generated schema elements | SDL input output nullability and error semantics |
| Compatibility and drift proof | Schema check and GraphQlTester consumer operation prove drift |

| GraphQL artifact | Exact path/symbol | Contract content | Spring mapping | Verification |
| --- | --- | --- | --- | --- |
| SDL | order.graphqls Mutation.createOrder | Input output and nullability | @MutationMapping | Schema test |
| Consumer operation | CreateOrder.graphql | Variables and selection | GraphQlTester consumer | Response and error test |

##### Compatibility and verification

The schema, resolver, consumer operation, error, authorization, and compatibility tests are named.
"""
        )

    rest_source = "REST Code-first Controller and generated OpenAPI" if rest else "N/A because no REST operation is affected"
    graphql_source = "Repository SDL resolver and named operations" if graphql else "N/A because no GraphQL field is affected"
    openapi_target = (
        "| OrderController#getOrder | @Operation and @ApiResponse | Verified REST contract | "
        "Generated OAS path and schema | Document assertion |"
        if rest
        else "| GraphQL-only | N/A because SDL and named operation documents are authoritative | "
        "Repository SDL resolver consumer evidence | N/A | Schema and GraphQlTester checks |"
    )
    return f"""## 9. Interface Definitions

### 9.0 API protocol and documentation governance

| Concern | Decision/evidence |
| --- | --- |
| Protocol selection | Named consumers and use cases prove the selected protocol set |
| CQRS application level | L1 separates boundary Query and Command while sharing existing persistence |
| REST source of truth | {rest_source} |
| GraphQL source of truth | {graphql_source} |
| Springdoc/OpenAPI compatibility | Current Boot MVC dependency management selects the starter and OAS version |
| Legacy Swagger/Springfox status | Repository scan proves Springfox absent from the affected module |
| Security and documentation exposure | Bearer schemes permissions tenant and production exposure are explicit |
| Contract publication and drift gate | Focused generated-document and schema tests own drift detection |

### 9.1 Interface Inventory

| ID | Change/necessity verdict | Name/purpose | Kind | API style/CQRS role | Consumer | Owner | Method + URL / GraphQL field / symbol / topic | Operation ID/schema source | Input | Output | Auth/tenant | Error model | Idempotency/version | Requirements |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
{chr(10).join(inventory_rows)}

### 9.2 Per-interface Detailed Contracts

{chr(10).join(details)}

### 9.3 OpenAPI 3 and springdoc annotation plan

| Target | Required annotation/configuration | Exact values/source | Generated OAS effect | Verification |
| --- | --- | --- | --- | --- |
{openapi_target}

### 9.4 API contract generation and blocking gate

| Gate ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
{api_gate_rows(rest=rest, graphql=graphql)}
"""


class ApiContractValidationTest(unittest.TestCase):
    def test_complete_rest_and_graphql_contract_passes(self) -> None:
        self.assertEqual([], validate_api_contracts_v7(complete_api_chapter()))

    def test_graphql_only_requires_rest_gates_to_be_not_applicable(self) -> None:
        text = complete_api_chapter(rest=False, graphql=True).replace(
            "| `API-GATE-003` | Not applicable | N/A |",
            "| `API-GATE-003` | Applicable | PASS |",
        )

        errors = validate_api_contracts_v7(text)

        self.assertTrue(any("API-GATE-003 protocol applicability" in error for error in errors))

    def test_missing_api_gate_blocks_validation(self) -> None:
        text = complete_api_chapter().replace(
            "| `API-GATE-009` | Applicable | PASS | Chapter 9 repository evidence | "
            "Design decision and feasible proof are complete | None |",
            "",
        )

        errors = validate_api_contracts_v7(text)

        self.assertTrue(any("Missing blocking API gate ID: API-GATE-009" in error for error in errors))

    def test_graphql_contract_requires_exact_root_field(self) -> None:
        text = complete_api_chapter().replace(
            "`POST /graphql :: Mutation.createOrder`",
            "`POST /graphql`",
            1,
        )

        errors = validate_api_contracts_v7(text)

        self.assertTrue(any("GraphQL inventory identity" in error for error in errors))

    def test_blocked_gate_is_valid_only_for_non_pass_verdict(self) -> None:
        text = complete_api_chapter().replace(
            "| `API-GATE-006` | Applicable | PASS |",
            "| `API-GATE-006` | Applicable | BLOCKED |",
        )

        blocked_errors = validate_api_contracts_v7(text, pass_verdict=False)
        pass_errors = validate_api_contracts_v7(text, pass_verdict=True)

        self.assertFalse(any("API-GATE-006" in error for error in blocked_errors))
        self.assertTrue(any("PASS verdict requires API-GATE-006" in error for error in pass_errors))


if __name__ == "__main__":
    unittest.main()
