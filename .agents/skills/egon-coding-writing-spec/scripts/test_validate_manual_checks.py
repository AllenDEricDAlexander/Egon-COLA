#!/usr/bin/env python3
"""Regression tests for blocking Manual Check validation."""

from __future__ import annotations

import unittest

from validate_spec import MANUAL_CHECK_IDS, validate_manual_checks


def document(statuses: dict[str, tuple[str, str]] | None = None, omit: str | None = None) -> str:
    configured = statuses or {}
    rows: list[str] = []
    for check_id in MANUAL_CHECK_IDS:
        if check_id == omit:
            continue
        status, action = configured.get(check_id, ("PASS", "None"))
        applicability = "Not applicable" if status == "N/A" else "Applicable"
        rows.append(
            f"| `{check_id}` | {applicability} | {status} | "
            f"`src/{check_id}.java:Symbol` | Verified {check_id} | {action} |"
        )
    return "\n".join(
        [
            "## 20. Review and Acceptance",
            "",
            "### 20.5 Blocking Manual Check",
            "",
            "| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |",
            "| --- | --- | --- | --- | --- | --- |",
            *rows,
            "",
            "### 20.6 Final verdict",
        ]
    )


class ManualCheckValidationTest(unittest.TestCase):
    def test_all_pass_and_evidenced_na_allow_pass_verdict(self) -> None:
        text = document({"MC-CONFIG-001": ("N/A", "None")})

        self.assertEqual([], validate_manual_checks(text, pass_verdict=True))

    def test_missing_check_id_is_rejected(self) -> None:
        text = document(omit="MC-TIME-001")

        errors = validate_manual_checks(text, pass_verdict=False)

        self.assertTrue(any("Missing blocking Manual Check ID: MC-TIME-001" in error for error in errors))

    def test_blocked_report_is_structurally_valid_without_pass_verdict(self) -> None:
        text = document(
            {
                "MC-DEP-001": ("BLOCKED", "User must approve dependency"),
                "MC-BLOCKER-001": ("BLOCKED", "Close MC-DEP-001"),
            }
        )

        self.assertEqual([], validate_manual_checks(text, pass_verdict=False))

    def test_pass_verdict_is_rejected_while_any_check_is_blocked(self) -> None:
        text = document(
            {
                "MC-DEP-001": ("BLOCKED", "User must approve dependency"),
                "MC-BLOCKER-001": ("BLOCKED", "Close MC-DEP-001"),
            }
        )

        errors = validate_manual_checks(text, pass_verdict=True)

        self.assertTrue(any("PASS verdict requires every Manual Check" in error for error in errors))

    def test_na_requires_concrete_evidence_and_finding(self) -> None:
        text = document({"MC-CONFIG-001": ("N/A", "None")}).replace(
            "`src/MC-CONFIG-001.java:Symbol` | Verified MC-CONFIG-001",
            "N/A | N/A",
        )

        errors = validate_manual_checks(text, pass_verdict=False)

        self.assertTrue(any("MC-CONFIG-001 requires concrete evidence" in error for error in errors))
        self.assertTrue(any("MC-CONFIG-001 requires concrete finding" in error for error in errors))


if __name__ == "__main__":
    unittest.main()
