#!/usr/bin/env python3
"""Validate an EGON coding Spec's structure, metadata, links, and coverage."""

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
VALID_STATUSES = {"Draft", "Review", "Accepted", "Implemented", "Superseded", "Rejected"}
VALID_COMPLEXITIES = {"Simple", "Complex"}
CURRENT_TEMPLATE_VERSION = 2
REQUIRED_FIELDS = [
    "Document",
    "Status",
    "Type",
    "Created",
    "Updated",
    "Owner",
    "Repository",
    "Scope",
    "Source Requirement",
    "Baseline Revision",
    "Amends",
    "Supersedes",
    "Depends On",
    "Related Specs",
    "Related Plans",
]
RELATION_FIELDS = ["Amends", "Supersedes", "Depends On", "Related Specs", "Related Plans"]
REQUIRED_HEADINGS = [
    "## 1. Summary",
    "## 2. Background and Current State",
    "## 3. Goals and Non-goals",
    "## 4. Requirements and Acceptance Criteria",
    "## 5. Constraints, Assumptions, and Decisions",
    "## 6. Project Technology Context",
    "## 7. Architecture Design",
    "## 8. Package Structure and Code File Tree",
    "## 9. Interface Definitions",
    "## 11. Database Design",
    "## 12. Frontend Page Design",
    "## 13. Design Patterns and Architecture Principles",
    "## 14. Test Design",
    "## 15. Non-functional and Cross-cutting Design",
    "## 16. Compatibility, Migration, Rollout, and Rollback",
    "## 17. Alternatives and Decisions",
    "## 18. Risks and Open Questions",
    "## 19. Traceability Matrix",
    "## 20. Review and Acceptance",
]
REQUIRED_HEADING_ALTERNATIVES = [
    (
        "## 10. POJO and Data Model Design",
        "## 10. Entity and Domain Model Design",
    ),
]
PLACEHOLDER_PATTERNS = [
    re.compile(r"\b(?:TBD|TODO|FIXME|XXX)\b", re.IGNORECASE),
    re.compile(
        r"<\s*(?:specification title|decision owner|repository|modules?|bounded context|"
        r"user request|issue|ticket|brief|commit|path|symbol|fill|owner|command)[^>]*>",
        re.IGNORECASE,
    ),
    re.compile(r"YYYY-MM-DD(?:-HH-MM)?"),
    re.compile(r"\bABSTRACT\b"),
]
VERDICTS = {
    "PASS — Ready for user review",
    "BLOCKED — User decision required",
    "REVISE — Internal inconsistency found",
}
CONTRACT_ID_RE = re.compile(r"\b(?:API|RPC|EVENT|MESSAGE|JOB|CLI|INTERNAL)-\d{3}\b")
CONTRACT_DETAIL_HEADING_RE = re.compile(
    r"(?m)^####\s+(?:9\.2\.\d+\s+)?(?P<id>(?:API|RPC|EVENT|MESSAGE|JOB|CLI|INTERNAL)-\d{3})\b"
)
JSONC_BLOCK_RE = re.compile(r"```jsonc\s*\n(?P<body>.*?)```", re.IGNORECASE | re.DOTALL)
MERMAID_BLOCK_RE = re.compile(r"```mermaid\s*\n(?P<body>.*?)```", re.IGNORECASE | re.DOTALL)
HTTP_METHOD_RE = re.compile(r"\b(?:GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)\b")
HTTP_ROUTE_RE = re.compile(r"(?<![A-Za-z0-9_])/(?:[A-Za-z0-9_{}.:?=&*\-]+/?)*")
TABLE_NAME_RE = re.compile(r"^[A-Za-z_][A-Za-z0-9_$]*(?:\.[A-Za-z_][A-Za-z0-9_$]*)?$")
TABLE_DETAIL_HEADING_RE = re.compile(
    r"(?m)^####\s+(?:11\.2\.\d+\s+)?`?(?P<name>[A-Za-z_][A-Za-z0-9_$]*(?:\.[A-Za-z_][A-Za-z0-9_$]*)?)`?\b"
)


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


def validate_commented_jsonc(contract_id: str, block: str) -> list[str]:
    errors: list[str] = []
    for line_number, line in enumerate(block.splitlines(), start=1):
        if not re.match(r'^\s*"[^"]+"\s*:', line):
            continue
        if not re.search(r'(?:,|\{|\[|\}|\]|"|\d|true|false|null)\s*//\s*\S', line, re.IGNORECASE):
            errors.append(
                f"{contract_id} jsonc field lacks a line-end meaning comment "
                f"(block line {line_number}): {line.strip()}"
            )
    return errors


def markdown_inventory_names(text: str, header: str) -> set[str]:
    names: set[str] = set()
    for line in text.splitlines():
        if not line.lstrip().startswith("|"):
            continue
        cells = [cell.strip() for cell in line.strip().strip("|").split("|")]
        if not cells:
            continue
        name = clean(cells[0])
        if name == header or not TABLE_NAME_RE.fullmatch(name):
            continue
        names.add(name)
    return names


def validate_v2_content(text: str, fields: dict[str, str], status: str) -> list[str]:
    errors: list[str] = []
    complexity = clean(fields.get("Complexity", ""))
    drivers = clean(fields.get("Complexity Drivers", ""))

    if complexity not in VALID_COMPLEXITIES:
        errors.append("Template Version 2 requires Complexity to be Simple or Complex")
    if not drivers:
        errors.append("Template Version 2 requires a non-empty Complexity Drivers field")
    elif complexity == "Complex" and drivers.lower() == "none":
        errors.append("A Complex Spec must name material Complexity Drivers")

    required_v2_headings = [
        "### 7.1 System Architecture Design",
        "### 7.2 High-Level Design",
        "### 7.3 Detailed Design",
        "### 9.1 Interface Inventory",
        "### 9.2 Per-interface Detailed Contracts",
        "### 11.1 Table Inventory",
        "### 11.2 Per-table Detailed Design",
    ]
    for heading in required_v2_headings:
        if heading not in text:
            errors.append(f"Template Version 2 is missing required subsection: {heading}")

    architecture = section(text, "## 7. Architecture Design")
    if complexity == "Complex":
        mermaid_blocks = [match.group("body").lstrip() for match in MERMAID_BLOCK_RE.finditer(architecture)]
        flowchart_count = sum(block.startswith("flowchart") for block in mermaid_blocks)
        sequence_count = sum(block.startswith("sequenceDiagram") for block in mermaid_blocks)
        if flowchart_count < 2:
            errors.append("A Complex Spec requires separate Mermaid architecture and critical-flow flowcharts")
        if sequence_count < 1:
            errors.append("A Complex Spec requires a Mermaid sequenceDiagram swimlane view")

    interfaces = section(text, "## 9. Interface Definitions")
    detail_start = interfaces.find("### 9.2 Per-interface Detailed Contracts")
    inventory_text = interfaces[:detail_start] if detail_start >= 0 else interfaces
    detail_text = interfaces[detail_start:] if detail_start >= 0 else ""
    inventory_ids = set(CONTRACT_ID_RE.findall(inventory_text))
    detail_matches = list(CONTRACT_DETAIL_HEADING_RE.finditer(detail_text))
    detail_ids = [match.group("id") for match in detail_matches]

    for contract_id in sorted(inventory_ids):
        count = detail_ids.count(contract_id)
        if count != 1:
            errors.append(
                f"Interface inventory ID {contract_id} must have exactly one detailed heading; found {count}"
            )
    for contract_id in sorted(set(detail_ids) - inventory_ids):
        errors.append(f"Detailed contract {contract_id} is absent from the interface inventory")

    for line in inventory_text.splitlines():
        api_ids = re.findall(r"\bAPI-\d{3}\b", line)
        if not api_ids:
            continue
        methods = HTTP_METHOD_RE.findall(line)
        routes = HTTP_ROUTE_RE.findall(line)
        if len(api_ids) != 1 or len(methods) != 1 or len(routes) != 1:
            errors.append(
                "Each HTTP interface inventory row must contain exactly one API ID, one Method, "
                f"and one URL; found ids={api_ids}, methods={methods}, urls={routes}"
            )

    for index, match in enumerate(detail_matches):
        contract_id = match.group("id")
        end = detail_matches[index + 1].start() if index + 1 < len(detail_matches) else len(detail_text)
        contract_text = detail_text[match.start():end]
        jsonc_blocks = [item.group("body") for item in JSONC_BLOCK_RE.finditer(contract_text)]
        if contract_id.startswith("API-"):
            if not re.search(r"\b(?:GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)\s+/\S+", contract_text):
                errors.append(f"{contract_id} detailed contract lacks an HTTP method and URL")
            if not jsonc_blocks and "No Content" not in contract_text:
                errors.append(f"{contract_id} detailed contract lacks a jsonc response or explicit No Content outcome")
        for block in jsonc_blocks:
            errors.extend(validate_commented_jsonc(contract_id, block))
            if status in {"Review", "Accepted", "Implemented", "Superseded"} and "..." in block:
                errors.append(f"{contract_id} jsonc payload cannot contain an abbreviated ellipsis at Status {status}")

    database = section(text, "## 11. Database Design")
    table_detail_start = database.find("### 11.2 Per-table Detailed Design")
    table_inventory = database[:table_detail_start] if table_detail_start >= 0 else database
    table_detail = database[table_detail_start:] if table_detail_start >= 0 else ""
    inventory_tables = markdown_inventory_names(table_inventory, "Table")
    detail_tables = [match.group("name") for match in TABLE_DETAIL_HEADING_RE.finditer(table_detail)]
    for table_name in sorted(inventory_tables):
        count = detail_tables.count(table_name)
        if count != 1:
            errors.append(
                f"Database inventory table {table_name} must have exactly one detailed heading; found {count}"
            )
    for table_name in sorted(set(detail_tables) - inventory_tables):
        errors.append(f"Detailed database table {table_name} is absent from the table inventory")

    return errors


def validate_relation_links(path: Path, field: str, value: str) -> list[str]:
    errors: list[str] = []
    normalized = clean(value)
    if normalized.lower() == "none":
        return errors

    links = re.findall(r"\[[^\]]+\]\(([^)]+)\)", value)
    if not links:
        return [f"{field} must be None or contain at least one relative Markdown link"]

    for link in links:
        target = link.strip().split("#", 1)[0]
        if not target:
            errors.append(f"{field} contains an anchor-only link; link the predecessor document explicitly")
            continue
        if re.match(r"^[a-z][a-z0-9+.-]*://", target, re.IGNORECASE) or Path(target).is_absolute():
            errors.append(f"{field} link must be repository-relative: {link}")
            continue
        resolved = (path.parent / target).resolve()
        if not resolved.is_file():
            errors.append(f"{field} link does not exist: {resolved}")

    if field in {"Amends", "Supersedes", "Depends On"}:
        has_section_reference = "§" in value or any("#" in link for link in links)
        if not has_section_reference:
            errors.append(f"{field} must identify exact predecessor sections or anchors")
    return errors


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

    document = fields.get("Document", "")
    if document and path.name not in document:
        errors.append(f"Document header must name the current file: {path.name}")

    status = clean(fields.get("Status", ""))
    if status and status not in VALID_STATUSES:
        errors.append(f"Invalid Status '{status}'. Expected one of: {', '.join(sorted(VALID_STATUSES))}")

    template_version = clean(fields.get("Template Version", ""))
    if template_version:
        if not template_version.isdigit():
            errors.append(f"Template Version must be an integer: {template_version}")
        elif int(template_version) != CURRENT_TEMPLATE_VERSION:
            errors.append(
                f"Unsupported Template Version {template_version}; expected {CURRENT_TEMPLATE_VERSION}"
            )
        else:
            for field in ("Complexity", "Complexity Drivers"):
                if field not in fields or not clean(fields[field]):
                    errors.append(f"Template Version 2 requires header field: {field}")
            errors.extend(validate_v2_content(text, fields, status))

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

    for alternatives in REQUIRED_HEADING_ALTERNATIVES:
        if not any(heading in text for heading in alternatives):
            errors.append(f"Missing required section: one of {', '.join(alternatives)}")

    requirements = sorted(set(re.findall(r"\bREQ-\d{3}\b", text)))
    if not requirements:
        errors.append("No REQ-NNN requirement IDs found")

    test_ids = sorted(set(re.findall(r"\bTEST-\d{3}\b", text)))
    if not test_ids:
        warnings.append("No TEST-NNN IDs found; test design may be untraceable")

    for field in RELATION_FIELDS:
        value = fields.get(field, "")
        if value:
            errors.extend(validate_relation_links(path, field, value))

    if status in {"Review", "Accepted", "Implemented", "Superseded"}:
        for pattern in PLACEHOLDER_PATTERNS:
            match = pattern.search(text)
            if match:
                errors.append(f"Status {status} cannot contain unresolved placeholder: {match.group(0)}")

        decisions = section(text, "## 5. Constraints, Assumptions, and Decisions")
        if re.search(r"(?im)^\|[^\n]+\|[^\n]+\|[^\n]+\|[^\n]+\|[^\n]+\|\s*Open\s*\|$", decisions):
            errors.append(f"Status {status} cannot contain an open major decision")

    present_verdicts = {verdict for verdict in VERDICTS if verdict in text}
    if len(present_verdicts) != 1:
        errors.append("Document must contain exactly one allowed final verdict")
    elif status == "Draft" and "PASS — Ready for user review" in present_verdicts:
        warnings.append("Draft status uses a PASS verdict; verify that no major decision remains")
    elif status in {"Review", "Accepted"} and "BLOCKED — User decision required" in present_verdicts:
        errors.append(f"Status {status} cannot use the BLOCKED verdict")

    traceability = section(text, "## 19. Traceability Matrix")
    for requirement in requirements:
        if requirement not in traceability:
            errors.append(f"Requirement absent from traceability matrix: {requirement}")

    if strict and warnings:
        errors.extend(f"STRICT: {warning}" for warning in warnings)
        warnings = []
    return errors, warnings


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate an EGON coding Spec Markdown file")
    parser.add_argument("spec", type=Path, help="Path to YYYY-MM-DD-HH-MM-abstract.md")
    parser.add_argument("--strict", action="store_true", help="Treat warnings as errors")
    args = parser.parse_args()

    errors, warnings = validate(args.spec.resolve(), args.strict)
    for warning in warnings:
        print(f"WARN: {warning}")
    for error in errors:
        print(f"ERROR: {error}")

    if errors:
        print(f"FAIL: {len(errors)} error(s)")
        return 1
    print("PASS: EGON coding Spec structure, metadata, links, and traceability are valid")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
