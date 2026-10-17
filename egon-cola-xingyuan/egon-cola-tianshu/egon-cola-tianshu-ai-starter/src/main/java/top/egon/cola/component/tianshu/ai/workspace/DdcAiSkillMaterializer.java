package top.egon.cola.component.tianshu.ai.workspace;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeTypeEnum;
import top.egon.cola.component.tianshu.model.ai.DdcSkillResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 原子落盘已发布 Skill 树：先写临时目录再原子改名为 versions/v{n}，最后替换 current 标记与 state.json。
 * 任何失败都会清理临时目录并保留旧的 current。
 * / Atomically materializes a published Skill tree: it writes a temp directory, renames it to versions/v{n}, then
 * replaces the current marker and state.json. Any failure removes the temp directory and keeps the previous current.
 */
@Slf4j
public class DdcAiSkillMaterializer {

    static final String VERSIONS = "versions";
    static final String CURRENT = "current";
    static final String STATE = "state.json";
    private static final Pattern VERSION_NAME = Pattern.compile("^v[1-9][0-9]{0,18}$");

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public DdcAiSkillMaterializer(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** 落盘并切换 current，返回版本目录。 / Materializes and switches current, returning the version directory. */
    public Path materialize(Path root, String namespaceCode, String resourceKey, DdcSkillResult skill)
            throws IOException {
        if (skill == null || skill.getVersionNo() == null || skill.getVersionNo() <= 0 || skill.getFiles() == null
                || skill.getContentSha256() == null || skill.getResourceId() == null) {
            throw new IllegalArgumentException("A complete published Skill is required");
        }
        Path resourceDir = resourceDirectory(root, namespaceCode, resourceKey);
        Path versions = resourceDir.resolve(VERSIONS);
        Files.createDirectories(versions);
        String name = "v" + skill.getVersionNo();
        Path target = versions.resolve(name);
        Path digestFile = versions.resolve(name + ".sha256");
        if (!(Files.isDirectory(target) && skill.getContentSha256().equals(readQuietly(digestFile)))) {
            Path temp = Files.createTempDirectory(versions, ".tmp-" + name + "-");
            try {
                writeTree(temp, skill.getFiles());
                if (Files.exists(target)) {
                    Path stale = versions.resolve(".stale-" + name + "-" + UUID.randomUUID());
                    Files.move(target, stale, StandardCopyOption.ATOMIC_MOVE);
                    deleteRecursively(stale);
                }
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
                writeAtomically(digestFile, skill.getContentSha256());
            } catch (IOException | RuntimeException failure) {
                deleteRecursively(temp);
                throw failure;
            }
        }
        writeAtomically(resourceDir.resolve(CURRENT), name);
        DdcAiSkillWorkspaceStateDTO state = new DdcAiSkillWorkspaceStateDTO(namespaceCode, resourceKey,
                skill.getResourceId(), skill.getVersionNo(), skill.getContentSha256(), clock.instant());
        writeAtomically(resourceDir.resolve(STATE), objectMapper.writeValueAsString(state));
        log.info("AI_WORKSPACE_MATERIALIZED resourceKey={} versionNo={}", resourceKey, skill.getVersionNo());
        return target;
    }

    /** current 标记指向的版本目录。 / Version directory the current marker points to. */
    public Optional<Path> currentDirectory(Path root, String namespaceCode, String resourceKey) {
        Path resourceDir = resourceDirectory(root, namespaceCode, resourceKey);
        String name = readQuietly(resourceDir.resolve(CURRENT));
        if (name == null || !VERSION_NAME.matcher(name).matches()) return Optional.empty();
        Path directory = resourceDir.resolve(VERSIONS).resolve(name);
        return Files.isDirectory(directory) ? Optional.of(directory) : Optional.empty();
    }

    /** 读取 state.json；缺失或损坏时为空。 / Reads state.json; empty when missing or corrupt. */
    public Optional<DdcAiSkillWorkspaceStateDTO> readState(Path root, String namespaceCode, String resourceKey) {
        String json = readQuietly(resourceDirectory(root, namespaceCode, resourceKey).resolve(STATE));
        if (json == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(json, DdcAiSkillWorkspaceStateDTO.class));
        } catch (JsonProcessingException | RuntimeException corrupt) {
            log.warn("Ignoring unreadable AI workspace state for resourceKey={}", resourceKey);
            return Optional.empty();
        }
    }

    Path resourceDirectory(Path root, String namespaceCode, String resourceKey) {
        Objects.requireNonNull(root, "root");
        return safeResolve(safeResolve(root, requireSegment(namespaceCode)), requireSegment(resourceKey));
    }

    /** 解析相对路径并拒绝越界、绝对路径与控制字符。 / Resolves a relative path, rejecting escapes and control chars. */
    public static Path safeResolve(Path base, String relative) {
        if (relative == null) throw new IllegalArgumentException("AI workspace path is required");
        if (relative.isEmpty()) return base;
        if (relative.startsWith("/") || relative.endsWith("/") || relative.indexOf('\\') >= 0
                || relative.indexOf(':') >= 0 || relative.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Unsafe AI workspace path: " + relative);
        }
        for (String segment : relative.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new IllegalArgumentException("Unsafe AI workspace path: " + relative);
            }
        }
        Path normalizedBase = base.toAbsolutePath().normalize();
        Path resolved = normalizedBase.resolve(relative).normalize();
        if (!resolved.startsWith(normalizedBase) || resolved.equals(normalizedBase)) {
            throw new IllegalArgumentException("Unsafe AI workspace path: " + relative);
        }
        return resolved;
    }

    private static String requireSegment(String value) {
        if (value == null || value.isBlank() || value.indexOf('/') >= 0) {
            throw new IllegalArgumentException("Unsafe AI workspace segment: " + value);
        }
        return value;
    }

    private void writeTree(Path base, List<DdcAiNodeDTO> nodes) throws IOException {
        List<DdcAiNodeDTO> ordered = nodes.stream()
                .sorted(Comparator.comparing(DdcAiNodeDTO::getPath, Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();
        for (DdcAiNodeDTO node : ordered) {
            if (node == null || node.getNodeType() == null) throw new IllegalArgumentException("AI node is invalid");
            Path path = safeResolve(base, node.getPath());
            if (node.getNodeType() == DdcAiNodeTypeEnum.DIRECTORY) {
                Files.createDirectories(path);
            } else {
                if (node.getContent() == null) throw new IllegalArgumentException("AI file content is required");
                Files.createDirectories(path.getParent());
                Files.writeString(path, node.getContent(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW,
                        StandardOpenOption.WRITE);
            }
        }
    }

    private static void writeAtomically(Path target, String content) throws IOException {
        Path temp = target.resolveSibling(target.getFileName() + ".tmp-" + UUID.randomUUID());
        try {
            Files.writeString(temp, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static String readQuietly(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8).trim();
        } catch (NoSuchFileException missing) {
            return null;
        } catch (IOException unreadable) {
            return null;
        }
    }

    static void deleteRecursively(Path directory) {
        if (directory == null || !Files.exists(directory)) return;
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException failure) throws IOException {
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException cleanupFailure) {
            log.warn("Unable to remove AI workspace temp directory {}", directory.getFileName());
        }
    }
}
