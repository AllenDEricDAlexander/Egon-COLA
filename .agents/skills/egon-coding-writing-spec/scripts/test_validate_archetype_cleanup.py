#!/usr/bin/env python3
"""Behavioral regression cases for first-business archetype cleanup decisions."""

from __future__ import annotations

import unittest

from validate_spec import (
    ARCHETYPE_CLEANUP_TABLE,
    CURRENT_TEMPLATE_VERSION,
    SUPPORTED_TEMPLATE_VERSIONS,
    validate_archetype_cleanup,
    validate_v2_content,
)


def row(
    action: str = "Delete",
    example_id: str = "EXAMPLE-001",
    paths: str = "src/main/java/shop/example/CourseController.java:getCourse",
    purpose: str = "REQ-001 retires the unused teaching example",
    origin: str = "Matching source-projects Web controller and generated README",
    handling: str = "Remove sample route, Bean registration and unused XML references",
) -> str:
    return (
        f"| {example_id} | `{paths}` | {origin} | {action} | {purpose} | "
        f"{handling} | TEST-001 scans retired registrations and compiles the affected modules |"
    )


def document(
    disposition: str = "Required",
    rows: list[str] | None = None,
    scan_scope: str = "All target modules",
    verdict: str = "PASS — Ready for user review",
) -> str:
    lines = [
        "## 2. Background and Current State",
        "### 2.5 Archetype example cleanup",
        f"Cleanup disposition: {disposition}",
    ]
    if disposition == "Not applicable":
        lines.append("Reason: This task maintains the code-generator component, not a generated business project.")
    else:
        lines.extend(
            [
                "Target project: `shop/` and all seven business-project modules",
                "Origin evidence: Generated Web POM/README and current source-projects comparison",
                f"Scan scope: {scan_scope}",
                "Scan evidence: Source/resources/tests/docs/configuration and entry registrations in all modules were inspected",
            ]
        )
        if disposition == "Complete":
            lines.append("Residual evidence: Current source and registration scans show only the accepted shop behavior and technical wiring")
    selected = rows if rows is not None else ([row()] if disposition == "Required" else [])
    if selected:
        lines.extend([ARCHETYPE_CLEANUP_TABLE, "| --- | --- | --- | --- | --- | --- | --- |", *selected])
    lines.extend(
        [
            "## 3. Goals and Non-goals",
            "### 3.3 Change Surface and Design Depth",
            "| Area/layer | Disposition | Exact repository evidence | Changed or preserved behavior/contract | Required Spec treatment | Chapter(s) |",
            "| --- | --- | --- | --- | --- | --- |",
            "| Archetype example cleanup | Affected | `shop/` example chain | Retire unused routes and references | Exact removals and tests | §8, §14 |",
            "## 4. Requirements and Acceptance Criteria",
            "REQ-001 Remove unused teaching examples before shop behavior is accepted.",
            "UC-001 Shop users place orders through real business endpoints.",
            "## 8. Package Structure and Code File Tree",
            "EXAMPLE-001 owns the sample controller/route deletion and affected caller updates.",
            "## 14. Test Design",
            "TEST-001 proves EXAMPLE-001 is absent from source and registrations without starting a service.",
            "## 19. Traceability Matrix",
            "REQ-001 maps to EXAMPLE-001, exact deletions and TEST-001.",
            "## 20. Review and Acceptance",
            verdict,
        ]
    )
    return "\n".join(lines)


class ArchetypeCleanupValidationTest(unittest.TestCase):
    def check(self, text: str, disposition: str = "Required", status: str = "Review") -> list[str]:
        return validate_archetype_cleanup(text, {"Archetype Cleanup": disposition}, status)

    def test_whole_project_cleanup_with_requirement_and_absence_tests_is_valid(self) -> None:
        self.assertEqual([], self.check(document()))

    def test_local_feature_scan_does_not_close_project_remnants(self) -> None:
        errors = self.check(document(scan_scope="Only the order feature"))
        self.assertTrue(any("not feature-local" in error for error in errors))

    def test_creation_history_can_be_missing_when_current_comparison_is_evidenced(self) -> None:
        text = document().replace(
            "Generated Web POM/README and current source-projects comparison",
            "No origin record; shop POM/README and source-projects signatures prove the current example provenance",
        )
        self.assertEqual([], self.check(text))

    def test_pending_cleanup_cannot_be_reported_complete(self) -> None:
        errors = self.check(document("Complete", rows=[row()]), "Complete")
        self.assertTrue(any("cannot contain Delete" in error for error in errors))

    def test_fresh_complete_assessment_avoids_requiring_new_deletions(self) -> None:
        self.assertEqual([], self.check(document("Complete"), "Complete"))

    def test_complete_requires_residual_evidence(self) -> None:
        text = document("Complete").replace(
            "Residual evidence: Current source and registration scans show only the accepted shop behavior and technical wiring",
            "Residual evidence: None",
        )
        self.assertTrue(any("Residual evidence" in error for error in self.check(text, "Complete")))

    def test_component_maintenance_is_evidence_backed_not_applicable(self) -> None:
        self.assertEqual([], self.check(document("Not applicable"), "Not applicable"))

    def test_not_applicable_cannot_hide_an_inventory_or_missing_reason(self) -> None:
        text = document("Not applicable", rows=[row()]).replace(
            "Reason: This task maintains the code-generator component, not a generated business project.",
            "Reason: N/A",
        )
        errors = self.check(text, "Not applicable")
        self.assertTrue(any("Reason evidence" in error for error in errors))
        self.assertTrue(any("cannot carry an example inventory" in error for error in errors))

    def test_keep_for_reference_is_not_a_cleanup_decision(self) -> None:
        errors = self.check(document(rows=[row(action="Keep for reference")]))
        self.assertTrue(any("reference-only retention is prohibited" in error for error in errors))

    def test_real_business_requires_requirement_and_use_case_evidence(self) -> None:
        candidate = row(action="Real business", example_id="EXAMPLE-002", purpose="REQ-001 adapted account behavior")
        errors = self.check(document(rows=[row(), candidate]))
        self.assertTrue(any("Real business requires" in error for error in errors))
        with_use_case = candidate.replace("REQ-001 adapted", "REQ-001 / UC-001 adapted")
        self.assertEqual([], self.check(document(rows=[row(), with_use_case])))

    def test_required_cleanup_must_be_an_explicit_change_surface(self) -> None:
        text = document().replace("| Archetype example cleanup | Affected |", "| Archetype example cleanup | Unchanged |")
        self.assertTrue(any("must be Affected" in error for error in self.check(text)))

    def test_missing_file_or_test_mapping_is_rejected(self) -> None:
        text = document().replace(
            "EXAMPLE-001 owns the sample controller/route deletion and affected caller updates.",
            "Only create the order controller.",
        )
        self.assertTrue(any("§8" in error for error in self.check(text)))
        text = document().replace(
            "TEST-001 proves EXAMPLE-001 is absent from source and registrations without starting a service.",
            "TEST-001 verifies only order behavior.",
        )
        self.assertTrue(any("§14" in error for error in self.check(text)))

    def test_cleanup_requirement_must_exist_and_be_traced(self) -> None:
        text = document().replace("REQ-001 maps to EXAMPLE-001, exact deletions and TEST-001.", "No cleanup trace.")
        self.assertTrue(any("defined in §4 and traced in §19" in error for error in self.check(text)))

    def test_wildcard_only_deletion_is_rejected(self) -> None:
        errors = self.check(document(rows=[row(paths="src/**/User*")]))
        self.assertTrue(any("exact, not wildcard-only" in error for error in errors))

    def test_applied_history_is_limited_to_evidenced_sql(self) -> None:
        history = row(
            action="Applied history",
            example_id="EXAMPLE-002",
            paths="src/main/resources/db/V001__sample.sql",
            purpose="Preserve immutable checksum-producing history",
            origin="Applied SQL is recorded in the existing managed-history evidence",
            handling="Retain applied bytes; the new corrective version retires the sample schema",
        )
        self.assertEqual([], self.check(document(rows=[row(), history])))
        errors = self.check(document(rows=[row(), history.replace("V001__sample.sql", "SampleController.java")]))
        self.assertTrue(any("limited to evidenced applied SQL" in error for error in errors))

    def test_infrastructure_can_be_retained_for_real_runtime_purpose(self) -> None:
        technical = row(
            action="Infrastructure", example_id="EXAMPLE-002", paths="src/main/java/shop/config/TenantConfiguration.java",
            purpose="Tenant isolation required by every shop repository", handling="Keep tenant Bean wiring used by real repositories",
        )
        self.assertEqual([], self.check(document(rows=[row(), technical])))

    def test_blocked_ownership_is_draft_only_and_prevents_pass(self) -> None:
        blocked = row(action="Blocked", purpose="Published facade consumers need a user decision before retirement")
        draft = document(rows=[blocked], verdict="BLOCKED — User decision required")
        self.assertEqual([], self.check(draft, status="Draft"))
        self.assertTrue(any("cannot use a PASS" in error for error in self.check(document(rows=[blocked]))))

    def test_duplicate_ids_and_empty_evidence_do_not_close_candidates(self) -> None:
        errors = self.check(document(rows=[row(), row(origin="None")]))
        self.assertTrue(any("unique EXAMPLE-NNN" in error for error in errors))
        self.assertTrue(any("requires concrete" in error for error in errors))

    def test_header_and_assessment_disposition_must_match(self) -> None:
        self.assertTrue(any("must match" in error for error in self.check(document("Complete"))))

    def test_v8_dispatch_requires_assessment_without_invalidating_v7(self) -> None:
        text = "## 2. Background and Current State\nExisting mapper query."
        fields = {"Complexity": "Simple", "Complexity Drivers": "None"}
        old_errors = validate_v2_content(text, fields, "Draft", template_version=7)
        new_errors = validate_v2_content(text, fields, "Draft", template_version=8)
        self.assertFalse(any("Archetype" in error or "archetype" in error for error in old_errors))
        self.assertTrue(any("Archetype Cleanup" in error for error in new_errors))
        self.assertTrue(any("2.5" in error for error in new_errors))
        self.assertEqual(9, CURRENT_TEMPLATE_VERSION)
        self.assertTrue(set(range(2, 9)) <= SUPPORTED_TEMPLATE_VERSIONS)


if __name__ == "__main__":
    unittest.main()
