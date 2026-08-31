import tempfile
import unittest
from pathlib import Path

from controller_ui_coverage import (
    AuditRow,
    API_CONSUMED,
    PROTOCOL_OR_INTERNAL,
    UNCONSUMED,
    classify,
    inventory_java,
    inventory_web,
)


class ControllerUiCoverageTest(unittest.TestCase):
    def test_composes_class_and_method_mapping(self):
        source = '''
        @RestController
        @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
        @RequestMapping("/api")
        public class UserController {
            @GetMapping("/users")
            public String users() { return "ok"; }
            @GetMapping("/users/page")
            public Map<
                    String, Object> page() { return Map.of(); }
        }
        '''
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'UserController.java'
            path.write_text(source, encoding='utf-8')
            rows = inventory_java(Path(directory), 'TEST')

        self.assertEqual(len(rows), 2)
        self.assertEqual(rows[0].http_method, 'GET')
        self.assertEqual(rows[0].path, '/api/users')
        self.assertEqual(rows[0].method, 'users')
        self.assertEqual(rows[1].path, '/api/users/page')
        self.assertEqual(rows[1].method, 'page')

    def test_classifies_consumed_protocol_and_unconsumed_rows(self):
        consumed = AuditRow('TEST', 'UserController', 'users', 'GET', '/api/users')
        protocol = AuditRow('TEST', 'OAuthController', 'token', 'POST', '/oauth2/token')
        missing = AuditRow('TEST', 'RoleController', 'create', 'POST', '/api/roles')

        result = classify([consumed, protocol, missing], {'source.ts': "'/api/users'"})

        self.assertEqual(result.rows[0].status, API_CONSUMED)
        self.assertEqual(result.rows[1].status, PROTOCOL_OR_INTERNAL)
        self.assertEqual(result.rows[2].status, UNCONSUMED)
        self.assertEqual(result.exit_code, 1)

    def test_excludes_test_and_spec_files_from_production_evidence(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'App.tsx').write_text("'/api/users'", encoding='utf-8')
            (root / 'App.test.tsx').write_text("'/api/only-in-test'", encoding='utf-8')
            (root / 'App.spec.ts').write_text("'/api/only-in-spec'", encoding='utf-8')

            files = inventory_web(root)

        self.assertEqual(list(files), ['App.tsx'])


if __name__ == '__main__':
    unittest.main()
