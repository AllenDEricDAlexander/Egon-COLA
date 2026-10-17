package top.egon.cola.component.tianshu.ai.workspace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeTypeEnum;
import top.egon.cola.component.tianshu.model.ai.DdcSkillResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcAiSkillMaterializerTest {

    static final Instant NOW = Instant.parse("2026-10-17T07:00:00Z");

    private final DdcAiSkillMaterializer materializer = materializer();

    static DdcAiSkillMaterializer materializer() {
        return new DdcAiSkillMaterializer(new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void writesVersionDirectoryAndAtomicallySwitchesCurrentMarker(@TempDir Path root) throws IOException {
        Path v2 = materializer.materialize(root, "ns-a", "skill-x", skill(2L, "a", "SKILL.md", "# v2"));

        assertThat(v2).isEqualTo(root.toAbsolutePath().normalize().resolve("ns-a/skill-x/versions/v2"));
        assertThat(Files.readString(v2.resolve("SKILL.md"))).isEqualTo("# v2");
        assertThat(Files.readString(v2.resolve("docs/guide.md"))).isEqualTo("guide");
        assertThat(Files.readString(root.resolve("ns-a/skill-x/current"))).isEqualTo("v2");
        assertThat(materializer.currentDirectory(root, "ns-a", "skill-x")).contains(v2);

        Path v3 = materializer.materialize(root, "ns-a", "skill-x", skill(3L, "b", "SKILL.md", "# v3"));

        assertThat(materializer.currentDirectory(root, "ns-a", "skill-x")).contains(v3);
        assertThat(Files.readString(v2.resolve("SKILL.md"))).isEqualTo("# v2");
        try (var entries = Files.list(root.resolve("ns-a/skill-x/versions"))) {
            assertThat(entries.map(path -> path.getFileName().toString()))
                    .noneMatch(name -> name.startsWith(".tmp-") || name.startsWith(".stale-"));
        }
    }

    @Test
    void failureKeepsPreviousCurrent(@TempDir Path root) throws IOException {
        materializer.materialize(root, "ns-a", "skill-x", skill(2L, "a", "SKILL.md", "# v2"));

        assertThatThrownBy(() -> materializer.materialize(root, "ns-a", "skill-x",
                skill(3L, "b", "../evil", "x"))).isInstanceOf(IllegalArgumentException.class);

        assertThat(Files.readString(root.resolve("ns-a/skill-x/current"))).isEqualTo("v2");
        assertThat(root.resolve("ns-a/skill-x/versions/v3")).doesNotExist();
        assertThat(root.resolve("evil")).doesNotExist();
        assertThat(materializer.readState(root, "ns-a", "skill-x").orElseThrow().versionNo()).isEqualTo(2L);
    }

    @Test
    void rejectsPathTraversalAndAbsolutePaths(@TempDir Path root) {
        assertThatThrownBy(() -> materializer.materialize(root, "..", "skill-x", skill(1L, "a", "SKILL.md", "x")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> materializer.materialize(root, "ns-a", "a/b", skill(1L, "a", "SKILL.md", "x")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> materializer.materialize(root, "ns-a", "skill-x", skill(1L, "a", "/etc/passwd", "x")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> materializer.materialize(root, "ns-a", "skill-x", skill(1L, "a", "a\\b", "x")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(DdcAiSkillMaterializer.safeResolve(root, "")).isEqualTo(root);
    }

    @Test
    void stateFileRecordsVersionNoAndShaAndRematerializingTheSameVersionIsIdempotent(@TempDir Path root)
            throws IOException {
        materializer.materialize(root, "ns-a", "skill-x", skill(4L, "c", "SKILL.md", "# v4"));
        materializer.materialize(root, "ns-a", "skill-x", skill(4L, "c", "SKILL.md", "# v4"));

        DdcAiSkillWorkspaceStateDTO state = materializer.readState(root, "ns-a", "skill-x").orElseThrow();
        assertThat(state.versionNo()).isEqualTo(4L);
        assertThat(state.contentSha256()).isEqualTo("c".repeat(64));
        assertThat(state.resourceId()).isEqualTo(7L);
        assertThat(state.updatedAt()).isEqualTo(NOW);
        assertThat(Files.readString(root.resolve("ns-a/skill-x/state.json"))).contains("\"2026-10-17T07:00:00Z\"");

        Files.writeString(root.resolve("ns-a/skill-x/state.json"), "{broken");
        assertThat(materializer.readState(root, "ns-a", "skill-x")).isEmpty();
    }

    static DdcSkillResult skill(long versionNo, String shaChar, String filePath, String content) {
        List<DdcAiNodeDTO> files = new ArrayList<>();
        files.add(DdcAiNodeDTO.builder().path("").nodeType(DdcAiNodeTypeEnum.DIRECTORY).build());
        files.add(DdcAiNodeDTO.builder().path(filePath).nodeType(DdcAiNodeTypeEnum.FILE).content(content).build());
        files.add(DdcAiNodeDTO.builder().path("docs").nodeType(DdcAiNodeTypeEnum.DIRECTORY).build());
        files.add(DdcAiNodeDTO.builder().path("docs/guide.md").nodeType(DdcAiNodeTypeEnum.FILE).content("guide")
                .build());
        return DdcSkillResult.builder().resourceId(7L).resourceKey("skill-x").versionId(100L + versionNo)
                .versionNo(versionNo).contentSha256(shaChar.repeat(64)).files(files).build();
    }
}
