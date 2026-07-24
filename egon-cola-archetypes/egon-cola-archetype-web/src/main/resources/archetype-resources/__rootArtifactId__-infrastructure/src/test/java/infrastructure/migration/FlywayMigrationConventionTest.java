package ${package}.infrastructure.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class FlywayMigrationConventionTest {

    private static final Pattern VERSIONED_MIGRATION = Pattern.compile(
            "^V(\\d{8})_(\\d{3})__[a-z0-9]+(?:_[a-z0-9]+)*\\.sql$");

    @Test
    void shouldUseGlobalDailySequenceAndCompleteHeaderComments() throws Exception {
        List<Path> migrations = migrationFiles();
        Set<String> dailySequences = new HashSet<>();

        assertThat(migrations).isNotEmpty();
        for (Path migration : migrations) {
            String fileName = migration.getFileName().toString();
            Matcher matcher = VERSIONED_MIGRATION.matcher(fileName);
            assertThat(matcher.matches())
                    .as("Flyway 文件名必须采用 VyyyyMMdd_NNN__description.sql：%s", fileName)
                    .isTrue();
            assertThat(dailySequences.add(matcher.group(1) + "_" + matcher.group(2)))
                    .as("同一 archetype 的日期加序列号必须全局唯一：%s", fileName)
                    .isTrue();

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
