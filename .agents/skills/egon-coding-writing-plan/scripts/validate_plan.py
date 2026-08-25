#!/usr/bin/env python3
"""Validate an EGON coding Plan's metadata, Spec links, steps, and coverage."""

from __future__ import annotations

import argparse
import re
from pathlib import Path

FILENAME_RE = re.compile(
    r"^(?P<minute>\d{4}-\d{2}-\d{2}-\d{2}-\d{2})-"
    r"(?P<abstract>[a-z0-9]+(?:-[a-z0-9]+)*)\.md$"
)
TITLE_RE = re.compile(r"\A#\s+\S.+$", re.MULTILINE)
DATE_TIME_RE = re.compile(r"^(\d{4}-\d{2}-\d{2}) (\d{2}):(\d{2}) \S+$")
SOURCE_ID_RE = re.compile(
    r"(?<![A-Z0-9-])(?:PLAN-REQ-\d{3}|[A-Z][A-Z0-9]{1,11}-\d{2,4})\b"
)
REQ_ID_RE = re.compile(r"\bREQ-\d{3}\b")
VALID_STATUSES = {"Draft", "Review", "Ready", "In Progress", "Completed", "Blocked", "Superseded"}
CURRENT_TEMPLATE_VERSION = 4
SUPPORTED_TEMPLATE_VERSIONS = {2, 3, 4}
MANUAL_CHECK_IDS = (
    "MC-ARCH-001",
    "MC-REUSE-001",
    "MC-DEP-001",
    "MC-NAME-001",
    "MC-VALID-001",
    "MC-MODEL-001",
    "MC-CONVERT-001",
    "MC-LOG-001",
    "MC-BEAN-001",
    "MC-UTIL-001",
    "MC-JSON-001",
    "MC-TIME-001",
    "MC-CONFIG-001",
    "MC-PATTERN-001",
    "MC-SCOPE-001",
    "MC-TEST-001",
    "MC-BLOCKER-001",
)
MANUAL_CHECK_RE = re.compile(r"\bMC-[A-Z]+-\d{3}\b")
LITERAL_RULE_NUMBERS = (1, 2, 3, 4, 5, 6, 7, 9, 10, 11)
LITERAL_RULE_RE = re.compile(r"\bRule\s+(1|2|3|4|5|6|7|9|10|11)\b")
ANY_LITERAL_RULE_RE = re.compile(r"\bRule\s+(\d+)\b")
REQUIRED_FIELDS = [
    "Document",
    "Status",
    "Created",
    "Updated",
    "Owner",
    "Repository",
    "Scope",
    "Source Requirement",
    "Baseline Revision",
    "Implements Spec",
    "Spec Status",
    "Spec Revision",
    "Effective Specs",
    "Depends On Plans",
    "Supersedes",
    "Superseded By",
    "Related Plans",
]
REQUIRED_HEADINGS = [
    "## 1. Summary",
    "## 2. Target Spec and Effective Design",
    "## 3. Effective Requirements and Acceptance",
    "## 4. Implementation Strategy and Dependency Order",
    "## 5. Change File Tree",
    "## 6. Prerequisites, Constraints, and Plan Clarifications",
    "## 7. Ordered File-by-file Implementation Steps",
    "## 8. Test, Validation, and Quality Gates",
    "## 9. Migration, Compatibility, Rollout, and Rollback",
    "## 10. Requirement-to-Step Traceability Matrix",
    "## 11. Risks, Blockers, and User Decisions",
    "## 12. Review and Acceptance",
]
STEP_MARKERS_V1 = [
    "- Requirements:",
    "- Dependencies:",
    "- Observable outcome:",
    "- Ordered files:",
    "- Verification command:",
    "- Expected result:",
    "- Completion criteria:",
    "- Rollback:",
    "- Commit:",
]
STEP_MARKERS_V2 = [
    *STEP_MARKERS_V1[:2],
    "- Baseline state:",
    *STEP_MARKERS_V1[2:3],
    "- End state:",
    "- Test-first gate:",
    *STEP_MARKERS_V1[3:4],
    "- Validation working directory:",
    *STEP_MARKERS_V1[4:6],
    "- Failure returns to:",
    *STEP_MARKERS_V1[6:8],
    "- Commit paths:",
    *STEP_MARKERS_V1[8:],
]
FILE_MARKERS_V1 = [
    "- Purpose:",
    "- Symbols:",
    "- Why now:",
    "- Contract/signature changes:",
    "- Implementation pseudocode:",
    "- After this file:",
]
FILE_MARKERS_V2 = [
    *FILE_MARKERS_V1[:2],
    "- Repository evidence:",
    "- Dependencies and consumers:",
    *FILE_MARKERS_V1[2:4],
    "- Input/output and state mapping:",
    "- Error and edge behavior:",
    *FILE_MARKERS_V1[4:5],
    "- Verification contribution:",
    *FILE_MARKERS_V1[5:],
]
STEP_MARKERS_V3 = [
    *STEP_MARKERS_V2[:6],
    "- Manual Checks:",
    *STEP_MARKERS_V2[6:],
]
FILE_MARKERS_V3 = [
    *FILE_MARKERS_V2[:8],
    "- Standards impact:",
    *FILE_MARKERS_V2[8:],
]
STEP_MARKERS_V4 = [
    *STEP_MARKERS_V3[:7],
    "- Literal Rules:",
    *STEP_MARKERS_V3[7:],
]
FILE_MARKERS_V4 = [
    *FILE_MARKERS_V3[:9],
    "- Literal rule enforcement:",
    *FILE_MARKERS_V3[9:],
]
PLACEHOLDER_PATTERNS = [
    re.compile(r"\b(?:TBD|TODO|FIXME|XXX)\b", re.IGNORECASE),
    re.compile(
        r"<\s*(?:implementation plan title|decision owner|repository|modules?|bounded context|"
        r"user request|issue|ticket|brief|commit|path|symbol|command|primary spec|relative link)[^>]*>",
        re.IGNORECASE,
    ),
    re.compile(r"YYYY-MM-DD(?:-HH-MM)?"),
    re.compile(r"\bABSTRACT\b"),
]
GENERIC_PSEUDOCODE = re.compile(
    r"\b(?:implement (?:the )?(?:service|logic|validation)|handle errors?|update (?:the )?frontend|run tests?)\b",
    re.IGNORECASE,
)
VERDICTS = {
    "PASS — Ready for user review",
    "BLOCKED — Spec or user decision required",
    "REVISE — Plan and Spec are inconsistent",
}


def clean(value: str) -> str:
    return value.strip().strip("`").strip()


def parse_header_table(text: str) -> dict[str, str]:
    fields: dict[str, str] = {}
    for line in text.splitlines():
        if not line.lstrip().startswith("|"):
            continue
        cells = [cell.strip() for cell in line.strip().strip("|").split("|")]
        if len(cells) != 2:
            continue
        key, value = cells
        if key in {"Field", "---"} or set(key) == {"-"}:
            continue
        if key and key not in fields:
            fields[key] = value
    return fields


def section(text: str, heading: str) -> str:
    start = text.find(heading)
    if start < 0:
        return ""
    body_start = start + len(heading)
    next_heading = re.search(r"(?m)^##\s+\d+\.", text[body_start:])
    end = body_start + next_heading.start() if next_heading else len(text)
    return text[body_start:end]


def heading_body(text: str, heading: str) -> str:
    """Return one Markdown heading body up to the next same-or-higher heading."""
    start = text.find(heading)
    if start < 0:
        return ""
    level_match = re.match(r"^(#+)\s", heading)
    if not level_match:
        return ""
    level = len(level_match.group(1))
    body_start = start + len(heading)
    next_heading = re.search(rf"(?m)^#{{1,{level}}}\s+", text[body_start:])
    end = body_start + next_heading.start() if next_heading else len(text)
    return text[body_start:end]


def markdown_table_rows(text: str, first_header_cell: str) -> list[list[str]]:
    """Return data rows from the first Markdown table with the requested header."""
    lines = text.splitlines()
    for index, line in enumerate(lines):
        if not line.lstrip().startswith("|"):
            continue
        cells = [cell.strip() for cell in line.strip().strip("|").split("|")]
        if not cells or clean(cells[0]) != first_header_cell:
            continue
        separator_index = index + 1
        if separator_index >= len(lines) or not lines[separator_index].lstrip().startswith("|"):
            return []
        rows: list[list[str]] = []
        for row in lines[separator_index + 1:]:
            if not row.lstrip().startswith("|"):
                break
            row_cells = [cell.strip() for cell in row.strip().strip("|").split("|")]
            if row_cells and any(cell for cell in row_cells):
                rows.append(row_cells)
        return rows
    return []


def validate_manual_checks(text: str, pass_verdict: bool) -> list[str]:
    """Validate the blocking Manual Check catalog and Plan verdict consistency."""
    errors: list[str] = []
    review = section(text, "## 12. Review and Acceptance")
    heading = "### 12.5 Blocking Manual Check"
    if heading not in review:
        return [f"Template Version 3 is missing required subsection: {heading}"]

    body = heading_body(review, heading)
    if "| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |" not in body:
        errors.append("Blocking Manual Check requires the canonical six-column table")
    rows = markdown_table_rows(body, "Check ID")
    rows_by_id: dict[str, list[str]] = {}
    for row_number, row in enumerate(rows, start=1):
        if len(row) < 6:
            errors.append(f"Manual Check row {row_number} requires six columns; found {len(row)}")
            continue
        check_id, applicability, status, evidence, finding, action = [clean(cell) for cell in row[:6]]
        if check_id in rows_by_id:
            errors.append(f"Duplicate Manual Check ID: {check_id}")
            continue
        rows_by_id[check_id] = [applicability, status, evidence, finding, action]

    expected = set(MANUAL_CHECK_IDS)
    actual = set(rows_by_id)
    for check_id in sorted(expected - actual):
        errors.append(f"Missing blocking Manual Check ID: {check_id}")
    for check_id in sorted(actual - expected):
        errors.append(f"Unknown blocking Manual Check ID: {check_id}")

    unresolved: list[str] = []
    for check_id in MANUAL_CHECK_IDS:
        row = rows_by_id.get(check_id)
        if not row:
            continue
        applicability, status, evidence, finding, action = row
        if applicability not in {"Applicable", "Not applicable"}:
            errors.append(
                f"{check_id} Applicability must be Applicable or Not applicable: {applicability}"
            )
        if applicability == "Applicable" and status not in {"PASS", "FAIL", "BLOCKED"}:
            errors.append(f"{check_id} applicable status must be PASS, FAIL, or BLOCKED: {status}")
        if applicability == "Not applicable" and status != "N/A":
            errors.append(f"{check_id} not-applicable status must be N/A: {status}")
        for label, value in (("Evidence", evidence), ("Finding", finding)):
            if not value or value.lower() in {"none", "n/a", "unknown", "tbd", "todo"}:
                errors.append(f"{check_id} requires concrete {label.lower()}")
        if status in {"FAIL", "BLOCKED"}:
            unresolved.append(check_id)
            if not action or action.lower() in {"none", "n/a", "unknown", "tbd", "todo"}:
                errors.append(f"{check_id} {status} requires an exact action/owner")

    blocker_status = rows_by_id.get("MC-BLOCKER-001", ["", "", "", "", ""])[1]
    unresolved_without_summary = [item for item in unresolved if item != "MC-BLOCKER-001"]
    if unresolved_without_summary and blocker_status not in {"FAIL", "BLOCKED"}:
        errors.append(
            "MC-BLOCKER-001 must be FAIL or BLOCKED while other Manual Checks are unresolved"
        )
    if not unresolved_without_summary and blocker_status not in {"PASS", ""}:
        errors.append("MC-BLOCKER-001 must be PASS when all other Manual Checks are closed")
    if pass_verdict:
        nonpassing = [
            check_id for check_id, row in rows_by_id.items() if row[1] not in {"PASS", "N/A"}
        ]
        if nonpassing:
            errors.append(
                "PASS verdict requires every Manual Check to be PASS or evidence-backed N/A: "
                + ", ".join(sorted(nonpassing))
            )
    return errors


def markdown_links(value: str) -> list[str]:
    return [link.strip() for link in re.findall(r"\[[^\]]+\]\(([^)]+)\)", value)]


def resolve_link(owner: Path, link: str) -> Path | None:
    target = link.split("#", 1)[0]
    if not target or re.match(r"^[a-z][a-z0-9+.-]*://", target, re.IGNORECASE) or Path(target).is_absolute():
        return None
    return (owner.parent / target).resolve()


def validate_link_field(path: Path, field: str, value: str, require_link: bool) -> tuple[list[str], list[Path]]:
    errors: list[str] = []
    resolved: list[Path] = []
    normalized = clean(value)
    if normalized.lower() == "none":
        if require_link:
            errors.append(f"{field} must contain a repository-relative Markdown link")
        return errors, resolved

    links = markdown_links(value)
    if not links:
        return [f"{field} must be None or contain repository-relative Markdown links"], resolved

    for link in links:
        target = resolve_link(path, link)
        if target is None:
            errors.append(f"{field} contains an invalid or non-relative link: {link}")
        elif not target.is_file():
            errors.append(f"{field} link does not exist: {target}")
        else:
            resolved.append(target)
    return errors, resolved


def extract_steps(text: str) -> list[tuple[int, str]]:
    body = section(text, "## 7. Ordered File-by-file Implementation Steps")
    matches = list(re.finditer(r"(?m)^###\s+Step\s+(\d+)\s+[—-]\s+\S.+$", body))
    steps: list[tuple[int, str]] = []
    for index, match in enumerate(matches):
        end = matches[index + 1].start() if index + 1 < len(matches) else len(body)
        steps.append((int(match.group(1)), body[match.start():end]))
    return steps


def extract_file_blocks(step: str) -> list[tuple[int, str, str, str]]:
    pattern = re.compile(
        r"(?m)^####\s+File\s+(\d+)\s+[—-]\s+`"
        r"(CREATE|MODIFY|DELETE|RENAME|GENERATED)\s+([^`]+)`$"
    )
    matches = list(pattern.finditer(step))
    files: list[tuple[int, str, str, str]] = []
    for index, match in enumerate(matches):
        end = matches[index + 1].start() if index + 1 < len(matches) else len(step)
        tail = step[match.start():end]
        # Step-level markers begin after the final file and are not file content.
        step_tail = re.search(r"(?m)^- Verification command:", tail)
        if step_tail:
            tail = tail[:step_tail.start()]
        files.append((int(match.group(1)), match.group(2), match.group(3).strip(), tail))
    return files


def parse_spec(path: Path) -> tuple[str, set[str]]:
    text = path.read_text(encoding="utf-8")
    fields = parse_header_table(text)
    requirements_section = section(text, "## 4. Requirements and Acceptance Criteria")
    requirements = set(REQ_ID_RE.findall(requirements_section or text))
    return clean(fields.get("Status", "")), requirements


def validate(path: Path, strict: bool) -> tuple[list[str], list[str]]:
    errors: list[str] = []
    warnings: list[str] = []

    if not path.is_file():
        return [f"File does not exist: {path}"], warnings

    text = path.read_text(encoding="utf-8")
    filename_match = FILENAME_RE.fullmatch(path.name)
    if not filename_match:
        errors.append("Filename must match YYYY-MM-DD-HH-MM-lowercase-kebab-abstract.md")
    if not TITLE_RE.search(text):
        errors.append("The first line must be a non-empty level-1 Markdown title")

    fields = parse_header_table(text)
    for field in REQUIRED_FIELDS:
        if field not in fields or not clean(fields[field]):
            errors.append(f"Missing required header field: {field}")

    if fields.get("Document") and path.name not in fields["Document"]:
        errors.append(f"Document header must name the current file: {path.name}")

    status = clean(fields.get("Status", ""))
    if status and status not in VALID_STATUSES:
        errors.append(f"Invalid Status '{status}'. Expected one of: {', '.join(sorted(VALID_STATUSES))}")

    template_version_value = clean(fields.get("Template Version", ""))
    template_version: int | None = None
    if template_version_value:
        if not template_version_value.isdigit():
            errors.append(f"Template Version must be an integer: {template_version_value}")
        elif int(template_version_value) not in SUPPORTED_TEMPLATE_VERSIONS:
            errors.append(
                f"Unsupported Template Version {template_version_value}; supported: "
                f"{', '.join(str(item) for item in sorted(SUPPORTED_TEMPLATE_VERSIONS))}"
            )
        else:
            template_version = int(template_version_value)

    parsed_dates: dict[str, re.Match[str]] = {}
    for field in ("Created", "Updated"):
        value = clean(fields.get(field, ""))
        match = DATE_TIME_RE.fullmatch(value)
        if value and not match:
            errors.append(f"{field} must use YYYY-MM-DD HH:mm ZONE: {value}")
        elif match:
            parsed_dates[field] = match
    if filename_match and "Created" in parsed_dates:
        created = parsed_dates["Created"]
        expected = f"{created.group(1)}-{created.group(2)}-{created.group(3)}"
        if filename_match.group("minute") != expected:
            errors.append(
                "Filename timestamp must match the Created header minute: "
                f"filename={filename_match.group('minute')}, created={expected}"
            )

    for heading in REQUIRED_HEADINGS:
        if heading not in text:
            errors.append(f"Missing required section: {heading}")

    if template_version in {2, 3}:
        required_detailed_headings = [
            "### 4.5 Spec Simplicity and Implementation-necessity Audit",
            "### 4.6 Change-unit Dependency Matrix",
        ]
        for heading in required_detailed_headings:
            if heading not in text:
                errors.append(
                    f"Template Version {template_version} is missing required subsection: {heading}"
                )
        strategy = section(text, "## 4. Implementation Strategy and Dependency Order")
        if "| Spec element | Spec necessity verdict/section | Current repository evidence |" not in strategy:
            errors.append(
                f"Template Version {template_version} requires the Spec simplicity/necessity audit table"
            )
        if "| Change unit | Requirements | Proof/RED point |" not in strategy:
            errors.append(
                f"Template Version {template_version} requires the change-unit dependency matrix"
            )
    if template_version in {3, 4}:
        if "### 4.7 Java, Spring, and Egon-COLA Implementation Standards" not in text:
            errors.append(
                f"Template Version {template_version} is missing required subsection: "
                "### 4.7 Java, Spring, and Egon-COLA Implementation Standards"
            )
        strategy = section(text, "## 4. Implementation Strategy and Dependency Order")
        if "| Concern | Current repository evidence | Effective Spec decision |" not in strategy:
            errors.append(
                f"Template Version {template_version} requires the implementation-standards decision table"
            )
        if "| Need | Candidates inspected | Exact evidence | Fit/gap | Decision |" not in strategy:
            errors.append(f"Template Version {template_version} requires the capability reuse ledger")
    if template_version == 4:
        literal_heading = "### 4.8 User-mandated Java Rule Implementation Matrix"
        if literal_heading not in text:
            errors.append(f"Template Version 4 is missing required subsection: {literal_heading}")
        strategy = section(text, "## 4. Implementation Strategy and Dependency Order")
        literal_body = heading_body(strategy, literal_heading)
        expected_header = (
            "| Literal rule | Spec source | Repository evidence | Exact files and order | "
            "Pseudocode obligations | Validation gate | Steps | Status/blocker |"
        )
        if expected_header not in literal_body:
            errors.append("Template Version 4 requires the canonical literal-rule implementation table")
        rows = markdown_table_rows(literal_body, "Literal rule")
        actual_rules = [clean(row[0]) for row in rows if row]
        expected_rules = [f"Rule {number}" for number in LITERAL_RULE_NUMBERS]
        if actual_rules != expected_rules:
            errors.append(
                "Literal-rule rows must preserve exact order 1,2,3,4,5,6,7,9,10,11: "
                f"{actual_rules}"
            )
        for row_number, row in enumerate(rows, start=1):
            if len(row) < 8:
                errors.append(
                    f"Literal-rule row {row_number} requires eight columns; found {len(row)}"
                )

    implements_errors, primary_paths = validate_link_field(
        path, "Implements Spec", fields.get("Implements Spec", ""), require_link=True
    )
    errors.extend(implements_errors)
    if len(primary_paths) != 1:
        errors.append("Implements Spec must resolve to exactly one file")
        primary_path = None
    else:
        primary_path = primary_paths[0]

    effective_errors, effective_paths = validate_link_field(
        path, "Effective Specs", fields.get("Effective Specs", ""), require_link=True
    )
    errors.extend(effective_errors)
    if primary_path and primary_path not in effective_paths:
        errors.append("Effective Specs must include the primary Implements Spec target")

    for field in ("Depends On Plans", "Supersedes", "Superseded By", "Related Plans"):
        link_errors, _ = validate_link_field(path, field, fields.get(field, ""), require_link=False)
        errors.extend(link_errors)

    effective_requirements: set[str] = set()
    actual_primary_status = ""
    for spec_path in effective_paths:
        spec_status, requirements = parse_spec(spec_path)
        effective_requirements.update(requirements)
        if spec_path == primary_path:
            actual_primary_status = spec_status

    header_spec_status = clean(fields.get("Spec Status", ""))
    if actual_primary_status and header_spec_status and actual_primary_status != header_spec_status:
        errors.append(
            f"Spec Status mismatch: Plan header={header_spec_status}, primary Spec={actual_primary_status}"
        )
    if status in {"Ready", "In Progress", "Completed"} and actual_primary_status not in {"Accepted", "Implemented"}:
        errors.append(
            f"Plan status {status} requires an Accepted/Implemented primary Spec; "
            f"actual={actual_primary_status or 'unknown'}"
        )

    steps = extract_steps(text)
    if not steps:
        errors.append("No '### Step N — ...' implementation Steps found in section 7")
    elif [number for number, _ in steps] != list(range(1, len(steps) + 1)):
        errors.append(f"Step numbers must be contiguous from 1: {[number for number, _ in steps]}")

    step_requirements: set[str] = set()
    for step_number, step in steps:
        if template_version == 4:
            step_markers = STEP_MARKERS_V4
            file_markers = FILE_MARKERS_V4
        elif template_version == 3:
            step_markers = STEP_MARKERS_V3
            file_markers = FILE_MARKERS_V3
        elif template_version == 2:
            step_markers = STEP_MARKERS_V2
            file_markers = FILE_MARKERS_V2
        else:
            step_markers = STEP_MARKERS_V1
            file_markers = FILE_MARKERS_V1
        for marker in step_markers:
            if marker not in step:
                errors.append(f"Step {step_number} missing required marker: {marker}")

        if template_version in {2, 3, 4}:
            test_first = re.search(r"(?m)^- Test-first gate:\s*(.+)$", step)
            if test_first and not re.match(r"(?:`)?(?:Required|Not applicable)\b", test_first.group(1), re.IGNORECASE):
                errors.append(
                    f"Step {step_number} Test-first gate must start with Required or Not applicable"
                )
        if template_version in {3, 4}:
            manual_line = re.search(r"(?m)^- Manual Checks:\s*(.+)$", step)
            manual_ids = set(MANUAL_CHECK_RE.findall(manual_line.group(1))) if manual_line else set()
            if manual_line and not manual_ids:
                errors.append(f"Step {step_number} Manual Checks line contains no MC-* IDs")
            unknown_manual_ids = sorted(manual_ids - set(MANUAL_CHECK_IDS))
            if unknown_manual_ids:
                errors.append(
                    f"Step {step_number} contains unknown Manual Check IDs: "
                    + ", ".join(unknown_manual_ids)
                )
            required_step_checks = {"MC-SCOPE-001", "MC-TEST-001"}
            missing_step_checks = sorted(required_step_checks - manual_ids)
            if missing_step_checks:
                errors.append(
                    f"Step {step_number} Manual Checks must include: "
                    + ", ".join(missing_step_checks)
                )
        if template_version == 4:
            literal_line = re.search(r"(?m)^- Literal Rules:\s*(.+)$", step)
            literal_numbers = (
                {int(number) for number in LITERAL_RULE_RE.findall(literal_line.group(1))}
                if literal_line
                else set()
            )
            if literal_line and not literal_numbers:
                errors.append(f"Step {step_number} Literal Rules line contains no Rule N values")
            unknown_literal_numbers = (
                {int(number) for number in ANY_LITERAL_RULE_RE.findall(literal_line.group(1))}
                - set(LITERAL_RULE_NUMBERS)
                if literal_line
                else set()
            )
            if unknown_literal_numbers:
                errors.append(
                    f"Step {step_number} contains unknown literal Rule numbers: "
                    + ", ".join(str(number) for number in sorted(unknown_literal_numbers))
                )
            if 11 not in literal_numbers:
                errors.append(f"Step {step_number} Literal Rules must include Rule 11")

        coverage = re.search(r"(?m)^- Requirements:\s*(.+)$", step)
        covered = set(SOURCE_ID_RE.findall(coverage.group(1))) if coverage else set()
        if coverage and not covered:
            errors.append(f"Step {step_number} Requirements line contains no source IDs")
        step_requirements.update(covered)

        files = extract_file_blocks(step)
        if not files:
            errors.append(f"Step {step_number} contains no valid ordered File entries")
            continue
        if [number for number, _, _, _ in files] != list(range(1, len(files) + 1)):
            errors.append(
                f"Step {step_number} File numbers must be contiguous from 1: "
                f"{[number for number, _, _, _ in files]}"
            )
        for file_number, _operation, _file_path, block in files:
            for marker in file_markers:
                if marker not in block:
                    errors.append(f"Step {step_number} File {file_number} missing marker: {marker}")
            pseudocode = re.search(
                r"(?s)- Implementation pseudocode:\s*\n\s*```[^\n]*\n(.+?)\n```",
                block,
            )
            if not pseudocode:
                errors.append(f"Step {step_number} File {file_number} needs a fenced pseudocode block")
            else:
                pseudocode_body = pseudocode.group(1)
                if GENERIC_PSEUDOCODE.search(pseudocode_body):
                    warnings.append(
                        f"Step {step_number} File {file_number} contains generic, non-implementable pseudocode"
                    )
                if template_version in {2, 3, 4}:
                    nonempty_lines = [line for line in pseudocode_body.splitlines() if line.strip()]
                    if len(re.sub(r"\s+", " ", pseudocode_body).strip()) < 120 or len(nonempty_lines) < 3:
                        errors.append(
                            f"Step {step_number} File {file_number} pseudocode is too shallow "
                            f"for Template Version {template_version}"
                        )
            if template_version in {3, 4}:
                standards = re.search(r"(?m)^- Standards impact:\s*(.+)$", block)
                standards_ids = set(MANUAL_CHECK_RE.findall(standards.group(1))) if standards else set()
                if standards and not standards_ids:
                    errors.append(
                        f"Step {step_number} File {file_number} Standards impact contains no MC-* IDs"
                    )
                unknown_standards_ids = sorted(standards_ids - set(MANUAL_CHECK_IDS))
                if unknown_standards_ids:
                    errors.append(
                        f"Step {step_number} File {file_number} contains unknown Manual Check IDs: "
                        + ", ".join(unknown_standards_ids)
                    )
            if template_version == 4:
                literal_enforcement = re.search(
                    r"(?m)^- Literal rule enforcement:\s*(.+)$", block
                )
                file_rule_numbers = (
                    set(LITERAL_RULE_RE.findall(literal_enforcement.group(1)))
                    if literal_enforcement
                    else set()
                )
                if literal_enforcement and not file_rule_numbers:
                    errors.append(
                        f"Step {step_number} File {file_number} Literal rule enforcement "
                        "contains no Rule N values"
                    )
                unknown_file_rules = (
                    set(ANY_LITERAL_RULE_RE.findall(literal_enforcement.group(1)))
                    - {str(number) for number in LITERAL_RULE_NUMBERS}
                    if literal_enforcement
                    else set()
                )
                if unknown_file_rules:
                    errors.append(
                        f"Step {step_number} File {file_number} contains unknown literal Rule numbers: "
                        + ", ".join(sorted(unknown_file_rules))
                    )

        if template_version in {2, 3, 4} and files:
            commit_paths = re.search(r"(?m)^- Commit paths:\s*(.+)$", step)
            if commit_paths:
                commit_scope = commit_paths.group(1)
                for _number, _operation, file_path, _block in files:
                    if file_path not in commit_scope:
                        errors.append(
                            f"Step {step_number} Commit paths omits declared file: {file_path}"
                        )

    if effective_requirements:
        missing = sorted(effective_requirements - step_requirements)
        extra_req = sorted({item for item in step_requirements if item.startswith("REQ-")} - effective_requirements)
        if missing:
            errors.append(f"Effective Spec requirements not covered by Steps: {', '.join(missing)}")
        if extra_req:
            errors.append(f"Step requirements absent from effective Specs: {', '.join(extra_req)}")
    elif not any(item.startswith("PLAN-REQ-") for item in step_requirements):
        warnings.append("Effective Specs have no REQ-NNN IDs and Steps define no PLAN-REQ-NNN trace aliases")

    traceability = section(text, "## 10. Requirement-to-Step Traceability Matrix")
    for requirement in step_requirements:
        if requirement not in traceability:
            errors.append(f"Step requirement absent from traceability matrix: {requirement}")

    if status in {"Review", "Ready", "In Progress", "Completed", "Superseded"}:
        for pattern in PLACEHOLDER_PATTERNS:
            match = pattern.search(text)
            if match:
                errors.append(f"Status {status} cannot contain unresolved placeholder: {match.group(0)}")
        risks = section(text, "## 11. Risks, Blockers, and User Decisions")
        if re.search(r"(?im)^\|[^\n]+\|[^\n]+\|[^\n]+\|[^\n]+\|[^\n]+\|[^\n]*\bOpen\b[^\n]*\|$", risks):
            errors.append(f"Status {status} cannot contain an open major blocker")

    present_verdicts = {verdict for verdict in VERDICTS if verdict in text}
    if len(present_verdicts) != 1:
        errors.append("Document must contain exactly one allowed final verdict")
    elif status in {"Review", "Ready"} and "BLOCKED — Spec or user decision required" in present_verdicts:
        errors.append(f"Status {status} cannot use the BLOCKED verdict")

    if template_version in {3, 4}:
        errors.extend(
            validate_manual_checks(
                text,
                pass_verdict="PASS — Ready for user review" in present_verdicts,
            )
        )

    if strict and warnings:
        errors.extend(f"STRICT: {warning}" for warning in warnings)
        warnings = []
    return errors, warnings


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate an EGON coding Plan Markdown file")
    parser.add_argument("plan", type=Path, help="Path to YYYY-MM-DD-HH-MM-abstract.md")
    parser.add_argument("--strict", action="store_true", help="Treat warnings as errors")
    args = parser.parse_args()

    errors, warnings = validate(args.plan.resolve(), args.strict)
    for warning in warnings:
        print(f"WARN: {warning}")
    for error in errors:
        print(f"ERROR: {error}")

    if errors:
        print(f"FAIL: {len(errors)} error(s)")
        return 1
    print("PASS: EGON coding Plan metadata, Spec links, file Steps, and requirement coverage are valid")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
