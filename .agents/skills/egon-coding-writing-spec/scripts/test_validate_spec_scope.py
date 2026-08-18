#!/usr/bin/env python3
"""Regression tests for Template Version 4 change-surface validation."""

from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from validate_spec import validate, validate_change_surface, validate_v2_content


def spec_with_rows(rows: list[str]) -> str:
    return "\n".join(
        [
            "## 3. Goals and Non-goals",
            "",
            "### 3.1 Goals",
            "",
            "### 3.2 Non-goals",
            "",
            "### 3.3 Change Surface and Design Depth",
            "",
            "| Area/layer | Disposition | Exact repository evidence | "
            "Changed or preserved behavior/contract | Required Spec treatment | Chapter(s) |",
            "| --- | --- | --- | --- | --- | --- |",
            *rows,
            "",
            "## 4. Requirements and Acceptance Criteria",
        ]
    )


class ChangeSurfaceValidationTest(unittest.TestCase):
    def test_dao_only_scope_is_valid_without_controller_or_frontend_detail(self) -> None:
        text = spec_with_rows(
            [
                "| DAO/Mapper | Affected | `Mapper.xml:selectPage` | Filter changes | "
                "Full query and test design | §7, §8, §14 |",
                "| Service | Context-only | `ServiceImpl#list` | Delegation result invariant | "
                "Boundary evidence only | §7 |",
                "| Controller/API | Unchanged | `Controller#list` | Route and JSON unchanged | "
                "Unchanged record | §9 |",
                "| Database schema | Unchanged | `V001__schema.sql` | Columns and indexes unchanged | "
                "Schema invariant only | §11 |",
                "| Frontend | Unchanged | `pages/List.tsx` | Visible behavior unchanged | "
                "Unchanged record | §12 |",
            ]
        )

        errors, chapters = validate_change_surface(
            text,
            {"Affected Chapters": "§7, §8, §14"},
        )

        self.assertEqual([], errors)
        self.assertEqual({7, 8, 14}, chapters)

    def test_header_must_match_affected_matrix_chapters(self) -> None:
        text = spec_with_rows(
            [
                "| DAO | Affected | `Dao.java` | Query changes | Full design | §7, §8, §14 |",
            ]
        )

        errors, _ = validate_change_surface(text, {"Affected Chapters": "§7, §8, §9"})

        self.assertTrue(any("must exactly match" in error for error in errors))

    def test_at_least_one_area_must_be_affected(self) -> None:
        text = spec_with_rows(
            [
                "| Controller | Unchanged | `Controller.java` | Contract unchanged | "
                "Unchanged record | §9 |",
            ]
        )

        errors, chapters = validate_change_surface(text, {"Affected Chapters": "None"})

        self.assertEqual(set(), chapters)
        self.assertTrue(any("at least one Affected area" in error for error in errors))

    def test_disposition_vocabulary_is_closed(self) -> None:
        text = spec_with_rows(
            [
                "| DAO | Partially affected | `Dao.java` | Query changes | Full design | §7 |",
            ]
        )

        errors, _ = validate_change_surface(text, {"Affected Chapters": "§7"})

        self.assertTrue(any("invalid disposition" in error for error in errors))

    def test_affected_rows_require_design_chapter_references(self) -> None:
        text = spec_with_rows(
            [
                "| DAO | Affected | `Dao.java` | Query changes | Full design | Chapter seven |",
            ]
        )

        errors, chapters = validate_change_surface(text, {"Affected Chapters": "§7"})

        self.assertEqual(set(), chapters)
        self.assertTrue(any("names no §7-§18 chapter" in error for error in errors))

    def test_focused_v4_spec_passes_without_unaffected_deep_sections(self) -> None:
        filename = "2026-08-18-10-00-dao-query-scope.md"
        spec = f"""# DAO query scope

| Field | Value |
| --- | --- |
| Document | `{filename}` |
| Template Version | `4` |
| Status | `Review` |
| Type | `Bugfix` |
| Complexity | `Simple` |
| Complexity Drivers | `None` |
| Created | `2026-08-18 10:00 CST` |
| Updated | `2026-08-18 10:00 CST` |
| Owner | `Team` |
| Repository | `Example` |
| Scope | `Reporting mapper` |
| Change Surface | `DAO query and focused tests only` |
| Affected Chapters | `§7, §8, §14` |
| Source Requirement | `Correct one DAO filter` |
| Baseline Revision | `main at validation fixture` |
| Amends | `None` |
| Supersedes | `None` |
| Depends On | `None` |
| Related Specs | `None` |
| Related Plans | `None` |

## 1. Summary
Correct one query while preserving all caller-visible behavior.

## 2. Background and Current State
Static evidence shows `ReportMapper#page` owns the faulty predicate.

## 3. Goals and Non-goals
### 3.1 Goals
Return the correct filtered rows.
### 3.2 Non-goals
No API, Service contract, schema, or frontend changes.
### 3.3 Change Surface and Design Depth
| Area/layer | Disposition | Exact repository evidence | Changed or preserved behavior/contract | Required Spec treatment | Chapter(s) |
| --- | --- | --- | --- | --- | --- |
| DAO/Mapper | Affected | `ReportMapper#page` | Query predicate changes | Full query design | §7, §8, §14 |
| Service | Context-only | `ReportService#page` | Result contract is preserved | Boundary evidence only | §7 |
| Controller/API | Unchanged | `ReportController#page` | Route and JSON stay identical | Unchanged record | §9 |
| Database schema | Unchanged | `V001__report.sql` | Columns and relationships stay identical | Schema invariant only | §11 |
| Frontend | Unchanged | `ReportPage` | Visible states stay identical | Unchanged record | §12 |

## 4. Requirements and Acceptance Criteria
| ID | Atomic requirement | Priority | Observable acceptance criteria | Source |
| --- | --- | --- | --- | --- |
| REQ-001 | Correct the DAO filter | Must | Focused test returns only eligible rows | User request |
### 4.2 Use-case analysis
| ID | Use case/goal | Primary actor | Supporting actors/systems | Trigger | Preconditions | Main success outcome | Alternatives/failures | Postconditions | Requirements | Interfaces/pages | Tests |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| UC-001 | Generate an accurate report | ACTOR-001 | Database | Existing page request | Valid filters | Correct rows | Empty result remains valid | No schema change | REQ-001 | Existing route unchanged | TEST-001 |

## 5. Constraints, Assumptions, and Decisions
No open major decision; the existing contract remains authoritative.

## 6. Project Technology Context
The affected Mapper uses the repository's existing SQL mapping and test framework.

## 7. Architecture Design
### 7.0 Minimum-design baseline and element-necessity audit
| Proposed element | Change | Requirements | Existing/direct alternative | Concrete inadequacy of alternative | Added calls/state/coupling/failures/migration/operations | Verdict |
| --- | --- | --- | --- | --- | --- | --- |
| Existing Mapper query | Change | REQ-001 | Keep current query | Current predicate is incorrect | No new element | Keep |
| Path | Network calls | Client states | Server contracts/state | Failure and TOCTOU points | Additional user/business value |
| --- | --- | --- | --- | --- | --- |
| Direct baseline | 1 | Existing | Existing | Existing | Correct rows |
### 7.1 System Architecture Design
The existing Controller-to-Service boundary remains unchanged.
### 7.2 High-Level Design
The Service continues delegating to the corrected Mapper query.
### 7.3 Detailed Design
Change the exact predicate and preserve binding, ordering, and result mapping.

## 8. Package Structure and Code File Tree
Modify only the Mapper query and its focused test fixture.

## 9. Interface Definitions
Scope disposition: Unchanged. The existing route, JSON, and errors remain identical.

## 10. POJO and Data Model Design
Scope disposition: Unchanged. Existing result types and mappings remain identical.

## 11. Database Design
Scope disposition: Context-only. Existing columns, indexes, and relationships remain sufficient.

## 12. Frontend Page Design
Scope disposition: Unchanged. Existing page behavior and states remain identical.

## 13. Design Patterns and Architecture Principles
Direct SQL correction is sufficient; no new abstraction is justified.

## 14. Test Design
TEST-001 verifies included, excluded, empty, and ordered result rows.

## 15. Non-functional and Cross-cutting Design
Query cost and tenant filtering invariants remain unchanged.

## 16. Compatibility, Migration, Rollout, and Rollback
No contract or schema migration; rollback is the focused query revert.

## 17. Alternatives and Decisions
The direct predicate correction is selected over an unnecessary Service filter.

## 18. Risks and Open Questions
Risk is limited to incorrect predicate boundaries; the focused fixture covers them.

## 19. Traceability Matrix
| Requirement | Use case | Affected area/chapter | Context-only or unchanged boundary | Interface/model/database/frontend | Tests | Acceptance evidence |
| --- | --- | --- | --- | --- | --- | --- |
| REQ-001 | UC-001 | DAO query / §7, §8, §14 | Service/API/schema invariant | Unchanged | TEST-001 | Focused query result |

## 20. Review and Acceptance
PASS — Ready for user review
"""

        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / filename
            path.write_text(spec, encoding="utf-8")

            errors, warnings = validate(path, strict=True)

        self.assertEqual([], warnings)
        self.assertEqual([], errors)

    def test_affected_interface_chapter_still_requires_full_subsections(self) -> None:
        text = spec_with_rows(
            [
                "| HTTP API | Affected | `Controller.java` | Request field changes | "
                "Full contract design | §9 |",
            ]
        )

        errors = validate_v2_content(
            text,
            {
                "Complexity": "Simple",
                "Complexity Drivers": "None",
                "Affected Chapters": "§9",
            },
            status="Draft",
            template_version=4,
        )

        self.assertTrue(
            any("### 9.1 Interface Inventory" in error for error in errors)
        )
        self.assertTrue(
            any("### 9.2 Per-interface Detailed Contracts" in error for error in errors)
        )


if __name__ == "__main__":
    unittest.main()
