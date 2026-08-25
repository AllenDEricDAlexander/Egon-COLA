#!/usr/bin/env python3
"""Regression tests for Execute Plan skill resource integrity."""

from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from validate_skill_resources import validate_skill_resources


class ResourceIntegrityTest(unittest.TestCase):
    def create_root(self) -> Path:
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        root = Path(temporary.name)
        (root / "references").mkdir()
        (root / "scripts").mkdir()
        (root / "SKILL.md").write_text("# skill\n", encoding="utf-8")
        (root / "SKILL.zh-CN.md").write_text("# skill zh\n", encoding="utf-8")
        return root

    def test_current_skill_is_valid(self) -> None:
        root = Path(__file__).resolve().parent.parent
        errors, _, _ = validate_skill_resources(root)
        self.assertEqual([], errors)

    def test_missing_referenced_resource_is_rejected(self) -> None:
        root = self.create_root()
        (root / "SKILL.md").write_text(
            "Read `references/missing.md` before execution.\n", encoding="utf-8"
        )
        errors, _, _ = validate_skill_resources(root)
        self.assertTrue(any("missing resource" in error for error in errors))

    def test_ambiguous_bare_resource_is_rejected(self) -> None:
        root = self.create_root()
        (root / "references" / "checklist.md").write_text("# checklist\n", encoding="utf-8")
        (root / "SKILL.md").write_text("Read `checklist.md`.\n", encoding="utf-8")
        errors, _, _ = validate_skill_resources(root)
        self.assertTrue(any("ambiguous" in error for error in errors))

    def test_broken_local_markdown_link_is_rejected(self) -> None:
        root = self.create_root()
        (root / "SKILL.md").write_text("Use [missing](references/missing.md).\n", encoding="utf-8")
        errors, _, _ = validate_skill_resources(root)
        self.assertTrue(any("broken Markdown link" in error for error in errors))

    def test_repository_agents_file_is_allowed(self) -> None:
        root = self.create_root()
        (root / "SKILL.md").write_text("Read `AGENTS.md` before execution.\n", encoding="utf-8")
        errors, _, _ = validate_skill_resources(root)
        self.assertEqual([], errors)


if __name__ == "__main__":
    unittest.main()
