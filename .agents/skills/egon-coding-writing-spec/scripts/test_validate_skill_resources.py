#!/usr/bin/env python3
"""Regression tests for bundled EGON skill resource validation."""

from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from validate_skill_resources import validate_skill_resources


class SkillResourceValidationTest(unittest.TestCase):
    def create_skill(self, root: Path, english: str, chinese: str = "# 审核镜像\n") -> None:
        (root / "SKILL.md").write_text(english, encoding="utf-8")
        (root / "SKILL.zh-CN.md").write_text(chinese, encoding="utf-8")

    def validate(self, root: Path) -> list[str]:
        errors, _, _ = validate_skill_resources(root)
        return errors

    def test_missing_canonical_resource_fails_with_location(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self.create_skill(root, "Read `references/missing.md` before drafting.\n")

            errors = self.validate(root)

            self.assertTrue(
                any(
                    "SKILL.md:1: missing resource: references/missing.md" in error
                    for error in errors
                )
            )

    def test_existing_bare_bundled_resource_is_ambiguous(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            references = root / "references"
            references.mkdir()
            (references / "rules.md").write_text("# Rules\n", encoding="utf-8")
            self.create_skill(root, "Read `rules.md` before drafting.\n")

            errors = self.validate(root)

            self.assertTrue(any("ambiguous load path 'rules.md'" in error for error in errors))
            self.assertEqual(1, sum("rules.md" in error for error in errors))

    def test_repository_agents_instruction_is_allowed(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self.create_skill(root, "Read all applicable `AGENTS.md` files.\n")

            self.assertEqual([], self.validate(root))

    def test_resource_escape_fails(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self.create_skill(root, "Read `references/../../outside.md` before drafting.\n")

            errors = self.validate(root)

            self.assertTrue(any("resource escapes skill root" in error for error in errors))

    def test_broken_local_markdown_link_fails(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            references = root / "references"
            references.mkdir()
            (references / "rules.md").write_text("See [missing](missing.md).\n", encoding="utf-8")
            self.create_skill(root, "Read `references/rules.md` before drafting.\n")

            errors = self.validate(root)

            self.assertTrue(any("broken Markdown link: missing.md" in error for error in errors))


if __name__ == "__main__":
    unittest.main()
