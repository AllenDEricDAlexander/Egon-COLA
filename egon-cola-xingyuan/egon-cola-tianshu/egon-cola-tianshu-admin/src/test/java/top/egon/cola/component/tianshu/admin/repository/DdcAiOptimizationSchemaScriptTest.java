package top.egon.cola.component.tianshu.admin.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class DdcAiOptimizationSchemaScriptTest {

    private static final String SCRIPT = "db/egon-mp/V20261017_001__tianshu_ai_feedback_optimization.sql";

    @Test
    void scriptCreatesThreeTenantScopedTablesWithStateConstraints() throws Exception {
        String sql = text(SCRIPT).toLowerCase(Locale.ROOT);

        assertThat(sql).contains(
                "create table ddc_ai_usage_feedback_t0",
                "create table ddc_ai_optimization_policy_t0",
                "create table ddc_ai_optimization_run_t0",
                "judge_attempts smallint not null default 0 check (judge_attempts between 0 and 10)",
                "constraint uk_ai_usage_feedback_usage unique (tenant_id, usage_id)",
                "constraint ck_ai_usage_feedback_judge_done check ((judge_state = 'done') = (judge_score is not null))",
                "constraint uk_ai_optimization_run_no unique (tenant_id, resource_id, run_no)",
                "constraint ck_ai_optimization_run_finished",
                "constraint fk_ai_optimization_run_publication foreign key (tenant_id, publication_id)")
                .doesNotContain("if not exists");
        for (String table : new String[] {"ddc_ai_usage_feedback_t0", "ddc_ai_optimization_policy_t0",
                "ddc_ai_optimization_run_t0"}) {
            var body = Pattern.compile("(?s)create table " + table + " \\((.*?)\\n\\);").matcher(sql);
            assertThat(body.find()).as(table).isTrue();
            assertThat(body.group(1)).contains("tenant_id bigint not null check (tenant_id > 0)",
                    "deleted_at timestamp(6) without time zone", "version bigint not null default 0");
        }
    }

    @Test
    void lifecycleAndPartialUniqueIndexesIncludeDeletedAt() throws Exception {
        String sql = text(SCRIPT).toLowerCase(Locale.ROOT);

        assertThat(sql).contains(
                "create unique index uk_ai_optimization_policy_lifecycle\n"
                        + "    on ddc_ai_optimization_policy_t0 (tenant_id, resource_id, deleted_at);",
                "create unique index uk_ai_optimization_policy_live\n"
                        + "    on ddc_ai_optimization_policy_t0 (tenant_id, resource_id) where deleted_at is null;",
                "create unique index uk_ai_optimization_run_active",
                "where state in ('pending', 'running') and deleted_at is null",
                "create unique index uk_ai_optimization_run_request",
                "where request_id is not null",
                "where deleted_at is null and judge_state in ('pending', 'judging')");
    }

    @Test
    void manifestAppendsScriptWithMatchingSha256AndKeepsHistory() throws Exception {
        JsonNode manifest = new ObjectMapper().readTree(bytes("db/egon-mp/repository-manifest.json"));

        assertThat(manifest.path("scripts").size()).isEqualTo(4);
        JsonNode last = manifest.path("scripts").get(2);
        assertThat(last.path("version").asText()).isEqualTo("20261017_001");
        assertThat(last.path("path").asText()).isEqualTo(SCRIPT);
        assertThat(last.path("sha256").asText()).isEqualTo(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes(SCRIPT))));
        assertThat(manifest.path("scripts").get(0).path("version").asText()).isEqualTo("20260922_001");
        assertThat(manifest.path("scripts").get(1).path("version").asText()).isEqualTo("20260928_001");
    }

    @Test
    void persistenceYamlRegistersThreeLogicalTables() throws Exception {
        assertThat(text("egon-tianshu-persistence.yml")).contains(
                "ddc_ai_usage_feedback: { type: STANDARD_TENANT_ID }",
                "ddc_ai_optimization_policy: { type: STANDARD_TENANT_ID }",
                "ddc_ai_optimization_run: { type: STANDARD_TENANT_ID }");
    }

    private static String text(String path) throws Exception {
        return new String(bytes(path), StandardCharsets.UTF_8);
    }

    private static byte[] bytes(String path) throws Exception {
        try (var input = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            assertThat(input).as(path).isNotNull();
            return input.readAllBytes();
        }
    }
}
