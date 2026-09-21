#!/usr/bin/env python3
"""Check native archetype transport dependencies and curated runtime sources."""

from pathlib import Path
import argparse
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
NS = {"m": "http://maven.apache.org/POM/4.0.0"}
FORBIDDEN = re.compile(r"org\.apache\.dubbo|org\.springframework\.cloud|com\.alibaba\.cloud|"
                       r"org\.springdoc|@EnableDubbo|@Dubbo(Service|Reference)|"
                       r"\b(?:NACOS_|DUBBO_|nacos://)|^\s*(?:dubbo|nacos):", re.MULTILINE)
REQUIRED = {"egon-cola-component-rpc-starter", "egon-cola-component-rpc-tianshu-adapter",
            "egon-cola-tianshu-starter",
            "egon-cola-tianshu-http-registration-starter",
            "yuheng-starter-openapi-webmvc"}
# Each native family owns its contract artifact; the shared artifacts and their Java root are retired.
RETIRED = re.compile(r"top\.egon\.cola\.(?:organization|evaluation)\.facade\b|egon-cola-(?:organization|evaluation)-facade\b")
FROZEN_WIRE = re.compile(r"^\s*package\s+top\.egon\.cola\.(?:organization|evaluation)\.facade\.rpc\.v1\s*;\s*$",
                         re.MULTILINE)


def retired_violations(relative, contents):
    """Report a retired shared facade reference; the frozen Protobuf wire package stays allowed.

    Test sources are exempt because the ownership contract test must name the retired root to
    assert it no longer resolves; a real test dependency would fail compilation instead.
    """
    if "/src/test/" in "/" + relative.as_posix():
        return []
    match = RETIRED.search(FROZEN_WIRE.sub("", contents))
    return [] if match is None else [f"{relative}: retired shared facade reference {match.group()}"]


def check(family):
    source = ROOT / "egon-cola-archetypes/source-projects" / ("egon-cola-source-" + family)
    failures = []
    dependencies = set()
    inspected = 0
    for path in sorted(source.rglob("*")):
        if not path.is_file() or "target" in path.relative_to(source).parts:
            continue
        relative = path.relative_to(source)
        if path.name == "pom.xml":
            model = ET.parse(path).getroot()
            for dependency in model.findall("m:dependencies/m:dependency", NS):
                group = dependency.findtext("m:groupId", "", NS)
                artifact = dependency.findtext("m:artifactId", "", NS)
                dependencies.add(artifact)
                if group.startswith(("org.apache.dubbo", "org.springframework.cloud", "com.alibaba.cloud", "org.springdoc")):
                    failures.append(f"{relative}: forbidden direct dependency {group}:{artifact}")
            failures.extend(retired_violations(relative, path.read_text(encoding="utf-8")))
        elif path.suffix in {".java", ".proto", ".yml", ".yaml", ".md", ".example"} or path.name.startswith(".env"):
            inspected += 1
            contents = path.read_text(encoding="utf-8")
            match = FORBIDDEN.search(contents)
            if match:
                failures.append(f"{relative}:{contents[:match.start()].count(chr(10)) + 1}: {match.group().strip()}")
            failures.extend(retired_violations(relative, contents))
            if path.name.startswith("bootstrap"):
                failures.append(f"{relative}: retired bootstrap configuration")
    missing = REQUIRED - dependencies
    if missing:
        failures.append(f"missing native dependencies: {', '.join(sorted(missing))}")
    if failures:
        for failure in failures:
            print(f"FAIL {family}: {failure}", file=sys.stderr)
        return False
    print(f"PASS {family}: {inspected} source/config/doc files; required native dependencies present")
    return True


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("families", nargs="*", choices=["light", "service", "web"])
    args = parser.parse_args()
    results = [check(family) for family in (args.families or ["light", "service", "web"])]
    return 0 if all(results) else 1


if __name__ == "__main__":
    sys.exit(main())
