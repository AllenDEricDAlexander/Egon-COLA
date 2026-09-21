#!/usr/bin/env python3
"""Check Open/Agent declarations separately from optional resolved runtime trees."""

import argparse
import hashlib
import json
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET


NS = {"m": "http://maven.apache.org/POM/4.0.0"}
FAMILIES = ("light-open", "service-open", "web-open", "agent")
COMMON = {
    ("top.egon", "egon-cola-component-common-id-starter"),
    ("top.egon", "egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter"),
    ("top.egon", "egon-cola-component-dynamic-thread-pool-starter"),
}
EXTERNAL = {
    ("org.springframework.cloud", "spring-cloud-starter-bootstrap"),
    ("com.alibaba.cloud", "spring-cloud-starter-alibaba-nacos-discovery"),
    ("com.alibaba.cloud", "spring-cloud-starter-alibaba-nacos-config"),
}
AI = {
    ("top.egon", "egon-cola-component-agent-flow-starter"),
    ("org.springframework.ai", "spring-ai-openai"),
    ("org.springframework.ai", "spring-ai-mcp"),
}
CORE = ("top.egon", "egon-cola-component-common-core")
SHARED_FACADES = frozenset({"egon-cola-organization-facade", "egon-cola-evaluation-facade"})
FACADE_FAMILIES = ("service", "web", "service-open", "web-open")


def value(node, name):
    return node.findtext("m:" + name, default="", namespaces=NS).strip()


def declarations(pom):
    """Managed BOM entries and test/provided dependencies are not runtime edges."""
    return {(value(d, "groupId"), value(d, "artifactId"))
            for d in pom.findall("m:dependencies/m:dependency", NS)
            if value(d, "scope") not in ("test", "provided", "system")}


def native(coordinate):
    group, artifact = coordinate
    return group == "top.egon" and artifact.startswith((
        "egon-cola-component-rpc-", "egon-cola-tianshu",
        "yuheng-starter-openapi"))


def violations(family, coordinates, runtime=False):
    failures = []
    for group, artifact in sorted(coordinates):
        if native((group, artifact)) or (family == "agent" and (
                group.startswith(("org.apache.dubbo", "com.alibaba.nacos", "org.apache.shardingsphere"))
                or "nacos" in artifact)):
            failures.append(f"{family}: forbidden runtime dependency {group}:{artifact}")
    required = AI | {CORE} if family == "agent" else COMMON | EXTERNAL
    if family == "light-open" or runtime:
        required = required | {CORE}
    if family in ("light-open", "web-open"):
        required = required | {("org.springdoc", "springdoc-openapi-starter-webmvc-ui")}
    if family in ("service-open", "web-open"):
        required = required | {("org.apache.dubbo", "dubbo-spring-boot-starter"),
                               ("com.google.protobuf", "protobuf-java")}
        required = required | {("io.grpc", "grpc-api" if runtime or family == "service-open" else "grpc-stub")}
    for group, artifact in sorted(required - coordinates):
        failures.append(f"{family}: missing required dependency {group}:{artifact}")
    if family == "light-open" and any(g.startswith("org.apache.dubbo") for g, _ in coordinates):
        failures.append("light-open: existing source does not consume Dubbo")
    if family == "agent" and runtime and not any(g == "com.google.adk" for g, _ in coordinates):
        failures.append("agent: resolved Agent Flow runtime must retain Google ADK")
    return failures


def runtime_coordinates(text):
    return {(m[0], m[1]) for m in re.findall(
        r"(?:^|\s)([\w.-]+):([\w.-]+):(jar|pom|war):[^\s]+", text, re.M)}


def persistence_violations(root, family):
    """Check the source-owned MP boundary without connecting to a database."""
    failures = []
    java = [path for path in root.rglob("*.java") if "/src/main/java/" in path.as_posix() and "/target/" not in path.as_posix()]
    for path in java:
        text = path.read_text()
        if any(token in text for token in ("EgonColaIService", "EgonColaServiceImpl", "QueryChainWrapper", ".lambdaQuery(", ".query().")):
            failures.append(f"{family}: obsolete persistence API in {path.name}")
        if "/domain/" in path.as_posix() and "component.common.mybatis" in text:
            failures.append(f"{family}: domain depends on MP in {path.name}")
    if family == "agent":
        migrations = [path.name for path in root.rglob("V20260913_001__egon_model_repository.sql") if "/target/" not in path.as_posix()]
        if len(migrations) != 1:
            failures.append("agent: expected exactly one forward model correction")
        return failures
    repositories = [path for path in java if path.name.endswith("Repository.java") and "extends EgonColaRepository<" in path.read_text()]
    expected = 5 if family.startswith("service") else 8
    if len(repositories) != expected:
        failures.append(f"{family}: expected {expected} concrete repositories, found {len(repositories)}")
    manifests = [path for path in root.rglob("repository-manifest.json") if "/target/" not in path.as_posix()]
    if len(manifests) != 1:
        failures.append(f"{family}: expected one managed DDL manifest")
    else:
        manifest = json.loads(manifests[0].read_text())
        scripts = manifest.get("scripts", [])
        if manifest.get("family") != family or len(scripts) != 1:
            failures.append(f"{family}: incorrect initialization manifest")
        for script in scripts:
            resource = manifests[0].parents[2] / script["path"]
            if not resource.is_file() or hashlib.sha256(resource.read_bytes()).hexdigest() != script["sha256"]:
                failures.append(f"{family}: initialization checksum mismatch")
    return failures


def shared_facade_violations(coordinates):
    """The retired cross-family facade artifacts must not be a dependency of any source project."""
    return [f"retired shared facade dependency {group}:{artifact}"
            for group, artifact in sorted(coordinates) if artifact in SHARED_FACADES]


def facade_violations(root, family):
    """Every facade family publishes its own Protobuf contract and consumes it locally."""
    poms = [root / "pom.xml", *sorted(root.glob("*/pom.xml"))]
    coordinates = set().union(*(declarations(ET.parse(pom).getroot()) for pom in poms))
    failures = shared_facade_violations(coordinates)
    facade = root / (root.name + "-facade")
    if not list(facade.glob("src/main/proto/**/*.proto")):
        failures.append("missing local facade Protobuf contract")
    if not any(artifact == facade.name for _, artifact in coordinates):
        failures.append("missing local facade consumer")
    return [f"{family}: {failure}" for failure in failures]


def self_test():
    light = COMMON | EXTERNAL | {CORE, ("org.springdoc", "springdoc-openapi-starter-webmvc-ui")}
    assert not violations("light-open", light)
    assert violations("light-open", light - {CORE}), "missing Common must fail"
    assert violations("light-open", light | {("top.egon", "egon-cola-component-rpc-starter")}), "native starter must fail"
    assert not violations("agent", AI | {CORE, ("io.grpc", "grpc-core")}), "ADK gRPC is permitted"
    assert violations("agent", AI | {CORE, ("org.apache.shardingsphere", "shardingsphere-jdbc")})
    pom = ET.fromstring('<project xmlns="http://maven.apache.org/POM/4.0.0"><dependencyManagement><dependencies><dependency><groupId>top.egon</groupId><artifactId>egon-cola-component-rpc-starter</artifactId></dependency></dependencies></dependencyManagement></project>')
    assert not declarations(pom), "managed dependencies must not become runtime edges"
    assert runtime_coordinates('[INFO] +- io.grpc:grpc-core:jar:1.76.2:compile') == {("io.grpc", "grpc-core")}
    assert shared_facade_violations({("top.egon", "egon-cola-organization-facade")}), "shared facade must fail"
    assert not shared_facade_violations(light), "owned facade coordinates must pass"
    print("PASS: missing Common/native injection/Agent infrastructure/BOM/runtime parser/shared facade fixtures")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--runtime-trees", type=Path, help="Directory containing <family>.txt Maven runtime trees")
    parser.add_argument("--effective-poms", type=Path, help="Directory containing <family>.xml effective reactor POMs")
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    self_test()
    if args.self_test:
        return 0
    failures = []
    for family in FAMILIES:
        root = args.root / "egon-cola-archetypes/source-projects" / ("egon-cola-source-" + family)
        poms = [ET.parse(p).getroot() for p in [root / "pom.xml", *sorted(root.glob("*/pom.xml"))]]
        coords = set().union(*(declarations(p) for p in poms))
        failures.extend(violations(family, coords))
        if args.effective_poms:
            effective = ET.parse(args.effective_poms / (family + ".xml")).getroot()
            projects = [effective] if effective.tag.endswith("project") else effective.findall("m:project", NS)
            failures.extend(violations(family, set().union(*(declarations(p) for p in projects))))
        if args.runtime_trees:
            text = (args.runtime_trees / (family + ".txt")).read_text()
            failures.extend(violations(family, runtime_coordinates(text), runtime=True))
        print(f"CHECK {family}: {len(coords)} declared runtime coordinates; managed BOMs excluded")
    for family in ("light", "light-open", "service", "service-open", "web", "web-open", "agent"):
        source = args.root / "egon-cola-archetypes/source-projects" / ("egon-cola-source-" + family)
        failures.extend(persistence_violations(source, family))
        if family in FACADE_FAMILIES:
            failures.extend(facade_violations(source, family))
    for failure in failures:
        print("ERROR: " + failure, file=sys.stderr)
    if failures:
        return 1
    print("PASS: Open compatibility and Agent minimal dependency boundaries")
    return 0


if __name__ == "__main__":
    sys.exit(main())
