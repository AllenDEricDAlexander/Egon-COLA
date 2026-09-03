package top.egon.cola.archetype.source.web.infrastructure.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class FlywayMigrationConventionTest {

    private static final Pattern VERSIONED_MIGRATION = Pattern.compile(
            "^V(\\d{8})_(\\d{3})__[a-z0-9]+(?:_[a-z0-9]+)*\\.sql$");
    private static final Pattern BASELINE_MIGRATION = Pattern.compile(
            "^B(\\d{8})_(\\d{3})__[a-z0-9]+(?:_[a-z0-9]+)*\\.sql$");

    @Test
    void shouldKeepVersionedHistoryAndDeclareOneBaselinePerRole() throws Exception {
        List<Path> migrations = migrationFiles();
        Set<String> dailySequences = new HashSet<>();
        Map<String, Set<String>> versionedByRole = Map.of(
                "master-data", new HashSet<>(),
                "shard", new HashSet<>());
        Map<String, Set<String>> baselinesByRole = Map.of(
                "master-data", new HashSet<>(),
                "shard", new HashSet<>());

        assertThat(migrations)
                .extracting(path -> path.getFileName().toString())
                .containsExactlyInAnyOrder(
                        "B20260825_003__baseline_organization_master_data_schema.sql",
                        "B20260825_004__baseline_organization_sharded_schema.sql",
                        "V20260726_001__init_organization_master_data_schema.sql",
                        "V20260726_002__init_organization_sharded_schema.sql",
                        "V20260825_003__migrate_organization_master_data_to_egon_model.sql",
                        "V20260825_004__migrate_organization_sharded_to_tenant_model.sql");
        for (Path migration : migrations) {
            String fileName = migration.getFileName().toString();
            Matcher matcher = VERSIONED_MIGRATION.matcher(fileName);
            boolean versioned = matcher.matches();
            Matcher baselineMatcher = BASELINE_MIGRATION.matcher(fileName);
            boolean baseline = baselineMatcher.matches();
            assertThat(versioned || baseline)
                    .as("Flyway 文件名必须采用 V/ByyyyMMdd_NNN__description.sql：%s", fileName)
                    .isTrue();

            String role = migrationRole(migration);
            String version = versioned
                    ? matcher.group(1) + "_" + matcher.group(2)
                    : baselineMatcher.group(1) + "_" + baselineMatcher.group(2);
            assertThat(dailySequences.add((versioned ? "V" : "B") + version))
                    .as("同一 archetype 的日期加序列号必须全局唯一：%s", fileName)
                    .isTrue();
            (versioned ? versionedByRole : baselinesByRole).get(role).add(version);

            String sql = Files.readString(migration, StandardCharsets.UTF_8);
            String header = leadingCommentHeader(sql);
            assertThat(header)
                    .as("迁移文件必须在第一条 SQL 前声明变更内容、影响范围和兼容性说明：%s", fileName)
                    .containsPattern("(?m)^-- 变更内容：\\s*\\S.+$")
                    .containsPattern("(?m)^-- 影响范围：\\s*\\S.+$")
                    .containsPattern("(?m)^-- 兼容性说明：\\s*\\S.+$");
            assertThat(sql)
                    .as("迁移文件不得保留未完成标记：%s", fileName)
                    .doesNotContainIgnoringCase("TODO", "TBD")
                    .doesNotContain("待补充");
            assertThat(fileName)
                .doesNotStartWith("V1__")
                .doesNotStartWith("V2__");
        }

        assertThat(baselinesByRole.get("master-data"))
                .as("master-data 必须只有一个累计 baseline")
                .containsExactly("20260825_003");
        assertThat(baselinesByRole.get("shard"))
                .as("shard 必须只有一个累计 baseline")
                .containsExactly("20260825_004");
        baselinesByRole.forEach((role, baselines) -> {
            assertThat(baselines)
                    .as("每个 role 必须恰好一个 baseline：%s", role)
                    .hasSize(1);
            assertThat(versionedByRole.get(role))
                    .as("每个 role 必须保留 V 历史：%s", role)
                    .contains("20260726_" + (role.equals("master-data") ? "001" : "002"),
                            "20260825_" + (role.equals("master-data") ? "003" : "004"));
            assertThat(baselines.iterator().next())
                    .as("baseline 必须等于该 role 最高 V 版本：%s", role)
                    .isEqualTo(versionedByRole.get(role).stream().max(String::compareTo).orElseThrow());
        });

        assertThat(migrations)
                .noneMatch(path -> path.toString().contains("/migration/default/"))
                .noneMatch(path -> path.toString().contains("/sharding/single/"));
    }

    private static String migrationRole(Path migration) {
        String normalizedPath = migration.toString().replace('\\', '/');
        if (normalizedPath.contains("/master-data/")) {
            return "master-data";
        }
        if (normalizedPath.contains("/shard/")) {
            return "shard";
        }
        throw new IllegalArgumentException("未识别的 Web migration role: " + migration);
    }

    private static List<Path> migrationFiles() throws Exception {
        URL resource = Objects.requireNonNull(
                FlywayMigrationConventionTest.class.getClassLoader()
                        .getResource("db/migration"),
                "db/migration classpath resource");
        Path root = classpathDirectory(resource);
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .sorted()
                    .toList();
        }
    }

    private static Path classpathDirectory(URL resource) throws URISyntaxException {
        assertThat(resource.getProtocol())
                .as("测试要求迁移资源位于生成工程的文件系统 classpath")
                .isEqualTo("file");
        return Path.of(resource.toURI());
    }

    private static String leadingCommentHeader(String sql) {
        StringBuilder header = new StringBuilder();
        for (String line : sql.lines().toList()) {
            if (line.isBlank()) {
                if (!header.isEmpty()) {
                    header.append('\n');
                }
                continue;
            }
            if (!line.stripLeading().startsWith("--")) {
                break;
            }
            header.append(line).append('\n');
        }
        return header.toString();
    }
}
