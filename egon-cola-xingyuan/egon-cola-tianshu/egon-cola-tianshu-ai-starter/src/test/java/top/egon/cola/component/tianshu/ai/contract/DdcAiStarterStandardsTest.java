package top.egon.cola.component.tianshu.ai.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TEST-029/030（ai-starter 侧）：业务类 @Slf4j、java.time 与工具库白名单、配置元数据覆盖 Spec §15.4 全部键。
 * / TEST-029/030 (ai-starter side): business classes use @Slf4j, java.time and approved utilities only, and the
 * configuration metadata covers every key of Spec §15.4.
 */
class DdcAiStarterStandardsTest {

    private static final String BASE = "top.egon.cola.component.tianshu.ai.";

    private static final List<String> BUSINESS_CLASSES = List.of(
            "feedback.DefaultDdcAiUsageReporter", "workspace.DdcAiSkillMaterializer",
            "workspace.DdcAiSkillWorkspaceService", "workspace.DdcAiPublicationListener",
            "workspace.DdcAiWorkspaceReconcileScheduler", "authoring.DdcAiSkillTreeReader",
            "authoring.DdcAiSkillAuthoringWatcher");

    private static final Set<String> SPEC_KEYS = Set.of(
            "egon.cola.component.tianshu.ai.feedback.enabled",
            "egon.cola.component.tianshu.ai.feedback.queue-capacity",
            "egon.cola.component.tianshu.ai.feedback.batch-size",
            "egon.cola.component.tianshu.ai.feedback.flush-interval",
            "egon.cola.component.tianshu.ai.feedback.max-attempts",
            "egon.cola.component.tianshu.ai.skill.workspace.enabled",
            "egon.cola.component.tianshu.ai.skill.workspace.root",
            "egon.cola.component.tianshu.ai.skill.workspace.subscription-enabled",
            "egon.cola.component.tianshu.ai.skill.workspace.reconcile-interval",
            "egon.cola.component.tianshu.ai.skill.workspace.resources",
            "egon.cola.component.tianshu.ai.skill.authoring.enabled",
            "egon.cola.component.tianshu.ai.skill.authoring.root",
            "egon.cola.component.tianshu.ai.skill.authoring.poll-interval",
            "egon.cola.component.tianshu.ai.skill.authoring.debounce",
            "egon.cola.component.tianshu.ai.skill.authoring.resources");

    @Test
    void businessClassesDeclareSlf4j() throws Exception {
        for (String name : BUSINESS_CLASSES) {
            Class<?> type = Class.forName(BASE + name);
            Field log = type.getDeclaredField("log");
            assertThat(Modifier.isStatic(log.getModifiers())).as("%s @Slf4j", name).isTrue();
            assertThat(log.getType().getName()).isEqualTo("org.slf4j.Logger");
        }
    }

    @Test
    void sourcesUseJavaTimeAndApprovedUtilitiesOnly() throws IOException {
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                assertThat(Files.readString(file, StandardCharsets.UTF_8)).as(file.toString())
                        .doesNotContain("java.util.Date", "java.util.Calendar", "java.text.SimpleDateFormat",
                                "cn.hutool", "org.joda");
            }
        }
    }

    @Test
    void configurationMetadataListsEverySpecKey() throws IOException {
        Set<String> names = new TreeSet<>();
        try (InputStream metadata = getClass().getResourceAsStream("/META-INF/spring-configuration-metadata.json")) {
            assertThat(metadata).as("configuration metadata").isNotNull();
            for (JsonNode property : new ObjectMapper().readTree(metadata).path("properties")) {
                names.add(property.path("name").asText());
            }
        }
        assertThat(names).containsAll(SPEC_KEYS);
    }
}
