#!/usr/bin/env python3
"""Check the published parent and dependency owners of all source archetypes."""

import argparse
from pathlib import Path
import sys
import xml.etree.ElementTree as ET


NS = {"m": "http://maven.apache.org/POM/4.0.0"}
FAMILIES = ("light", "service", "web", "light-open", "service-open", "web-open", "agent")
PARENT = "egon-cola-archetypes-parent"
BOM = "egon-cola-components-bom"


def value(element, path):
    return element.findtext(path, default="", namespaces=NS).strip()


def dependencies(element):
    return element.findall("m:dependencyManagement/m:dependencies/m:dependency", NS)


def coordinate(dependency):
    return value(dependency, "m:groupId"), value(dependency, "m:artifactId")


def check_parent(pom, name, version, failures):
    parent = pom.find("m:parent", NS)
    if parent is None:
        failures.append(f"{name}: missing published archetypes parent")
        return
    if coordinate(parent) != ("top.egon", PARENT) or value(parent, "m:version") != version:
        failures.append(f"{name}: parent must be top.egon:{PARENT}:{version}")
    relative = parent.find("m:relativePath", NS)
    if relative is None or (relative.text or "").strip():
        failures.append(f"{name}: parent must declare an empty relativePath")
    if value(pom, "m:version") != "0.1.0-SNAPSHOT":
        failures.append(f"{name}: internal source project version changed")
    if value(pom, "m:properties/m:egon-cola.version") != version:
        failures.append(f"{name}: egon-cola.version must match the published parent")


def check_component_versions(pom, name, component_ids, failures):
    for dependency in pom.findall(".//m:dependency", NS):
        group_id, artifact_id = coordinate(dependency)
        if group_id == "top.egon" and artifact_id in component_ids:
            if dependency.find("m:version", NS) is not None:
                failures.append(f"{name}: {artifact_id} must use its Components BOM version")
        if group_id == "org.apache.shardingsphere" and dependency.find("m:version", NS) is not None:
            failures.append(f"{name}: {artifact_id} must use the archetypes parent version")
        if (group_id, artifact_id) == ("org.apache.commons", "commons-lang3"):
            if dependency.find("m:version", NS) is not None:
                failures.append(f"{name}: commons-lang3 cannot have a local version")
    for dependency in dependencies(pom):
        if coordinate(dependency) == ("top.egon", BOM):
            failures.append(f"{name}: Components BOM is already imported by the parent")


def check_commons_owner(repo, archetypes, components_bom, failures):
    if value(components_bom, "m:properties/m:commons.lang3.version") != "3.20.0":
        failures.append("Components BOM must own the existing Common Core commons-lang3 3.20.0")
    commons = [d for d in dependencies(components_bom)
               if coordinate(d) == ("org.apache.commons", "commons-lang3")]
    if len(commons) != 1 or value(commons[0], "m:version") != "${commons.lang3.version}":
        failures.append("Components BOM must export one managed commons-lang3 declaration")
    root = ET.parse(repo / "pom.xml").getroot()
    if value(root, "m:properties/m:commons-lang3.version") != value(
            components_bom, "m:properties/m:commons.lang3.version"):
        failures.append("aggregation root Boot Commons override must match the Components BOM")
    for name, pom in (("archetypes parent", archetypes),
                      ("components parent", ET.parse(repo / "egon-cola-components/pom.xml").getroot())):
        for key in ("commons-lang3.version", "commons.lang3.version"):
            if pom.find(f"m:properties/m:{key}", NS) is not None:
                failures.append(f"{name}: duplicate Commons version property {key}")
        if any(coordinate(d) == ("org.apache.commons", "commons-lang3") for d in dependencies(pom)):
            failures.append(f"{name}: Commons management must come from Components BOM")
    core = ET.parse(repo / "egon-cola-components/egon-cola-component-common/"
                    "egon-cola-component-common-core/pom.xml").getroot()
    if not any(coordinate(d) == ("org.apache.commons", "commons-lang3")
               for d in core.findall("m:dependencies/m:dependency", NS)):
        failures.append("Common Core must continue providing commons-lang3 transitively")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--effective-poms", type=Path,
                        help="Directory containing Maven effective POMs named <family>.xml")
    args = parser.parse_args()
    repo = args.root.resolve()
    root = ET.parse(repo / "pom.xml").getroot()
    archetypes = ET.parse(repo / "egon-cola-archetypes/pom.xml").getroot()
    components_bom = ET.parse(repo / "egon-cola-components/egon-cola-components-bom/pom.xml").getroot()
    version = value(root, "m:version")
    failures = []
    if (value(root, "m:parent/m:groupId"), value(root, "m:parent/m:artifactId")) != (
            "org.springframework.boot", "spring-boot-starter-parent"):
        failures.append("aggregation root must retain the Spring Boot parent")
    imports = [d for d in dependencies(archetypes) if coordinate(d) == ("top.egon", BOM)]
    if len(imports) != 1 or value(imports[0], "m:scope") != "import":
        failures.append("archetypes parent must import Components BOM exactly once")
    if value(archetypes, "m:properties/m:shardingsphere.version") != "5.5.3":
        failures.append("archetypes parent must own ShardingSphere 5.5.3")
    for key in ("spring-cloud.version", "spring-cloud-alibaba.version", "dubbo.version"):
        if root.find(f"m:properties/m:{key}", NS) is not None:
            failures.append(f"aggregation root cannot own optional {key}")
    component_ids = {value(d, "m:artifactId") for d in dependencies(components_bom)
                     if value(d, "m:groupId") == "top.egon"}
    check_commons_owner(repo, archetypes, components_bom, failures)
    sources = repo / "egon-cola-archetypes/source-projects"
    for family in FAMILIES:
        source = sources / f"egon-cola-source-{family}"
        root_pom = ET.parse(source / "pom.xml").getroot()
        check_parent(root_pom, source.name, version, failures)
        for path in [source / "pom.xml", *sorted(source.glob("*/pom.xml"))]:
            pom = ET.parse(path).getroot()
            name = str(path.relative_to(repo))
            check_component_versions(pom, name, component_ids, failures)
            for key in ("commons-lang3.version", "commons.lang3.version", "shardingsphere.version",
                        "spring-cloud.version", "spring-cloud-alibaba.version", "dubbo.version",
                        "grpc.version", "protobuf.version"):
                if pom.find(f"m:properties/m:{key}", NS) is not None:
                    failures.append(f"{name}: duplicate version owner {key}")
        if args.effective_poms:
            effective = ET.parse(args.effective_poms / f"{family}.xml").getroot()
            resolved = {coordinate(d): value(d, "m:version") for d in dependencies(effective)}
            for artifact, expected in (("commons-lang3", "3.20.0"),
                                       ("shardingsphere-jdbc", "5.5.3"),
                                       ("egon-cola-component-common-core", version)):
                group = {"commons-lang3": "org.apache.commons",
                         "shardingsphere-jdbc": "org.apache.shardingsphere"}.get(artifact, "top.egon")
                if resolved.get((group, artifact)) != expected:
                    failures.append(f"{family} effective POM: {artifact} must resolve to {expected}")
    if failures:
        for failure in failures:
            print(f"ERROR: {failure}", file=sys.stderr)
        print(f"FAIL: {len(failures)} dependency ownership violation(s)", file=sys.stderr)
        return 1
    print("PASS: seven source parents, Components BOM, Commons and ShardingSphere owners")
    return 0


if __name__ == "__main__":
    sys.exit(main())
