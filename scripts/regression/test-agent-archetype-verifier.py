#!/usr/bin/env python3
"""Exercise the real Agent Groovy verifier against an existing generated-project fixture.

The fixture keeps actual sources/reports but uses an entry-only archive for library policy
mutations. Full archetype IT remains responsible for verifying the real packaged runtime.
No application, database, listener, or provider is started by this regression harness.
"""

from pathlib import Path
import argparse
import shutil
import subprocess
import tempfile
import unittest
import zipfile

ROOT = Path(__file__).resolve().parents[2]


class AgentVerifierTest(unittest.TestCase):
    def setUp(self):
        self.project = self.work / "target" / self.id().split(".")[-1] / self.template.name
        shutil.copytree(self.template, self.project)
        self.jar = self.project / "deep-research-agent-starter/target/runtime.jar"

    def verify(self):
        return subprocess.run(
            ["java", "-cp", self.classpath, "groovy.ui.GroovyMain", str(self.runner),
             str(self.project), str(self.verifier)],
            capture_output=True, text=True, timeout=45,
        )

    def reject(self, message):
        result = self.verify()
        self.assertNotEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertIn(message, result.stdout + result.stderr)

    def add_library(self, name):
        with zipfile.ZipFile(self.jar, "a") as archive:
            archive.writestr("BOOT-INF/lib/" + name, b"")

    def test_native_sdk_fixture_passes_under_target_ancestor(self):
        result = self.verify()
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)

    def test_package_path_mismatch_is_rejected(self):
        path = self.project / "deep-research-agent-adapter/src/main/java/it/pkg/adapter/knowledge/pojo/dto/CreateKnowledgeBaseDTO.java"
        text = path.read_text()
        path.write_text(text.replace("package it.pkg.adapter.knowledge.pojo.dto;", "package it.pkg.adapter.knowledge.controller;", 1))
        self.reject("Java package/path mismatch")

    def test_business_request_suffix_is_rejected(self):
        path = self.project / "deep-research-agent-adapter/src/main/java/it/pkg/adapter/knowledge/pojo/dto/LegacyRequest.java"
        path.write_text("package it.pkg.adapter.knowledge.pojo.dto;\npublic class LegacyRequest {}\n")
        self.reject("Business carrier must use DTO/Command/Query or Result/VO")

    def test_unknown_rpc_module_is_rejected(self):
        self.add_library("egon-cola-component-rpc-unapproved-5.4.1.jar")
        self.reject("Unapproved Agent RPC/Tianshu runtime library")

    def test_rpc_prefix_lookalike_is_rejected(self):
        self.add_library("egon-cola-component-rpc-starter-extra-5.4.1.jar")
        self.reject("Unapproved Agent RPC/Tianshu runtime library")

    def test_tianshu_admin_is_rejected(self):
        self.add_library("egon-cola-tianshu-admin-5.4.1.jar")
        self.reject("Unapproved Agent RPC/Tianshu runtime library")

    def test_dubbo_remains_rejected(self):
        self.add_library("dubbo-3.3.5.jar")
        self.reject("Forbidden Agent runtime library dubbo-")

    def test_business_rpc_import_is_rejected(self):
        path = self.project / "deep-research-agent-application/src/main/java/it/pkg/application/research/manage/ForbiddenTransport.java"
        path.write_text("package it.pkg.application.research.manage;\n"
                        "import top.egon.cola.component.rpc.stub.RpcStubFactory;\n"
                        "class ForbiddenTransport {}\n")
        self.reject("Business source must not own SDK transport")

    def test_registered_legacy_filter_is_rejected(self):
        path = self.project / "deep-research-agent-adapter/src/main/java/it/pkg/adapter/filter/ResearchApiKeyFilter.java"
        path.write_text(path.read_text().replace("@Deprecated", "@Deprecated\n@Component"))
        self.reject("Legacy API-key filter must remain an isolated compatibility fixture")

    def test_missing_native_user_gate_is_rejected(self):
        paths = self.project.glob("deep-research-agent-adapter/src/main/java/it/pkg/adapter/**/controller/*.java")
        path = next(path for path in paths if "@PreAuthorize(" in path.read_text())
        path.write_text(path.read_text().replace("@PreAuthorize(", "@RemovedUserGate(", 1))
        self.reject("Every USER entry requires its own consent gate")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", type=Path, required=True, help="Already verified/packaged generated Agent project")
    parser.add_argument("--groovy-classpath", required=True, help="Existing groovy and groovy-xml jars")
    parser.add_argument("--verifier", type=Path, default=ROOT / "egon-cola-archetypes/definitions/egon-cola-archetype-agent/src/test/resources/projects/basic/verify.groovy")
    args = parser.parse_args()
    with tempfile.TemporaryDirectory(prefix="agent-verifier-") as temporary:
        work = Path(temporary)
        docs = next((parent / "architecture-docs" for parent in args.project.parents
                     if (parent / "architecture-docs").is_dir()), None)
        if docs is None:
            parser.error("Generated project must retain its archetype architecture-docs ancestor")
        shutil.copytree(docs, work / "architecture-docs")
        template = work / "template" / args.project.name
        shutil.copytree(args.project, template, ignore=shutil.ignore_patterns("target"))
        for module in args.project.glob("deep-research-agent-*"):
            reports = module / "target/surefire-reports"
            if reports.is_dir():
                shutil.copytree(reports, template / module.name / "target/surefire-reports")
        starter = args.project / "deep-research-agent-starter/target"
        target = template / "deep-research-agent-starter/target"
        target.mkdir(parents=True, exist_ok=True)
        shutil.copy2(starter / "launch.args", target / "launch.args")
        archives = [path for path in starter.glob("*.jar") if not path.name.endswith(("-sources.jar", "-javadoc.jar"))]
        if len(archives) != 1:
            parser.error("Expected exactly one packaged Agent runtime jar")
        with zipfile.ZipFile(archives[0]) as original, zipfile.ZipFile(target / "runtime.jar", "w") as fixture:
            for entry in original.namelist():
                if entry.startswith("BOOT-INF/lib/"):
                    fixture.writestr(entry, b"")
        runner = work / "run.groovy"
        runner.write_text("def project = new File(args[0])\n"
                          "new GroovyShell(new Binding([basedir: project, context: [projectDir: project]]))"
                          ".evaluate(new File(args[1]))\n")
        AgentVerifierTest.work = work
        AgentVerifierTest.template = template
        AgentVerifierTest.runner = runner
        AgentVerifierTest.classpath = args.groovy_classpath
        AgentVerifierTest.verifier = args.verifier.resolve()
        result = unittest.TextTestRunner(verbosity=2).run(unittest.defaultTestLoader.loadTestsFromTestCase(AgentVerifierTest))
        return 0 if result.wasSuccessful() else 1


if __name__ == "__main__":
    raise SystemExit(main())
