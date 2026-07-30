package top.egon.cola.platform.rbac3.admin.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class Rbac3MigrationContractTest {

    private static final String MIGRATION =
            "db/migration/V1__create_rbac3_schema.sql";
    private static final Pattern TABLE_PATTERN = Pattern.compile(
            "create\\s+table\\s+(rbac3_[a-z0-9_]+)\\s*\\((.*?)\\);",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Set<String> REQUIRED_TABLES = Set.of(
            "rbac3_tenant",
            "rbac3_user",
            "rbac3_user_credential",
            "rbac3_external_identity",
            "rbac3_directory_snapshot",
            "rbac3_org_unit",
            "rbac3_position",
            "rbac3_user_position_snapshot",
            "rbac3_service_principal",
            "rbac3_service_credential",
            "rbac3_service_permission",
            "rbac3_application",
            "rbac3_resource_manifest",
            "rbac3_resource",
            "rbac3_permission",
            "rbac3_permission_resource",
            "rbac3_role",
            "rbac3_role_inheritance",
            "rbac3_role_closure",
            "rbac3_role_permission",
            "rbac3_data_rule",
            "rbac3_data_rule_ref",
            "rbac3_field_definition",
            "rbac3_field_rule",
            "rbac3_user_role_assignment",
            "rbac3_auto_assignment_rule",
            "rbac3_role_prerequisite",
            "rbac3_role_cardinality",
            "rbac3_sod_set",
            "rbac3_sod_member",
            "rbac3_operation_sod_rule",
            "rbac3_business_participation",
            "rbac3_management_policy",
            "rbac3_management_subject",
            "rbac3_management_scope",
            "rbac3_management_role",
            "rbac3_management_operation",
            "rbac3_session",
            "rbac3_session_active_role",
            "rbac3_refresh_token",
            "rbac3_idempotency_record",
            "rbac3_authorization_mutation",
            "rbac3_audit_log"
    );

    @Test
    void definesAllRequiredTablesAndNoRotationApprovalOrOutboxTables()
            throws IOException {
        String sql = migrationSql();

        assertThat(tableBodies(sql).keySet()).containsExactlyInAnyOrderElementsOf(
                REQUIRED_TABLES
        );
        assertThat(sql)
                .doesNotContain("rbac3_role_rotation")
                .doesNotContain("rbac3_rotation_handover")
                .doesNotContain("rbac3_rotation_execution_log")
                .doesNotContain("approval_policy_id")
                .doesNotContain("approver_role_id")
                .doesNotContain("required_approvals")
                .doesNotContain("create table egon_cola_outbox_message")
                .doesNotContain("create table rbac3_outbox_event");
    }

    @Test
    void onlyOneRbac3MigrationExists() throws Exception {
        assertThat(listMigrationResources()).containsExactly(MIGRATION);
    }

    @Test
    void tenantReferencesUseCompositeDatabaseKeys() throws IOException {
        Map<String, String> tables = tableBodies(migrationSql());

        assertThat(tables.get("rbac3_user"))
                .contains("unique (tenant_id, id)")
                .contains("unique (tenant_id, normalized_username)")
                .contains("foreign key (tenant_id) references rbac3_tenant(id)");
        assertThat(tables.get("rbac3_user_role_assignment"))
                .contains("foreign key (tenant_id, user_id)")
                .contains("references rbac3_user(tenant_id, id)")
                .contains("foreign key (tenant_id, role_id)")
                .contains("references rbac3_role(tenant_id, id)");
        assertThat(tables.get("rbac3_session_active_role"))
                .contains("foreign key (tenant_id, session_id)")
                .contains("references rbac3_session(tenant_id, session_id)");
    }

    @Test
    void roleAndResourceReferencesCannotCrossApplications() throws IOException {
        Map<String, String> tables = tableBodies(migrationSql());

        assertThat(tables.get("rbac3_resource"))
                .contains("unique (tenant_id, application_id, id)")
                .contains("foreign key (tenant_id, application_id, parent_resource_id)")
                .contains("references rbac3_resource(tenant_id, application_id, id)");
        assertThat(tables.get("rbac3_role"))
                .contains("unique (tenant_id, application_id, id)")
                .contains("unique (tenant_id, application_id, role_code)");
        assertThat(tables.get("rbac3_role_inheritance"))
                .contains("foreign key (tenant_id, application_id, senior_role_id)")
                .contains("foreign key (tenant_id, application_id, junior_role_id)")
                .contains("references rbac3_role(tenant_id, application_id, id)");
        assertThat(tables.get("rbac3_role_permission"))
                .contains("application_id bigint not null")
                .contains("foreign key (tenant_id, application_id, role_id)")
                .contains("foreign key (tenant_id, application_id, permission_id)");
    }

    @Test
    void enforcesSessionRootClosureTimeWindowAndStateConstraints()
            throws IOException {
        Map<String, String> tables = tableBodies(migrationSql());

        assertThat(tables.get("rbac3_session_active_role"))
                .contains("unique (tenant_id, session_id, root_role_id)")
                .contains("foreign key (tenant_id, application_id, root_role_id)");
        assertThat(tables.get("rbac3_role_closure"))
                .contains("check (depth between 0 and 10)")
                .contains("check ((ancestor_role_id = descendant_role_id) = (depth = 0))");
        assertThat(tables.get("rbac3_user_role_assignment"))
                .contains("check (valid_to is null or valid_to > valid_from)")
                .contains("check (assignment_type not in ('temporary', 'emergency')")
                .contains("check (status in ('pending', 'active', 'suspended', 'expired', 'revoked'))");
        assertThat(tables.get("rbac3_session"))
                .contains("check (idle_expires_at > authenticated_at)")
                .contains("check (absolute_expires_at > authenticated_at)")
                .contains("check (status in ('active', 'revoked', 'expired'))");
        assertThat(tables.get("rbac3_authorization_mutation"))
                .contains("check (scope_type <> 'session' or session_id is not null)")
                .contains("check (status in ('preparing', 'committed', 'projected', 'completed', 'aborted', 'recovery_required'))");
    }

    @Test
    void protectsAppendOnlyFactsAndDefinesOperationalIndexes()
            throws IOException {
        String sql = migrationSql();

        assertThat(sql)
                .contains("create function rbac3_reject_append_only_change()")
                .contains("create trigger trg_rbac3_audit_log_append_only")
                .contains("before update or delete on rbac3_audit_log")
                .contains("create trigger trg_rbac3_business_participation_append_only")
                .contains("before update or delete on rbac3_business_participation")
                .contains("create index idx_rbac3_assignment_user_active_window")
                .contains("on rbac3_user_role_assignment (tenant_id, user_id, status, valid_from, valid_to)")
                .contains("create index idx_rbac3_closure_descendant_depth")
                .contains("on rbac3_role_closure (tenant_id, application_id, descendant_role_id, depth)")
                .contains("create index idx_rbac3_participation_conflict")
                .contains("actor_user_id, action_code")
                .contains("create index idx_rbac3_audit_tenant_created")
                .contains("on rbac3_audit_log (tenant_id, created_at desc)");
    }

    private Map<String, String> tableBodies(String sql) {
        Matcher matcher = TABLE_PATTERN.matcher(sql);
        Map<String, String> tables = new LinkedHashMap<>();
        while (matcher.find()) {
            tables.put(normalize(matcher.group(1)), normalize(matcher.group(2)));
        }
        return tables;
    }

    private String migrationSql() throws IOException {
        try (var input = getClass().getClassLoader().getResourceAsStream(MIGRATION)) {
            assertThat(input).as("RBAC3 V1 migration resource").isNotNull();
            return normalize(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private List<String> listMigrationResources()
            throws URISyntaxException, IOException {
        Path testClasses = Path.of(getClass().getProtectionDomain()
                .getCodeSource().getLocation().toURI());
        Path migrationDirectory = testClasses.getParent()
                .resolve("classes/db/migration");
        if (Files.notExists(migrationDirectory)) {
            return List.of();
        }
        try (var files = Files.list(migrationDirectory)) {
            return files.filter(Files::isRegularFile)
                    .map(path -> "db/migration/" + path.getFileName())
                    .sorted()
                    .toList();
        }
    }

    private String normalize(String value) {
        return Arrays.stream(value.toLowerCase().replace('\r', '\n').split("\\s+"))
                .filter(part -> !part.isBlank())
                .reduce((left, right) -> left + " " + right)
                .orElse("");
    }
}
