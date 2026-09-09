#!/usr/bin/env python3
"""Inventory external Spring Controller methods and their Admin Web evidence."""

from __future__ import annotations

import argparse
import json
import re
from dataclasses import asdict, dataclass, replace
from pathlib import Path
from typing import Mapping, Sequence

UI_ACTION = "UI_ACTION"
API_CONSUMED = "API_CONSUMED"
PROTOCOL_OR_INTERNAL = "PROTOCOL_OR_INTERNAL"
UNCONSUMED = "UNCONSUMED"

HTTP_MAPPING_NAMES = {
    "Get": "GET",
    "Post": "POST",
    "Put": "PUT",
    "Patch": "PATCH",
    "Delete": "DELETE",
}


@dataclass(frozen=True)
class AuditRow:
    platform: str
    controller: str
    method: str
    http_method: str
    path: str
    source_file: str = ""
    status: str = ""
    evidence: str = ""


@dataclass(frozen=True)
class AuditResult:
    rows: tuple[AuditRow, ...]

    @property
    def exit_code(self) -> int:
        return 1 if any(row.status == UNCONSUMED for row in self.rows) else 0


@dataclass(frozen=True)
class WebFile:
    path: str
    source: str
    expanded: str


def _matching_parenthesis(source: str, opening: int) -> int:
    depth = 0
    quote: str | None = None
    escaped = False
    for index in range(opening, len(source)):
        character = source[index]
        if quote is not None:
            if escaped:
                escaped = False
            elif character == "\\":
                escaped = True
            elif character == quote:
                quote = None
            continue
        if character in {"'", '"'}:
            quote = character
        elif character == "(":
            depth += 1
        elif character == ")":
            depth -= 1
            if depth == 0:
                return index
    return len(source) - 1


def _annotation_arguments(source: str, annotation_start: int, annotation_name: str) -> tuple[str, int]:
    cursor = annotation_start + len(annotation_name)
    while cursor < len(source) and source[cursor].isspace():
        cursor += 1
    if cursor >= len(source) or source[cursor] != "(":
        return "", cursor
    closing = _matching_parenthesis(source, cursor)
    return source[cursor + 1:closing], closing + 1


def _split_top_level(value: str) -> list[str]:
    result: list[str] = []
    start = 0
    depth = 0
    quote: str | None = None
    escaped = False
    for index, character in enumerate(value):
        if quote is not None:
            if escaped:
                escaped = False
            elif character == "\\":
                escaped = True
            elif character == quote:
                quote = None
            continue
        if character in {"'", '"'}:
            quote = character
        elif character in "({[":
            depth += 1
        elif character in ")}]":
            depth -= 1
        elif character == "," and depth == 0:
            result.append(value[start:index].strip())
            start = index + 1
    result.append(value[start:].strip())
    return [item for item in result if item]


def _string_literal_value(value: str) -> str | None:
    value = value.strip()
    if len(value) < 2 or value[0] != '"' or value[-1] != '"':
        return None
    try:
        return json.loads(value)
    except json.JSONDecodeError:
        return value[1:-1]


def _java_string_constants(source: str) -> dict[str, str]:
    constants: dict[str, str] = {}
    pattern = re.compile(
        r"\b(?:public|protected|private)?\s*static\s+final\s+String\s+(\w+)\s*=\s*(.*?);",
        re.DOTALL,
    )
    for match in pattern.finditer(source):
        expression = match.group(2)
        parts = []
        for token in re.finditer(r'"(?:\\.|[^"\\])*"|\b[A-Za-z_]\w*\b', expression):
            literal = _string_literal_value(token.group(0))
            if literal is not None:
                parts.append(literal)
            elif token.group(0) in constants:
                parts.append(constants[token.group(0)])
        if parts:
            constants[match.group(1)] = "".join(parts)
    return constants


def _resolve_java_expression(expression: str, constants: Mapping[str, str]) -> str:
    parts = []
    for token in re.finditer(r'"(?:\\.|[^"\\])*"|\b[A-Za-z_]\w*\b', expression):
        literal = _string_literal_value(token.group(0))
        if literal is not None:
            parts.append(literal)
        elif token.group(0) in constants:
            parts.append(constants[token.group(0)])
    return "".join(parts)


def _mapping_values(arguments: str, constants: Mapping[str, str]) -> list[str]:
    if not arguments.strip():
        return [""]
    named = re.search(r"\b(?:path|value)\s*=\s*", arguments)
    expression = arguments[named.end():] if named else arguments
    expression = _split_top_level(expression)[0]
    if expression.startswith("{") and expression.endswith("}"):
        expressions = _split_top_level(expression[1:-1])
    else:
        expressions = [expression]
    values = [_resolve_java_expression(item, constants) for item in expressions]
    return values or [""]


def _class_mapping(source: str, class_start: int, constants: Mapping[str, str]) -> str:
    prefix = source[:class_start]
    matches = list(re.finditer(r"@RequestMapping\b", prefix))
    if not matches:
        return ""
    arguments, _ = _annotation_arguments(prefix, matches[-1].start(), "@RequestMapping")
    return _mapping_values(arguments, constants)[0]


def _join_path(base: str, suffix: str) -> str:
    value = "/".join(part.strip("/") for part in (base, suffix) if part.strip("/"))
    return f"/{value}" if value else "/"


def _method_name_after(source: str, annotation_end: int) -> str | None:
    # The first mapped declaration is the public controller method.  The
    # annotation block may contain Operation/permission annotations and Javadoc.
    tail = source[annotation_end:]
    match = re.search(
        r"\b(?:public|protected|private)\s+(?:(?:static|final|synchronized|abstract)\s+)*"
        r"[\w.$<>?,\[\]\s]+?\s+(\w+)\s*\(",
        tail,
    )
    return match.group(1) if match else None


def inventory_java(root: Path, platform: str = "") -> list[AuditRow]:
    rows: list[AuditRow] = []
    for path in sorted(root.rglob("*Controller.java")):
        source = path.read_text(encoding="utf-8")
        if "EgonGatewayPolicy.Exposure.EXTERNAL" not in source:
            continue
        class_match = re.search(r"\bclass\s+(\w+)\b", source)
        if not class_match:
            continue
        controller = class_match.group(1)
        constants = _java_string_constants(source)
        base = _class_mapping(source, class_match.start(), constants)
        for mapping in re.finditer(r"@(Get|Post|Put|Patch|Delete)Mapping\b", source):
            annotation = f"@{mapping.group(1)}Mapping"
            arguments, annotation_end = _annotation_arguments(source, mapping.start(), annotation)
            method = _method_name_after(source, annotation_end)
            if method is None:
                continue
            for suffix in _mapping_values(arguments, constants):
                rows.append(AuditRow(
                    platform=platform,
                    controller=controller,
                    method=method,
                    http_method=HTTP_MAPPING_NAMES[mapping.group(1)],
                    path=_join_path(base, suffix),
                    source_file=str(path),
                ))
    return rows


def _javascript_string_constants(source: str) -> dict[str, str]:
    return {
        name: value
        for name, _quote, value in re.findall(r"\b(?:const|let)\s+(\w+)\s*=\s*(['\"])(.*?)\2", source)
    }


def _expanded_web_source(source: str) -> str:
    constants = _javascript_string_constants(source)
    fragments = [source]
    for match in re.finditer(r"`([^`]*)`", source, re.DOTALL):
        template = match.group(1)
        template = re.sub(
            r"\$\{\s*(\w+)\s*\}",
            lambda item: constants.get(item.group(1), ""),
            template,
        )
        template = re.sub(r"\$\{[^}]*\}", "", template)
        fragments.append(template)
    fragments.extend(match.group(0) for match in re.finditer(r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'', source))
    return "\n".join(fragments)


def inventory_web(root: Path) -> dict[str, WebFile]:
    files: dict[str, WebFile] = {}
    for path in sorted(root.rglob("*")):
        if not path.is_file() or path.suffix not in {".ts", ".tsx", ".json", ".js", ".jsx"}:
            continue
        if ".test." in path.name or ".spec." in path.name:
            continue
        relative = str(path.relative_to(root))
        source = path.read_text(encoding="utf-8")
        files[relative] = WebFile(relative, source, _expanded_web_source(source))
    return files


def _web_files(value: Mapping[str, object] | Mapping[str, WebFile]) -> list[WebFile]:
    result: list[WebFile] = []
    for name, item in value.items():
        if isinstance(item, WebFile):
            result.append(item)
        else:
            source = str(item)
            result.append(WebFile(str(name), source, _expanded_web_source(source)))
    return result


def _normalized_path(path: str) -> str:
    value = path.split("?", 1)[0]
    value = re.sub(r"\{[^}]*\}", "", value)
    value = re.sub(r"/+", "/", value)
    return value.rstrip("/") or "/"


def _path_candidates(path: str) -> list[str]:
    candidates = [path.split("?", 1)[0], _normalized_path(path)]
    if "{" in path:
        prefix = path.split("{", 1)[0].rstrip("/")
        if prefix:
            candidates.append(prefix)
    return list(dict.fromkeys(candidate for candidate in candidates if candidate))


def _protocol_reason(row: AuditRow) -> str | None:
    path = row.path.lower()
    controller = row.controller.lower()
    method = row.method.lower()
    protocol_prefixes = (
        "/oauth",
        "/openid",
        "/.well-known",
        "/userinfo",
        "/token",
        "/jwks",
        "/rpc",
        "/internal",
        "/actuator",
    )
    protocol_fragments = (
        "/callback",
        "/metadata",
        "/protocol",
        "/authorize",
        "/consent",
        "/introspect",
    )
    if path.startswith(protocol_prefixes):
        return f"protocol endpoint by path {row.path}"
    if path.startswith("/api/v1/auth/"):
        return f"authorization bootstrap endpoint by path {row.path}"
    if path.startswith("/api/rbac3/v1/internal/"):
        return f"internal integration endpoint by path {row.path}"
    if path.startswith("/api/rbac3/v1/registration/"):
        return f"CI resource registration endpoint by path {row.path}"
    if path.startswith("/api/v1/gateway/openapi/interface-definitions/reports"):
        return f"Gateway engine definition report callback by path {row.path}"
    if path == "/api/v1/gateway/admin/providers/services":
        return "Gateway runtime projection consumed by engine/provider integration"
    if path == "/api/rbac3/v1/directory-snapshots/{snapshotid}".lower():
        return "directory snapshot receipt endpoint; snapshot ingestion is not an Admin CRUD surface"
    if path in {"/api/rbac3/v1/org-units", "/api/rbac3/v1/positions"}:
        return "legacy directory projection; Admin UI uses the canonical IAM organization resource"
    if any(fragment in path for fragment in protocol_fragments):
        return f"protocol endpoint by path {row.path}"
    if any(fragment in controller for fragment in ("rpc", "callback", "protocol")):
        return f"protocol/internal controller {row.controller}"
    if method in {"token", "authorize", "consent", "callback", "introspect", "jwks", "metadata"}:
        return f"protocol/internal method {row.method}"
    return None


def _find_web_evidence(row: AuditRow, files: Sequence[WebFile]) -> tuple[str, str] | None:
    candidates = _path_candidates(row.path)
    for web_file in files:
        for candidate in candidates:
            if candidate in web_file.expanded:
                status = UI_ACTION if web_file.path.endswith((".tsx", ".jsx")) else API_CONSUMED
                return status, f"{web_file.path}: endpoint path evidence"
    if row.method not in {"list", "get", "create", "update", "delete", "save", "enable", "disable"}:
        method_pattern = re.compile(rf"\b(?:gatewayApi|httpClient|api)\s*\.\s*{re.escape(row.method)}\s*\(")
        for web_file in files:
            if web_file.path.endswith((".tsx", ".jsx")) and method_pattern.search(web_file.source):
                return UI_ACTION, f"{web_file.path}: API method {row.method}"
    return None


def classify(rows: Sequence[AuditRow], web_inventory: Mapping[str, object]) -> AuditResult:
    files = _web_files(web_inventory)
    classified: list[AuditRow] = []
    for row in rows:
        protocol_reason = _protocol_reason(row)
        if protocol_reason is not None:
            classified.append(replace(row, status=PROTOCOL_OR_INTERNAL, evidence=protocol_reason))
            continue
        evidence = _find_web_evidence(row, files)
        if evidence is None:
            classified.append(replace(row, status=UNCONSUMED, evidence="no Admin Web UI/API evidence"))
        else:
            status, reason = evidence
            classified.append(replace(row, status=status, evidence=reason))
    return AuditResult(tuple(classified))


def _platform_roots(repo_root: Path) -> list[tuple[str, Path, Path]]:
    platforms_root = repo_root / "egon-cola-xingyuan"
    return [
        ("IDP", platforms_root / "egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin/src/main/java", platforms_root / "egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-admin-web"),
        ("RBAC3", platforms_root / "egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin/src/main/java", platforms_root / "egon-cola-tianquan-jianshen/egon-cola-tianquan-jianshen-admin-web"),
        ("Gateway", platforms_root / "egon-cola-yuheng/yuheng-admin/src/main/java", platforms_root / "egon-cola-yuheng/yuheng-admin-web"),
        ("DDC", platforms_root / "egon-cola-tianshu/egon-cola-tianshu-admin/src/main/java", platforms_root / "egon-cola-tianshu/egon-cola-tianshu-admin-web"),
    ]


def audit_repository(repo_root: Path) -> AuditResult:
    all_rows: list[AuditRow] = []
    for platform, controller_root, web_root in _platform_roots(repo_root):
        if not controller_root.is_dir() or not web_root.joinpath("src").is_dir():
            raise FileNotFoundError(f"missing audit root for {platform}")
        rows = inventory_java(controller_root, platform)
        web = inventory_web(web_root / "src")
        all_rows.extend(classify(rows, web).rows)
    return AuditResult(tuple(all_rows))


def render_report(result: AuditResult, output_format: str = "text") -> str:
    if output_format == "json":
        return json.dumps([asdict(row) for row in result.rows], ensure_ascii=False, indent=2)
    lines = ["platform\tcontroller\tmethod\thttp\tpath\tstatus\tevidence"]
    lines.extend(
        "\t".join((row.platform, row.controller, row.method, row.http_method, row.path, row.status, row.evidence))
        for row in result.rows
    )
    return "\n".join(lines)


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo-root", type=Path, default=Path.cwd())
    parser.add_argument("--format", choices=("text", "json"), default="text")
    args = parser.parse_args(argv)
    result = audit_repository(args.repo_root.resolve())
    print(render_report(result, args.format))
    return result.exit_code


if __name__ == "__main__":
    raise SystemExit(main())
