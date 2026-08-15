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
STEP_MARKERS = [
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
FILE_MARKERS = [
    "- Purpose:",
    "- Symbols:",
    "- Why now:",
    "- Contract/signature changes:",
    "- Implementation pseudocode:",
    "- After this file:",
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
        for marker in STEP_MARKERS:
            if marker not in step:
                errors.append(f"Step {step_number} missing required marker: {marker}")

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
            for marker in FILE_MARKERS:
                if marker not in block:
                    errors.append(f"Step {step_number} File {file_number} missing marker: {marker}")
            pseudocode = re.search(
                r"(?s)- Implementation pseudocode:\s*\n\s*```[^\n]*\n(.+?)\n```",
                block,
            )
            if not pseudocode:
                errors.append(f"Step {step_number} File {file_number} needs a fenced pseudocode block")
            elif GENERIC_PSEUDOCODE.search(pseudocode.group(1)):
                warnings.append(
                    f"Step {step_number} File {file_number} contains generic, non-implementable pseudocode"
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
