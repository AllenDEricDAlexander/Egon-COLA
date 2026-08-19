package top.egon.cola.component.gateway.engine.mcp.adapter;

import top.egon.cola.component.gateway.core.mcp.app.McpAppArtifactStore;
import top.egon.cola.component.gateway.core.mcp.app.McpAppArtifactStore.ArtifactContent;
import top.egon.cola.component.gateway.core.mcp.app.McpAppArtifactStore.ReadRequest;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Read-only data-plane adapter for immutable MCP App artifacts.
 * 补充说明 / Supplementary summary: {@code FileSystemMcpAppArtifactReader} 是类型，位于当前 Gateway 模块的相关包中，负责FileSystemMCPApp制品Reader相关的职责与边界。
 * English supplement: {@code FileSystemMcpAppArtifactReader} is a type in the current Gateway module; it owns the file system mcp app artifact reader-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class FileSystemMcpAppArtifactReader
        implements McpAppArtifactStore.Reader {

    /**
     * 中文说明：保存 root 对应的状态、依赖或配置值；字段类型为 {@code Path}，由 {@code FileSystemMcpAppArtifactReader} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by root; its type is {@code Path}, and {@code FileSystemMcpAppArtifactReader} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code FileSystemMcpAppArtifactReader} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code FileSystemMcpAppArtifactReader}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Path root;

    /**
     * 中文说明：创建 {@code FileSystemMcpAppArtifactReader} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code FileSystemMcpAppArtifactReader} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param root 参数 root；parameter root。
     */
    public FileSystemMcpAppArtifactReader(Path root) {
        if (root == null) {
            throw rejected("MCP App artifact root is required");
        }
        Path normalized = root.toAbsolutePath().normalize();
        try {
            Files.createDirectories(normalized);
            if (Files.isSymbolicLink(normalized)) {
                throw rejected("MCP App artifact root cannot be a symlink");
            }
            this.root = normalized.toRealPath();
        } catch (McpAppArtifactStore.ArtifactRejectedException failure) {
            throw failure;
        } catch (IOException failure) {
            throw rejected("MCP App artifact root is unavailable", failure);
        }
    }

    /**
     * 中文说明：执行 read 操作；该方法是 {@code FileSystemMcpAppArtifactReader} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the read operation; this method is the invocation entry point on {@code FileSystemMcpAppArtifactReader} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code FileSystemMcpAppArtifactReader.read(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 read 的处理结果；returns the result of the operation.
     */
    @Override
    public ArtifactContent read(ReadRequest request) {
        Path target = resolve(request.artifactReference());
        byte[] content = readRegular(target);
        String digest = sha256(content);
        if (!digest.equals(request.expectedSha256())
                || content.length != request.expectedSizeBytes()) {
            throw rejected("MCP App artifact metadata does not match storage");
        }
        return new ArtifactContent(content, digest, content.length);
    }

    /**
     * 中文说明：执行 resolve 操作；该方法是 {@code FileSystemMcpAppArtifactReader} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the resolve operation; this method is the invocation entry point on {@code FileSystemMcpAppArtifactReader} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code FileSystemMcpAppArtifactReader.resolve(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param reference 参数 reference；parameter reference。
     * @return 返回 resolve 的处理结果；returns the result of the operation.
     */
    private Path resolve(String reference) {
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
                || !"index.html".equals(relative.getName(3).toString())) {
            throw rejected("MCP App artifact reference is invalid");
        }
        Path target = root.resolve(relative).toAbsolutePath().normalize();
        if (!target.startsWith(root)) {
            throw rejected("MCP App artifact path escapes its root");
        }
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
        return target;
    }

    /**
     * 中文说明：执行 readRegular 操作；该方法是 {@code FileSystemMcpAppArtifactReader} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the read regular operation; this method is the invocation entry point on {@code FileSystemMcpAppArtifactReader} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code FileSystemMcpAppArtifactReader.readRegular(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param path 参数 path；parameter path。
     * @return 返回 readRegular 的处理结果；returns the result of the operation.
     */
    private byte[] readRegular(Path path) {
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
     * 中文说明：执行 sha256 操作；该方法是 {@code FileSystemMcpAppArtifactReader} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the sha256 operation; this method is the invocation entry point on {@code FileSystemMcpAppArtifactReader} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code FileSystemMcpAppArtifactReader.sha256(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param content 参数 content；parameter content。
     * @return 返回 sha256 的处理结果；returns the result of the operation.
     */
    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(content)
            );
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 is unavailable", failure);
        }
    }

    /**
     * 中文说明：执行 rejected 操作；该方法是 {@code FileSystemMcpAppArtifactReader} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the rejected operation; this method is the invocation entry point on {@code FileSystemMcpAppArtifactReader} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code FileSystemMcpAppArtifactReader.rejected(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param message 参数 消息；parameter message。
     * @return 返回 rejected 的处理结果；returns the result of the operation.
     */
    private static McpAppArtifactStore.ArtifactRejectedException rejected(
            String message) {
        return new McpAppArtifactStore.ArtifactRejectedException(message);
    }

    /**
     * 中文说明：执行 rejected 操作；该方法是 {@code FileSystemMcpAppArtifactReader} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the rejected operation; this method is the invocation entry point on {@code FileSystemMcpAppArtifactReader} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code FileSystemMcpAppArtifactReader.rejected(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
