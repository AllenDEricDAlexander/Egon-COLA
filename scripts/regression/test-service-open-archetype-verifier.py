#!/usr/bin/env python3
"""Verify Service Open test-report policy using an already built generated project.

Only fixture report metadata is mutated. No Triple listener or application is started;
accepting a synthetic executed report tests policy, not transport interoperability.
"""

from pathlib import Path
import argparse
import os
import shutil
import subprocess
import tempfile
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]


def copy_fixture_file(source, destination):
    if Path(source).suffix == ".jar":
        os.link(source, destination)
        return destination
    return shutil.copy2(source, destination)


class ServiceOpenVerifierTest(unittest.TestCase):
    def setUp(self):
        self.project = self.work / "target" / self.id().split(".")[-1] / self.template.name
        shutil.copytree(self.template, self.project, copy_function=copy_fixture_file)
        self.report = next(self.project.rglob("TEST-*.EvaluationDubboTripleIntegrationTest.xml"))

    def verify(self, enabled=False):
        command = ["java"]
        if enabled:
            command.append("-Degon.test.triple.enabled=true")
        command.extend(["-cp", self.classpath, "groovy.ui.GroovyMain", str(self.runner),
                        str(self.project), str(self.verifier)])
        return subprocess.run(command, capture_output=True, text=True, timeout=45)

    def reject(self, message, enabled=False):
        result = self.verify(enabled)
        self.assertNotEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertIn(message, result.stdout + result.stderr)

    def change_report(self, **attributes):
        tree = ET.parse(self.report)
        tree.getroot().attrib.update(attributes)
        tree.write(self.report, encoding="utf-8", xml_declaration=True)

    def test_explicit_listener_gate_skip_is_recorded(self):
        result = self.verify()
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertIn("Triple listener validation is pending", result.stdout)

    def test_missing_transport_report_is_rejected(self):
        self.report.unlink()
        self.reject("Expected generated Service test report EvaluationDubboTripleIntegrationTest")

    def test_failed_transport_report_is_rejected(self):
        self.change_report(failures="1", skipped="0")
        self.reject("Expected generated Service test report EvaluationDubboTripleIntegrationTest")

    def test_error_transport_report_is_rejected(self):
        self.change_report(errors="1", skipped="0")
        self.reject("Expected generated Service test report EvaluationDubboTripleIntegrationTest")

    def test_zero_executed_or_gated_tests_are_rejected(self):
        self.change_report(tests="0", skipped="0")
        self.reject("Expected the complete Triple interoperability test")

    def test_unexpected_skip_reason_is_rejected(self):
        tree = ET.parse(self.report)
        tree.find("./testcase/skipped").set("message", "@Disabled")
        tree.write(self.report, encoding="utf-8", xml_declaration=True)
        self.reject("Triple test may only be skipped by its explicit listener gate")

    def test_enabled_but_skipped_listener_is_rejected(self):
        self.reject("Enabled Triple test must execute", enabled=True)

    def test_executed_success_report_is_accepted(self):
        tree = ET.parse(self.report)
        tree.getroot().set("skipped", "0")
        case = tree.find("./testcase")
        case.remove(case.find("skipped"))
        tree.write(self.report, encoding="utf-8", xml_declaration=True)
        result = self.verify(enabled=True)
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertNotIn("Triple listener validation is pending", result.stdout)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", type=Path, required=True, help="Built generated Service Open project with its default listener-gated report")
    parser.add_argument("--groovy-classpath", required=True, help="Existing groovy, groovy-xml and groovy-json jars")
    parser.add_argument("--verifier", type=Path, default=ROOT / "egon-cola-archetypes/definitions/egon-cola-archetype-service-open/src/test/resources/projects/basic/verify.groovy")
    args = parser.parse_args()
    with tempfile.TemporaryDirectory(prefix="service-open-verifier-") as temporary:
        work = Path(temporary)
        template = work / "template" / args.project.name
        shutil.copytree(args.project, template, ignore=shutil.ignore_patterns("target"))
        for module in args.project.glob("student-management-evaluation-*"):
            reports = module / "target/surefire-reports"
            if reports.is_dir():
                shutil.copytree(reports, template / module.name / "target/surefire-reports")
        starter = args.project / "student-management-evaluation-starter/target"
        target = template / "student-management-evaluation-starter/target"
        target.mkdir(parents=True, exist_ok=True)
        shutil.copy2(starter / "launch.args", target / "launch.args")
        archives = [path for path in starter.glob("*.jar") if not path.name.endswith(("-sources.jar", "-javadoc.jar"))]
        if len(archives) != 1:
            parser.error("Expected exactly one packaged Service Open runtime jar")
        shutil.copy2(archives[0], target / archives[0].name)
        runner = work / "run.groovy"
        runner.write_text("def project = new File(args[0])\n"
                          "new GroovyShell(new Binding([basedir: project, context: [projectDir: project]]))"
                          ".evaluate(new File(args[1]))\n")
        ServiceOpenVerifierTest.work = work
        ServiceOpenVerifierTest.template = template
        ServiceOpenVerifierTest.runner = runner
        ServiceOpenVerifierTest.classpath = args.groovy_classpath
        ServiceOpenVerifierTest.verifier = args.verifier.resolve()
        result = unittest.TextTestRunner(verbosity=2).run(unittest.defaultTestLoader.loadTestsFromTestCase(ServiceOpenVerifierTest))
        return 0 if result.wasSuccessful() else 1


if __name__ == "__main__":
    raise SystemExit(main())
