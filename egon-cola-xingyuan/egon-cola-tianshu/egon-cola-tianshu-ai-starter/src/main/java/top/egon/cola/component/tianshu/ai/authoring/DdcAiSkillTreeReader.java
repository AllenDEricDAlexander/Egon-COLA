package top.egon.cola.component.tianshu.ai.authoring;

import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeTypeEnum;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

/**
 * 把创作目录读成 Skill 节点树：忽略隐藏项（含 .tianshu-*），拒绝符号链接、非 UTF-8 与超出服务端限额的内容。
 * / Reads an authoring directory into a Skill node tree: hidden entries (including .tianshu-*) are ignored, and
 * symlinks, non-UTF-8 content and anything beyond the server limits are rejected.
 */
@Slf4j
public class DdcAiSkillTreeReader {

    static final int MAX_NODES = 257;
    static final int MAX_FILE_BYTES = 262_144;
    static final long MAX_TOTAL_BYTES = 1_048_576L;
    static final int MAX_DEPTH = 16;
    static final int MAX_PATH_BYTES = 1024;
    static final String SKILL_MD = "SKILL.md";
    private static final int MAX_DISPLAY_NAME = 128;

    public DdcAiSkillTreeSnapshotDTO read(Path directory) throws IOException {
        Path root = directory.toAbsolutePath().normalize();
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("AUTHORING_DIRECTORY_MISSING:" + directory);
        }
        List<Path> paths;
        try (Stream<Path> walk = Files.walk(root)) {
            paths = walk.filter(path -> !path.equals(root)).filter(path -> !hidden(root, path)).toList();
        }
        List<String> relativePaths = new ArrayList<>(paths.stream().map(path -> relative(root, path)).toList());
        relativePaths.sort((left, right) -> Arrays.compareUnsigned(utf8(left), utf8(right)));
        if (relativePaths.size() + 1 > MAX_NODES) throw new IllegalArgumentException("LIMIT:NODES");

        List<DdcAiNodeDTO> nodes = new ArrayList<>();
        nodes.add(DdcAiNodeDTO.builder().path("").nodeType(DdcAiNodeTypeEnum.DIRECTORY).build());
        MessageDigest digest = sha256();
        long total = 0;
        String displayName = null;
        for (String relative : relativePaths) {
            Path path = root.resolve(relative);
            if (utf8(relative).length > MAX_PATH_BYTES || relative.split("/").length > MAX_DEPTH) {
                throw new IllegalArgumentException("LIMIT:PATH:" + relative);
            }
            if (Files.isSymbolicLink(path)) throw new IllegalArgumentException("SYMLINK:" + relative);
            if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                nodes.add(DdcAiNodeDTO.builder().path(relative).nodeType(DdcAiNodeTypeEnum.DIRECTORY).build());
                update(digest, relative, null);
                continue;
            }
            if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalArgumentException("UNSUPPORTED_FILE:" + relative);
            }
            byte[] bytes = Files.readAllBytes(path);
            if (bytes.length > MAX_FILE_BYTES) throw new IllegalArgumentException("LIMIT:FILE:" + relative);
            total += bytes.length;
            if (total > MAX_TOTAL_BYTES) throw new IllegalArgumentException("LIMIT:TOTAL");
            String content = decodeStrictUtf8(bytes, relative);
            nodes.add(DdcAiNodeDTO.builder().path(relative).nodeType(DdcAiNodeTypeEnum.FILE).content(content).build());
            update(digest, relative, bytes);
            if (SKILL_MD.equals(relative)) displayName = displayName(content);
        }
        if (displayName == null && relativePaths.stream().noneMatch(SKILL_MD::equals)) {
            throw new IllegalArgumentException("ROOT_SKILL_MD_REQUIRED");
        }
        return new DdcAiSkillTreeSnapshotDTO(nodes, HexFormat.of().formatHex(digest.digest()), displayName);
    }

    private static boolean hidden(Path root, Path path) {
        for (Path segment : root.relativize(path)) {
            if (segment.toString().startsWith(".")) return true;
        }
        return false;
    }

    private static String relative(Path root, Path path) {
        return root.relativize(path).toString().replace('\\', '/');
    }

    private static void update(MessageDigest digest, String path, byte[] content) {
        digest.update(utf8(path));
        digest.update((byte) 0);
        if (content == null) {
            digest.update((byte) 'D');
        } else {
            digest.update((byte) 'F');
            digest.update(content);
        }
        digest.update((byte) 0);
    }

    private static String decodeStrictUtf8(byte[] bytes, String path) {
        try {
            String content = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
            if (content.indexOf('\u0000') >= 0) throw new IllegalArgumentException("NUL_CHARACTER:" + path);
            return content;
        } catch (CharacterCodingException invalid) {
            throw new IllegalArgumentException("NON_UTF8:" + path, invalid);
        }
    }

    /** SKILL.md 首个非空行去掉 Markdown 标题符号。 / First non-blank SKILL.md line without heading marks. */
    static String displayName(String skillMarkdown) {
        for (String line : skillMarkdown.split("\\R")) {
            String candidate = line.replaceFirst("^#+\\s*", "").strip();
            if (candidate.isEmpty()) continue;
            candidate = candidate.chars().anyMatch(Character::isISOControl)
                    ? candidate.replaceAll("\\p{Cntrl}", " ").strip() : candidate;
            return candidate.codePointCount(0, candidate.length()) <= MAX_DISPLAY_NAME ? candidate
                    : candidate.substring(0, candidate.offsetByCodePoints(0, MAX_DISPLAY_NAME));
        }
        return null;
    }

    private static byte[] utf8(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("SHA-256 is unavailable", unavailable);
        }
    }
}
