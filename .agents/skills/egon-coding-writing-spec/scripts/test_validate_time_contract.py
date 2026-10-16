#!/usr/bin/env python3
"""Time-policy conflicts and backwards-compatible document validation."""

from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from validate_spec import TIME_CONTRACT_TABLE, validate_time_contract, validate

HEADING = "### 6.3 Time semantics and conversion contract"


def row(**overrides: str) -> str:
    cells = {
        "id": "TIME-001", "fields": "OrderPO.paidAt / PaymentResponse.paidAt",
        "semantics": "Occurred instant", "type": "Instant", "storage": "UTC instant",
        "wire": "UTC Z", "business": "tenant.businessZone, validated IANA ID",
        "display": "user preference then browser fallback; reports use businessZone",
        "boundary": "Normalize offset input; business next-day boundary becomes exclusive UTC end",
        "precision": "DB micros / wire micros; injected Clock.systemUTC()",
        "compatibility": "Not applicable: no affected legacy field",
        "verification": "Step 1; TEST-001 UTC round trip and Shanghai calendar boundaries",
    }
    cells.update(overrides)
    return "| " + " | ".join(cells.values()) + " |"


def document(rows: list[str] | None = None) -> str:
    return "\n".join([
        HEADING, "Time contract: Required", TIME_CONTRACT_TABLE,
        "| " + " | ".join(["---"] * 12) + " |",
        *(rows if rows is not None else [row()]),
    ])


class TimeContractValidationTest(unittest.TestCase):
    def check(self, text: str) -> list[str]:
        return validate_time_contract(text, HEADING)

    def test_utc_instant_and_browser_display_are_valid(self) -> None:
        self.assertEqual([], self.check(document()))

    def test_all_semantics_with_distinct_storage_and_wire_are_valid(self) -> None:
        definitions = [
            ("Pure date", "LocalDate", "Pure date", "Pure date"),
            ("Local time", "LocalTime", "Local time", "Local"),
            ("Zoned schedule", "LocalTime plus ZoneId", "Local + zone", "Local + zone"),
            ("Duration", "Duration", "Duration", "Duration"),
        ]
        for meaning, runtime_type, storage, wire in definitions:
            with self.subTest(meaning=meaning):
                self.assertEqual([], self.check(document([row(semantics=meaning, type=runtime_type, storage=storage, wire=wire)])))

    def test_pure_date_cannot_be_stored_or_transmitted_as_an_instant(self) -> None:
        errors = self.check(document([row(semantics="Pure date", type="LocalDate")]))
        self.assertTrue(any("storage policy conflicts" in error for error in errors))
        self.assertTrue(any("wire policy conflicts" in error for error in errors))

    def test_local_datetime_is_not_a_new_instant_type(self) -> None:
        errors = self.check(document([row(type="LocalDateTime")]))
        self.assertTrue(any("Java type conflicts" in error for error in errors))

    def test_naive_or_offset_output_cannot_replace_utc_z_wire_policy(self) -> None:
        for wire in ["Local", "Local + zone", "OffsetDateTime", "yyyy-MM-dd HH:mm:ss"]:
            errors = self.check(document([row(wire=wire)]))
            self.assertTrue(any("wire policy conflicts" in error for error in errors))

    def test_legacy_utc_requires_conversion_evidence(self) -> None:
        candidate = row(type="LocalDateTime", storage="Legacy UTC", compatibility="Existing deletedAt UTC column; read/write conversion at repository boundary")
        self.assertEqual([], self.check(document([candidate])))
        errors = self.check(document([row(type="LocalDateTime", storage="Legacy UTC")]))
        self.assertTrue(any("Legacy UTC requires" in error for error in errors))

    def test_runtime_type_can_be_non_java_or_a_generic_carrier(self) -> None:
        for runtime_type in ["Temporal.Instant", "Optional<Instant>"]:
            self.assertEqual([], self.check(document([row(type=runtime_type)])))

    def test_business_zone_cannot_come_from_host_or_browser_defaults(self) -> None:
        for zone in ["ZoneId.systemDefault()", "browser timezone", "server default", "服务器默认时区"]:
            self.assertTrue(any("business zone must be explicit" in error for error in self.check(document([row(business=zone)]))))

    def test_zoned_schedule_requires_a_zone_source(self) -> None:
        errors = self.check(document([row(semantics="Zoned schedule", type="LocalDateTime plus ZoneId", storage="Local + zone", wire="Local + zone", business="Not applicable: no configured zone")]))
        self.assertTrue(any("region-zone source" in error for error in errors))

    def test_unpersisted_internal_time_does_not_force_a_database_or_api_change(self) -> None:
        self.assertEqual([], self.check(document([row(storage="Not persisted", wire="Internal", display="Not applicable: no UI boundary")])) )

    def test_not_applicable_requires_reason_and_no_inventory(self) -> None:
        text = HEADING + "\nTime contract: Not applicable\nReason: Only a naming correction; all time fields and contracts are unchanged"
        self.assertEqual([], self.check(text))
        self.assertTrue(self.check(text.replace("Reason: Only a naming correction; all time fields and contracts are unchanged", "Reason: N/A")))
        text = document().replace("Time contract: Required", "Time contract: Not applicable")
        self.assertTrue(any("cannot contain TIME rows" in error for error in self.check(text)))

    def test_missing_duplicate_malformed_or_placeholder_rows_are_rejected(self) -> None:
        for text in [document([]), document([row(), row()]), document(["| TIME-001 | missing cells |"]), document([row(precision="<exact precision>")]), document([row(boundary="N/A")])]:
            self.assertTrue(self.check(text))

    def test_explained_non_applicability_accepts_chinese_but_not_placeholders(self) -> None:
        self.assertEqual([], self.check(document([row(display="Not applicable: 无界面")])) )
        for evidence in ["Not applicable: <reason>", "Not applicable: TBD"]:
            self.assertTrue(self.check(document([row(display=evidence)])))

    def test_duplicate_applicability_cannot_hide_a_conflict(self) -> None:
        text = document().replace("Time contract: Required", "Time contract: Required\nTime contract: Not applicable")
        self.assertTrue(any("exactly one" in error for error in self.check(text)))

    def test_verification_requires_test_identity(self) -> None:
        self.assertTrue(any("TEST-NNN" in error for error in self.check(document([row(verification="Step 1; inspect the output")]))))

    def test_required_time_contract_cannot_hide_manual_check(self) -> None:
        text = document() + "\n## Review\n| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |\n| --- | --- | --- | --- | --- | --- |\n| MC-TIME-001 | Not applicable | N/A | old types | no time change | None |"
        self.assertTrue(any("MC-TIME-001" in error for error in self.check(text)))

    def test_plan_contract_has_an_owning_step(self) -> None:
        text = document([row(verification="TEST-001 round trip")])
        errors = self.check(text)
        if HEADING.startswith("### 4."):
            self.assertTrue(any("owning Step" in error for error in errors))
        else:
            self.assertEqual([], errors)

    def test_new_version_dispatch_requires_time_section_while_old_version_does_not(self) -> None:
        template = (Path(__file__).resolve().parent.parent / "assets" / "spec-template.md").read_text()
        start = template.index(HEADING)
        end = template.index("\n## ", start)
        text = template[:start] + template[end:]
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "2026-10-13-12-00-time-policy.md"
            path.write_text(text)
            new_errors, _ = validate(path, strict=False)
            path.write_text(text.replace("| Template Version | `9` |", "| Template Version | `8` |"))
            old_errors, _ = validate(path, strict=False)
        self.assertTrue(any("Missing required time subsection" in error for error in new_errors))
        self.assertFalse(any("time subsection" in error or "Time contract" in error for error in old_errors))


if __name__ == "__main__":
    unittest.main()
