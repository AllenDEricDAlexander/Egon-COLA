package top.egon.cola.component.gateway.admin.mcp.artifact;

import top.egon.cola.component.gateway.core.mcp.app.McpAppArtifactStore;
import top.egon.cola.component.gateway.core.mcp.app.McpAppArtifactStore.ArtifactContent;
import top.egon.cola.component.gateway.core.mcp.app.McpAppArtifactStore.ReadRequest;
import top.egon.cola.component.gateway.core.mcp.app.McpAppArtifactStore.StoredArtifact;
import top.egon.cola.component.gateway.core.mcp.app.McpAppArtifactStore.WriteRequest;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Filesystem adapter that atomically publishes immutable MCP App artifacts.
 * 补充说明 / Supplementary summary: {@code FileSystemMcpAppArtifactStore} 是存储组件，位于当前 Gateway 模块的相关包中，负责FileSystemMCPApp制品存储相关的职责与边界。
 * English supplement: {@code FileSystemMcpAppArtifactStore} is a file system mcp app artifact store store in the current Gateway module; it owns the file system mcp app artifact store-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class FileSystemMcpAppArtifactStore
        implements McpAppArtifactStore.Writer, McpAppArtifactStore.Reader {

    /**
     * 中文说明：表示 ENTRYFILE 这一固定值；它属于 {@code FileSystemMcpAppArtifactStore} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value entry file; it is a state, type, or protocol value of {@code FileSystemMcpAppArtifactStore} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code FileSystemMcpAppArtifactStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code FileSystemMcpAppArtifactStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final String ENTRY_FILE = "index.html";

    /**
     * 中文说明：保存 root 对应的状态、依赖或配置值；字段类型为 {@code Path}，由 {@code FileSystemMcpAppArtifactStore} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by root; its type is {@code Path}, and {@code FileSystemMcpAppArtifactStore} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code FileSystemMcpAppArtifactStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code FileSystemMcpAppArtifactStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Path root;

    /**
     * 中文说明：创建 {@code FileSystemMcpAppArtifactStore} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code FileSystemMcpAppArtifactStore} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param root 参数 root；parameter root。
     */
    public FileSystemMcpAppArtifactStore(Path root) {
        this.root = prepareRoot(root);
    }

    /**
     * 中文说明：执行 write 操作；该方法是 {@code FileSystemMcpAppArtifactStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the write operation; this method is the invocation entry point on {@code FileSystemMcpAppArtifactStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code FileSystemMcpAppArtifactStore.write(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 write 的处理结果；returns the result of the operation.
     */
    @Override
    public StoredArtifact write(WriteRequest request) {
        byte[] content = request.content();
        String actualDigest = sha256(content);
        if (!actualDigest.equals(request.expectedSha256())) {
            throw rejected("MCP App upload SHA-256 does not match");
        }
        validateSegment(request.appCode(), "appCode");
        validateSegment(request.version(), "version");
        String reference = "apps/" + request.appCode() + "/"
                + request.version() + "/" + ENTRY_FILE;
        Path directory = ensureDirectories(
                "apps",
                request.appCode(),
                request.version()
        );
        Path target = contained(directory.resolve(ENTRY_FILE));
        Path lockPath = contained(directory.resolve(".upload.lock"));
        try (FileChannel lockChannel = FileChannel.open(
                lockPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE
        ); FileLock ignored = lockChannel.lock()) {
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                return existing(target, reference, actualDigest, content.length);
            }
            Path temporary = Files.createTempFile(
                    directory,
                    ".index.html-",
                    ".tmp"
            );
            try {
                writeAndForce(temporary, content);
                if (!actualDigest.equals(sha256(readRegular(temporary)))) {
                    throw rejected("MCP App upload verification failed");
                }
                try {
                    Files.move(
                            temporary,
                            target,
                            StandardCopyOption.ATOMIC_MOVE
                    );
                } catch (FileAlreadyExistsException race) {
                    return existing(
                            target,
                            reference,
                            actualDigest,
                            content.length
                    );
                }
                forceDirectory(directory);
                return new StoredArtifact(
                        reference,
                        actualDigest,
                        content.length
                );
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (McpAppArtifactStore.ArtifactConflictException
                 | McpAppArtifactStore.ArtifactRejectedException failure) {
            throw failure;
        } catch (IOException failure) {
            throw rejected("MCP App artifact could not be stored", failure);
        }
    }

    /**
     * 中文说明：执行 read 操作；该方法是 {@code FileSystemMcpAppArtifactStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the read operation; this method is the invocation entry point on {@code FileSystemMcpAppArtifactStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code FileSystemMcpAppArtifactStore.read(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 read 的处理结果；returns the result of the operation.
     */
    @Override
    public ArtifactContent read(ReadRequest request) {
        Path target = resolveReference(request.artifactReference());
        byte[] content = readRegular(target);
        String actualDigest = sha256(content);
        if (!actualDigest.equals(request.expectedSha256())
                || content.length != request.expectedSizeBytes()) {
            throw rejected("MCP App artifact metadata does not match storage");
        }
        return new ArtifactContent(
                content,
                actualDigest,
                content.length
        );
    }

    /**
     * 中文说明：执行 existing 操作；该方法是 {@code FileSystemMcpAppArtifactStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the existing operation; this method is the invocation entry point on {@code FileSystemMcpAppArtifactStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code FileSystemMcpAppArtifactStore.existing(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param target 参数 target；parameter target。
     * @param reference 参数 reference；parameter reference。
     * @param expectedDigest 参数 expectedDigest；parameter expected digest。
     * @param expectedSize 参数 expectedSize；parameter expected size。
     * @return 返回 existing 的处理结果；returns the result of the operation.
     */
    private StoredArtifact existing(
            Path target,
            String reference,
            String expectedDigest,
            long expectedSize) {
        byte[] content = readRegular(target);
        String storedDigest = sha256(content);
        if (!storedDigest.equals(expectedDigest)
                || content.length != expectedSize) {
            throw new McpAppArtifactStore.ArtifactConflictException(
                    "MCP App artifact version is immutable"
            );
        }
        return new StoredArtifact(reference, storedDigest, content.length);
    }

    /**
     * 中文说明：执行 resolveReference 操作；该方法是 {@code FileSystemMcpAppArtifactStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the resolve reference operation; this method is the invocation entry point on {@code FileSystemMcpAppArtifactStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code FileSystemMcpAppArtifactStore.resolveReference(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param reference 参数 reference；parameter reference。
     * @return 返回 resolveReference 的处理结果；returns the result of the operation.
     */
    private Path resolveReference(String reference) {
        if (reference.indexOf('\\') >= 0) {
            throw rejected("MCP App artifact reference is invalid");
        }
        Path relative;
        try {
            relative = Path.of(reference);
        } catch (RuntimeException failure) {
            throw rejected("MCP App artifact reference is invalid", failure);
        }
        if (relative.isAbsolute()
                || relative.getNameCount() != 4
                || !"apps".equals(relative.getName(0).toString())
                || !ENTRY_FILE.equals(relative.getName(3).toString())) {
            throw rejected("MCP App artifact reference is invalid");
        }
        validateSegment(relative.getName(1).toString(), "appCode");
        validateSegment(relative.getName(2).toString(), "version");
        Path target = contained(root.resolve(relative));
        verifyExistingPath(target);
        return target;
    }

    /**
     * 中文说明：执行 ensureDirectories 操作；该方法是 {@code FileSystemMcpAppArtifactStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the ensure directories operation; this method is the invocation entry point on {@code FileSystemMcpAppArtifactStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code FileSystemMcpAppArtifactStore.ensureDirectories(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param segments 参数 segments；parameter segments。
     * @return 返回 ensureDirectories 的处理结果；returns the result of the operation.
     */
    private Path ensureDirectories(String... segments) {
        Path current = root;
        for (String segment : segments) {
            validateSegment(segment, "path segment");
            current = contained(current.resolve(segment));
            try {
                Files.createDirectory(current);
            } catch (FileAlreadyExistsException ignored) {
                // The path is verified below without following symbolic links.
            } catch (IOException failure) {
                throw rejected(
                        "MCP App artifact directory could not be created",
                        failure
                );
            }
            if (Files.isSymbolicLink(current)
                    || !Files.isDirectory(
                    current,
                    LinkOption.NOFOLLOW_LINKS
            )) {
                throw rejected("MCP App artifact path is not a directory");
            }
        }
        return current;
    }

    /**
     * 中文说明：执行 verifyExistingPath 操作；该方法是 {@code FileSystemMcpAppArtifactStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the verify existing path operation; this method is the invocation entry point on {@code FileSystemMcpAppArtifactStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code FileSystemMcpAppArtifactStore.verifyExistingPath(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param target 参数 target；parameter target。
     */
    private void verifyExistingPath(Path target) {
        Path current = root;
        for (Path segment : root.relativize(target)) {
            current = current.resolve(segment);
            if (Files.isSymbolicLink(current)) {
                throw rejected("MCP App artifact symlink is forbidden");
            }
        }
        if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
            throw rejected("MCP App artifact was not found");
        }
    }

    /**
     * 中文说明：执行 readRegular 操作；该方法是 {@code FileSystemMcpAppArtifactStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the read regular operation; this method is the invocation entry point on {@code FileSystemMcpAppArtifactStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code FileSystemMcpAppArtifactStore.readRegular(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param path 参数 path；parameter path。
     * @return 返回 readRegular 的处理结果；returns the result of the operation.
     */
    private byte[] readRegular(Path path) {
        verifyExistingPath(path);
        try (FileChannel channel = FileChannel.open(
                path,
                StandardOpenOption.READ,
                LinkOption.NOFOLLOW_LINKS
        )) {
            long size = channel.size();
            if (size < 1L || size > McpAppArtifactStore.MAX_ARTIFACT_BYTES) {
                throw rejected("MCP App artifact size is invalid");
            }
            ByteBuffer buffer = ByteBuffer.allocate(Math.toIntExact(size));
            while (buffer.hasRemaining() && channel.read(buffer) >= 0) {
                // Continue until the bounded artifact is fully read.
            }
            if (buffer.hasRemaining() || channel.read(ByteBuffer.allocate(1)) >= 0) {
                throw rejected("MCP App artifact changed while being read");
            }
            return buffer.array();
        } catch (McpAppArtifactStore.ArtifactRejectedException failure) {
            throw failure;
        } catch (IOException failure) {
            throw rejected("MCP App artifact could not be read", failure);
        }
    }

    /**
     * 中文说明：执行 writeAndForce 操作；该方法是 {@code FileSystemMcpAppArtifactStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the write and force operation; this method is the invocation entry point on {@code FileSystemMcpAppArtifactStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code FileSystemMcpAppArtifactStore.writeAndForce(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param target 参数 target；parameter target。
     * @param content 参数 content；parameter content。
     */
    private void writeAndForce(Path target, byte[] content)
            throws IOException {
        try (FileChannel channel = FileChannel.open(
                target,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING,
                LinkOption.NOFOLLOW_LINKS
        )) {
            ByteBuffer buffer = ByteBuffer.wrap(content);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
        }
    }

    /**
     * 中文说明：执行 forceDirectory 操作；该方法是 {@code FileSystemMcpAppArtifactStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the force directory operation; this method is the invocation entry point on {@code FileSystemMcpAppArtifactStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code FileSystemMcpAppArtifactStore.forceDirectory(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param directory 参数 directory；parameter directory。
     */
    private void forceDirectory(Path directory) throws IOException {
        try (FileChannel channel = FileChannel.open(
                directory,
                StandardOpenOption.READ
        )) {
            channel.force(true);
        }
    }

    /**
     * 中文说明：执行 contained 操作；该方法是 {@code FileSystemMcpAppArtifactStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the contained operation; this method is the invocation entry point on {@code FileSystemMcpAppArtifactStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code FileSystemMcpAppArtifactStore.contained(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 contained 的处理结果；returns the result of the operation.
     */
    private Path contained(Path value) {
        Path normalized = value.toAbsolutePath().normalize();
        if (!normalized.startsWith(root)) {
            throw rejected("MCP App artifact path escapes its root");
        }
        return normalized;
    }

    /**
     * 中文说明：执行 prepareRoot 操作；该方法是 {@code FileSystemMcpAppArtifactStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the prepare root operation; this method is the invocation entry point on {@code FileSystemMcpAppArtifactStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code FileSystemMcpAppArtifactStore.prepareRoot(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 prepareRoot 的处理结果；returns the result of the operation.
     */
    private static Path prepareRoot(Path value) {
        if (value == null) {
            throw rejected("MCP App artifact root is required");
        }
        Path normalized = value.toAbsolutePath().normalize();
        try {
            Files.createDirectories(normalized);
            if (Files.isSymbolicLink(normalized)
                    || !Files.isDirectory(
                    normalized,
                    LinkOption.NOFOLLOW_LINKS
            )) {
                throw rejected("MCP App artifact root is invalid");
            }
            return normalized.toRealPath();
        } catch (McpAppArtifactStore.ArtifactRejectedException failure) {
            throw failure;
        } catch (IOException failure) {
            throw rejected("MCP App artifact root is unavailable", failure);
        }
    }

    /**
     * 中文说明：执行 validateSegment 操作；该方法是 {@code FileSystemMcpAppArtifactStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate segment operation; this method is the invocation entry point on {@code FileSystemMcpAppArtifactStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code FileSystemMcpAppArtifactStore.validateSegment(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     */
    private static void validateSegment(String value, String field) {
        if (value == null
                || !value.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}")
                || ".".equals(value)
                || "..".equals(value)) {
            throw rejected("MCP App artifact " + field + " is invalid");
        }
    }

    /**
     * 中文说明：执行 sha256 操作；该方法是 {@code FileSystemMcpAppArtifactStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the sha256 operation; this method is the invocation entry point on {@code FileSystemMcpAppArtifactStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code FileSystemMcpAppArtifactStore.sha256(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param content 参数 content；parameter content。
     * @return 返回 sha256 的处理结果；returns the result of the operation.
     */
    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(content)
            );
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 is unavailable", failure);
        }
    }

    /**
     * 中文说明：执行 rejected 操作；该方法是 {@code FileSystemMcpAppArtifactStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the rejected operation; this method is the invocation entry point on {@code FileSystemMcpAppArtifactStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code FileSystemMcpAppArtifactStore.rejected(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param message 参数 消息；parameter message。
     * @return 返回 rejected 的处理结果；returns the result of the operation.
     */
    private static McpAppArtifactStore.ArtifactRejectedException rejected(
            String message) {
        return new McpAppArtifactStore.ArtifactRejectedException(message);
    }

    /**
     * 中文说明：执行 rejected 操作；该方法是 {@code FileSystemMcpAppArtifactStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the rejected operation; this method is the invocation entry point on {@code FileSystemMcpAppArtifactStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code FileSystemMcpAppArtifactStore.rejected(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param message 参数 消息；parameter message。
     * @param cause 参数 cause；parameter cause。
     * @return 返回 rejected 的处理结果；returns the result of the operation.
     */
    private static McpAppArtifactStore.ArtifactRejectedException rejected(
            String message,
            Throwable cause) {
        return new McpAppArtifactStore.ArtifactRejectedException(
                message,
                cause
        );
    }
}
