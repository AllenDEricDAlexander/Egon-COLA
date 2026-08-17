#!/usr/bin/env python3
"""Validate that an EGON skill's referenced bundled resources are resolvable."""

from __future__ import annotations

import argparse
import re
from pathlib import Path

ROOT_RESOURCE_RE = re.compile(
    r"(?<![A-Za-z0-9_.-])(?P<path>(?:references|assets|scripts)/"
    r"[A-Za-z0-9_.\-/]+\.(?:md|py|json|ya?ml))(?![A-Za-z0-9_.-])"
)
MARKDOWN_LINK_RE = re.compile(r"\[[^\]]+\]\((?P<target>[^)]+)\)")
LOAD_INSTRUCTION_RE = re.compile(
    r"(?:Read|Apply|Run|读取|执行|运行)[^\n`]*`(?P<path>[^`]+\.(?:md|py))`",
    re.IGNORECASE,
)
BARE_FILE_RE = re.compile(r"`(?P<path>[A-Za-z0-9_.-]+\.(?:md|py|json|ya?ml))`")
CORE_FILES = ("SKILL.md", "SKILL.zh-CN.md")
ALLOWED_REPOSITORY_FILES = {"AGENTS.md"}


def inside_root(root: Path, target: Path) -> bool:
    try:
        target.relative_to(root)
        return True
    except ValueError:
        return False


def validate_skill_resources(root: Path) -> tuple[list[str], set[Path], list[Path]]:
    root = root.resolve()
    errors: list[str] = []
    referenced: set[Path] = set()

    for relative in CORE_FILES:
        target = root / relative
        referenced.add(target)
        if not target.is_file():
            errors.append(f"Missing core skill file: {relative}")

    markdown_files = sorted(root.rglob("*.md"))
    if not markdown_files:
        errors.append(f"No Markdown skill resources found under: {root}")
        return errors, referenced, markdown_files

    for document in markdown_files:
        text = document.read_text(encoding="utf-8")
        for line_number, line in enumerate(text.splitlines(), start=1):
            ambiguous_load_spans: set[tuple[int, int]] = set()
            for match in ROOT_RESOURCE_RE.finditer(line):
                relative = Path(match.group("path"))
                target = (root / relative).resolve()
                referenced.add(target)
                if not inside_root(root, target):
                    errors.append(
                        f"{document.relative_to(root)}:{line_number}: "
                        f"resource escapes skill root: {relative}"
                    )
                elif not target.is_file():
                    errors.append(
                        f"{document.relative_to(root)}:{line_number}: "
                        f"missing resource: {relative}"
                    )

            for match in LOAD_INSTRUCTION_RE.finditer(line):
                raw = match.group("path")
                if (
                    raw in CORE_FILES
                    or raw in ALLOWED_REPOSITORY_FILES
                    or raw.startswith(("references/", "assets/", "scripts/"))
                ):
                    continue
                ambiguous_load_spans.add(match.span("path"))
                errors.append(
                    f"{document.relative_to(root)}:{line_number}: ambiguous load path '{raw}'; "
                    "use a skill-root-relative references/, assets/, or scripts/ path"
                )

            for match in BARE_FILE_RE.finditer(line):
                raw = match.group("path")
                if (
                    match.span("path") in ambiguous_load_spans
                    or raw in CORE_FILES
                    or raw in ALLOWED_REPOSITORY_FILES
                ):
                    continue
                bundled_candidates = (
                    root / "references" / raw,
                    root / "assets" / raw,
                    root / "scripts" / raw,
                )
                if any(candidate.is_file() for candidate in bundled_candidates):
                    errors.append(
                        f"{document.relative_to(root)}:{line_number}: ambiguous bundled resource '{raw}'; "
                        "use its skill-root-relative references/, assets/, or scripts/ path"
                    )

            for match in MARKDOWN_LINK_RE.finditer(line):
                raw_target = match.group("target").strip().split("#", 1)[0]
                if (
                    not raw_target
                    or raw_target.startswith("#")
                    or re.match(r"^[a-z][a-z0-9+.-]*://", raw_target, re.IGNORECASE)
                    or "<" in raw_target
                    or "YYYY-MM-DD" in raw_target
                ):
                    continue
                target = (document.parent / raw_target).resolve()
                if not target.exists():
                    errors.append(
                        f"{document.relative_to(root)}:{line_number}: broken Markdown link: {raw_target}"
                    )

    return errors, referenced, markdown_files


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate bundled EGON skill resource paths")
    parser.add_argument(
        "--root",
        type=Path,
        default=Path(__file__).resolve().parent.parent,
        help="Skill root containing SKILL.md (defaults to this script's skill)",
    )
    args = parser.parse_args()
    errors, referenced, markdown_files = validate_skill_resources(args.root)
    for error in errors:
        print(f"ERROR: {error}")
    if errors:
        print(f"FAIL: {len(errors)} resource-integrity error(s)")
        return 1
    print(
        "PASS: skill resource integrity is valid "
        f"({len(markdown_files)} Markdown files, {len(referenced)} referenced/core resources)"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
