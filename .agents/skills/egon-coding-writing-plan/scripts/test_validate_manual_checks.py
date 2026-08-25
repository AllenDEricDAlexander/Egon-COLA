#!/usr/bin/env python3
"""Regression tests for Plan Template Version 3 blocking Manual Checks."""

from __future__ import annotations

import unittest

from validate_plan import MANUAL_CHECK_IDS, validate_manual_checks


def manual_section(overrides: dict[str, tuple[str, str, str, str, str]] | None = None) -> str:
    overrides = overrides or {}
    rows = []
    for check_id in MANUAL_CHECK_IDS:
        applicability, status, evidence, finding, action = overrides.get(
            check_id,
            ("Applicable", "PASS", f"repository evidence for {check_id}", "requirement satisfied", "None"),
        )
        rows.append(
            f"| `{check_id}` | {applicability} | {status} | {evidence} | {finding} | {action} |"
        )
    return "\n".join(
        [
            "## 12. Review and Acceptance",
            "",
            "### 12.5 Blocking Manual Check",
            "",
            "| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |",
            "| --- | --- | --- | --- | --- | --- |",
            *rows,
            "",
            "### 12.6 Final verdict",
        ]
    )


class ManualCheckValidationTest(unittest.TestCase):
    def test_all_pass_and_evidenced_na_are_valid(self) -> None:
        text = manual_section(
            {"MC-JSON-001": ("Not applicable", "N/A", "no external JSON contract", "not in change tree", "None")}
        )
        self.assertEqual([], validate_manual_checks(text, pass_verdict=True))

    def test_missing_id_is_rejected(self) -> None:
        text = manual_section().replace(
            "| `MC-TIME-001` | Applicable | PASS | repository evidence for MC-TIME-001 | requirement satisfied | None |\n",
            "",
        )
        errors = validate_manual_checks(text, pass_verdict=False)
        self.assertTrue(any("Missing blocking Manual Check ID: MC-TIME-001" in error for error in errors))

    def test_blocked_plan_is_structurally_valid_with_summary_blocker(self) -> None:
        text = manual_section(
            {
                "MC-DEP-001": ("Applicable", "BLOCKED", "dependency absent from manifests", "capability gap approval pending", "User approves coordinate"),
                "MC-BLOCKER-001": ("Applicable", "BLOCKED", "MC-DEP-001 remains open", "one blocker remains", "User decides dependency"),
            }
        )
        self.assertEqual([], validate_manual_checks(text, pass_verdict=False))

    def test_pass_verdict_rejects_blocker(self) -> None:
        text = manual_section(
            {
                "MC-DEP-001": ("Applicable", "FAIL", "reuse ledger shows gap", "dependency not approved", "Obtain user approval"),
                "MC-BLOCKER-001": ("Applicable", "FAIL", "MC-DEP-001 failed", "manual gate remains open", "Close MC-DEP-001"),
            }
        )
        errors = validate_manual_checks(text, pass_verdict=True)
        self.assertTrue(any("PASS verdict requires every Manual Check" in error for error in errors))

    def test_na_requires_concrete_evidence_and_finding(self) -> None:
        text = manual_section(
            {"MC-CONFIG-001": ("Not applicable", "N/A", "N/A", "None", "None")}
        )
        errors = validate_manual_checks(text, pass_verdict=False)
        self.assertTrue(any("MC-CONFIG-001 requires concrete evidence" in error for error in errors))
        self.assertTrue(any("MC-CONFIG-001 requires concrete finding" in error for error in errors))


if __name__ == "__main__":
    unittest.main()
