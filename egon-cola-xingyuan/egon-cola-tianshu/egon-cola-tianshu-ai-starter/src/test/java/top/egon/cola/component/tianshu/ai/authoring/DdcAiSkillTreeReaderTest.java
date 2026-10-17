package top.egon.cola.component.tianshu.ai.authoring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeTypeEnum;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcAiSkillTreeReaderTest {

    private final DdcAiSkillTreeReader reader = new DdcAiSkillTreeReader();

    @Test
    void producesRootDirectoryFirstAndIgnoresHiddenFilesAndDirectories(@TempDir Path dir) throws IOException {
        write(dir, "SKILL.md", "# Refund helper\nbody");
        write(dir, "docs/guide.md", "guide");
        write(dir, ".git/config", "secret");
        write(dir, ".tianshu-authoring-state.json", "{}");
        write(dir, "docs/.draft.md", "hidden");

        DdcAiSkillTreeSnapshotDTO snapshot = reader.read(dir);

        assertThat(snapshot.nodes()).extracting(DdcAiNodeDTO::getPath)
                .containsExactly("", "SKILL.md", "docs", "docs/guide.md");
        assertThat(snapshot.nodes().get(0).getNodeType()).isEqualTo(DdcAiNodeTypeEnum.DIRECTORY);
        assertThat(snapshot.nodes().get(1).getContent()).isEqualTo("# Refund helper\nbody");
        assertThat(snapshot.displayName()).isEqualTo("Refund helper");
        assertThat(snapshot.sha256()).matches("^[0-9a-f]{64}$");
    }

    @Test
    void rejectsMoreThan256NodesOrFileOver256KiBOrMissingSkillMd(@TempDir Path dir) throws IOException {
        write(dir, "SKILL.md", "# a");
        write(dir, "big.bin.md", "x".repeat(262_145));
        assertThatThrownBy(() -> reader.read(dir)).hasMessageStartingWith("LIMIT:FILE");
        Files.delete(dir.resolve("big.bin.md"));

        for (int index = 0; index < 256; index++) write(dir, "f" + index + ".md", "x");
        assertThatThrownBy(() -> reader.read(dir)).hasMessage("LIMIT:NODES");

        Path other = Files.createDirectory(dir.resolveSibling(dir.getFileName() + "-other"));
        write(other, "README.md", "no skill");
        assertThatThrownBy(() -> reader.read(other)).hasMessage("ROOT_SKILL_MD_REQUIRED");
    }

    @Test
    void rejectsNonUtf8Content(@TempDir Path dir) throws IOException {
        write(dir, "SKILL.md", "# a");
        Files.write(dir.resolve("latin1.md"), new byte[]{(byte) 0xC3, (byte) 0x28});

        assertThatThrownBy(() -> reader.read(dir)).hasMessage("NON_UTF8:latin1.md");
    }

    @Test
    void producesStableSha256OverSortedPaths(@TempDir Path first, @TempDir Path second) throws IOException {
        write(first, "SKILL.md", "# a");
        write(first, "b/z.md", "z");
        write(first, "a.md", "a");
        write(second, "a.md", "a");
        write(second, "b/z.md", "z");
        write(second, "SKILL.md", "# a");

        String digest = reader.read(first).sha256();
        assertThat(reader.read(second).sha256()).isEqualTo(digest);

        write(second, "a.md", "changed");
        assertThat(reader.read(second).sha256()).isNotEqualTo(digest);
    }

    static void write(Path dir, String relative, String content) throws IOException {
        Path file = dir.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }
}
