#!/usr/bin/env python3
"""Check native archetype transport dependencies and curated runtime sources."""

from pathlib import Path
import argparse
from functools import lru_cache
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
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


@lru_cache(maxsize=1)
def frozen_wire_identifiers():
    identifiers = set()
    source = ROOT / "egon-cola-archetypes/source-projects"
    for family in ("light", "service", "web"):
        for proto in (source / ("egon-cola-source-" + family)).rglob("*.proto"):
            if "target" in proto.parts:
                continue
            text = proto.read_text()
            package = re.search(r"^\s*package\s+([\w.]+)\s*;", text, re.M)
            if not package:
                continue
            for service, body in re.findall(r"service\s+(\w+)\s*\{([^}]+)\}", text, re.S):
                name = package[1] + "." + service
                identifiers.add(name)
                identifiers.update(name + "/" + method for method in re.findall(r"rpc\s+(\w+)\s*\(", body))
    return identifiers


def retired_violations(relative, contents):
    """Report a retired shared facade reference; the frozen Protobuf wire package stays allowed.

    Test sources are exempt because the ownership contract test must name the retired root to
    assert it no longer resolves; a real test dependency would fail compilation instead.
    """
    if "/src/test/" in "/" + relative.as_posix():
        return []
    checked = FROZEN_WIRE.sub("", contents)
    for identifier in sorted(frozen_wire_identifiers(), key=len, reverse=True):
        checked = re.sub(re.escape(identifier) + r"(?![\w./])", "FROZEN_WIRE_SELECTOR", checked)
    match = RETIRED.search(checked)
    return [] if match is None else [f"{relative}: retired shared facade reference {match.group()}"]


def java_identity_violations(relative, contents, family):
    failures = []
    if "/src/main/java/" not in "/" + relative.as_posix():
        return failures
    if "adapter" not in relative.parts and any(layer in relative.parts for layer in ("domain", "facade")) and re.search(
            r"import\s+(?:org\.springframework|com\.baomidou|top\.egon\.cola\.platform\.tianquan)", contents):
        failures.append(f"{relative}: framework/security dependency in domain/facade")
    if re.search(r'getHeader\s*\(\s*"X-(?:Tenant-Id|Actor-Id|Actor-Roles)"\s*\)', contents):
        failures.append(f"{relative}: untrusted header identity")
    if family == "agent" and relative.name == "ResearchApiKeyFilter.java" and "@Component" in contents:
        failures.append(f"{relative}: legacy API-key filter remains registered")
    return failures


def self_test():
    domain = Path("fixture-domain/src/main/java/example/domain/Order.java")
    adapter = Path("fixture-adapter/src/main/java/example/adapter/facade/Orders.java")
    for dependency in ("org.springframework.security.core.Authentication", "com.baomidou.mybatisplus.core.mapper.BaseMapper",
                       "top.egon.cola.platform.tianquan.jianshen.core.authorization.AuthorizationContext"):
        assert java_identity_violations(domain, "import " + dependency + ";", "agent"), dependency
    assert not java_identity_violations(domain, "import java.util.List;", "agent")
    assert not java_identity_violations(adapter, "import org.springframework.stereotype.Service;", "web")
    assert java_identity_violations(adapter, 'request.getHeader ( "X-Actor-Id" );', "web")
    assert retired_violations(adapter, "import top.egon.cola.organization.facade.Order;")
    assert not retired_violations(adapter, "top.egon.cola.organization.facade.rpc.v1.UserService/GetUser")
    assert retired_violations(adapter, "top.egon.cola.organization.facade.rpc.v1.UserService/UnknownMethod")
    print("PASS: domain framework/security, raw identity and frozen wire boundary fixtures")


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
            for match in FORBIDDEN.finditer(contents):
                if "/src/test/" in "/" + relative.as_posix() and match.group() == "org.springdoc":
                    continue
                failures.append(f"{relative}:{contents[:match.start()].count(chr(10)) + 1}: {match.group().strip()}")
            failures.extend(java_identity_violations(relative, contents, family))
            failures.extend(retired_violations(relative, contents))
            if path.name.startswith("bootstrap"):
                failures.append(f"{relative}: retired bootstrap configuration")
    required = REQUIRED if family != "agent" else (REQUIRED - {"egon-cola-component-rpc-starter"}) | {"egon-cola-tianquan-jianshen-starter"}
    missing = required - dependencies
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
    families = ("light", "service", "web", "agent")
    parser.add_argument("families", nargs="*", metavar="{light,service,web,agent}")
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    unknown = set(args.families) - set(families)
    if unknown:
        parser.error("unknown native families: " + ", ".join(sorted(unknown)))
    self_test()
    if args.self_test:
        return 0
    results = [check(family) for family in (args.families or families)]
    return 0 if all(results) else 1


if __name__ == "__main__":
    sys.exit(main())
