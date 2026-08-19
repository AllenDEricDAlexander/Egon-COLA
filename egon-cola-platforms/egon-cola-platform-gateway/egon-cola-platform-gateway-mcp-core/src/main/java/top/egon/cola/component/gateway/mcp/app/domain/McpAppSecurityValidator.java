package top.egon.cola.component.gateway.mcp.app.domain;

import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeApp;
import top.egon.cola.component.gateway.core.mcp.app.McpAppArtifactStore;
import top.egon.cola.component.gateway.mcp.resource.service.McpResourceDriver;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates immutable MCP App manifests and rejects active navigation content.
 * 补充说明 / Supplementary summary: {@code McpAppSecurityValidator} 是校验器，位于当前 Gateway 模块的相关包中，负责MCPApp安全校验器相关的职责与边界。
 * English supplement: {@code McpAppSecurityValidator} is a mcp app security validator validator in the current Gateway module; it owns the mcp app security validator-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class McpAppSecurityValidator {

    /**
     * 中文说明：表示 REQUIREDCSPDIRECTIVES 这一固定值；它属于 {@code McpAppSecurityValidator} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value required csp directives; it is a state, type, or protocol value of {@code McpAppSecurityValidator} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code McpAppSecurityValidator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAppSecurityValidator}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Set<String> REQUIRED_CSP_DIRECTIVES = Set.of(
            "default-src",
            "script-src",
            "connect-src",
            "base-uri",
            "form-action",
            "frame-ancestors"
    );

    /**
     * 中文说明：表示 FORBIDDENHTML 这一固定值；它属于 {@code McpAppSecurityValidator} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value forbidden html; it is a state, type, or protocol value of {@code McpAppSecurityValidator} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code McpAppSecurityValidator} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAppSecurityValidator}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Pattern FORBIDDEN_HTML = Pattern.compile(
            "(?is)(<\\s*(?:base|iframe|object|embed)\\b"
                    + "|<\\s*meta\\b[^>]*http-equiv\\s*=\\s*['\"]?refresh"
                    + "|javascript\\s*:"
                    + "|window\\s*\\.\\s*(?:location|open)"
                    + "|(?:top|parent)\\s*\\.\\s*location"
                    + "|location\\s*\\.\\s*(?:assign|replace)\\s*\\("
                    + "|document\\s*\\.\\s*cookie"
                    + "|(?:local|session)Storage\\b"
                    + "|<\\s*script\\b[^>]*\\bsrc\\s*=\\s*['\"]?"
                    + "(?:https?:)?//)"
    );

    /**
     * 中文说明：执行 validate 操作；该方法是 {@code McpAppSecurityValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate operation; this method is the invocation entry point on {@code McpAppSecurityValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpAppSecurityValidator.validate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param app 参数 app；parameter app。
     * @param artifact 参数 制品；parameter artifact。
     */
    public void validate(
            McpRuntimeApp app,
            McpAppArtifactStore.ArtifactContent artifact) {
        if (app == null || artifact == null) {
            throw rejected("MCP App descriptor and artifact are required");
        }
        validate(new Manifest(
                app.serverCode(),
                app.appCode(),
                app.version(),
                app.resourceUri(),
                app.artifactSha256(),
                app.artifactSizeBytes(),
                app.mimeType(),
                app.contentSecurityPolicy(),
                app.permissions(),
                app.allowedOrigins()
        ), artifact);
    }

    /**
     * 中文说明：执行 validate 操作；该方法是 {@code McpAppSecurityValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate operation; this method is the invocation entry point on {@code McpAppSecurityValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpAppSecurityValidator.validate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param manifest 参数 manifest；parameter manifest。
     * @param artifact 参数 制品；parameter artifact。
     */
    public void validate(
            Manifest manifest,
            McpAppArtifactStore.ArtifactContent artifact) {
        if (manifest == null || artifact == null) {
            throw rejected("MCP App manifest and artifact are required");
        }
        validateManifest(manifest);
        byte[] content = artifact.content();
        String actualSha256 = sha256(content);
        if (!actualSha256.equals(artifact.sha256())
                || !actualSha256.equals(manifest.sha256())) {
            throw rejected("MCP App artifact SHA-256 does not match");
        }
        if (content.length != artifact.sizeBytes()
                || content.length != manifest.sizeBytes()) {
            throw rejected("MCP App artifact size does not match");
        }
        String html = decode(content);
        if (FORBIDDEN_HTML.matcher(html).find()) {
            throw rejected("MCP App contains forbidden navigation content");
        }
    }

    /**
     * 中文说明：执行 validateManifest 操作；该方法是 {@code McpAppSecurityValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate manifest operation; this method is the invocation entry point on {@code McpAppSecurityValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpAppSecurityValidator.validateManifest(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param manifest 参数 manifest；parameter manifest。
     */
    private void validateManifest(Manifest manifest) {
        if (!McpAppArtifactStore.MCP_APP_MIME_TYPE.equals(
                manifest.mimeType()
        )) {
            throw rejected("MCP App MIME type must use the MCP App profile");
        }
        if (manifest.permissions().isEmpty()) {
            throw rejected("MCP App permissions are required");
        }
        validateResourceUri(manifest);
        validateContentSecurityPolicy(manifest);
    }

    /**
     * 中文说明：执行 validate资源Uri 操作；该方法是 {@code McpAppSecurityValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate resource uri operation; this method is the invocation entry point on {@code McpAppSecurityValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpAppSecurityValidator.validateResourceUri(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param manifest 参数 manifest；parameter manifest。
     */
    private void validateResourceUri(Manifest manifest) {
        URI uri;
        try {
            uri = URI.create(manifest.resourceUri());
        } catch (IllegalArgumentException failure) {
            throw rejected("MCP App resource URI is invalid");
        }
        String expectedPath = "/" + manifest.appCode()
                + "/" + manifest.version();
        if (!"ui".equals(uri.getScheme())
                || !manifest.serverCode().equals(uri.getRawAuthority())
                || !expectedPath.equals(uri.getRawPath())
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null
                || uri.getRawUserInfo() != null) {
            throw rejected("MCP App resource URI is not canonical");
        }
    }

    /**
     * 中文说明：执行 validateContent安全策略 操作；该方法是 {@code McpAppSecurityValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate content security policy operation; this method is the invocation entry point on {@code McpAppSecurityValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpAppSecurityValidator.validateContentSecurityPolicy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param manifest 参数 manifest；parameter manifest。
     */
    private void validateContentSecurityPolicy(Manifest manifest) {
        String csp = manifest.contentSecurityPolicy();
        if (csp.indexOf('\r') >= 0 || csp.indexOf('\n') >= 0) {
            throw rejected("MCP App content security policy is invalid");
        }
        Map<String, Set<String>> directives = new HashMap<>();
        for (String statement : csp.split(";")) {
            String trimmed = statement.trim().toLowerCase(Locale.ROOT);
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] values = trimmed.split("\\s+");
            if (directives.putIfAbsent(
                    values[0],
                    Set.copyOf(Arrays.asList(values).subList(1, values.length))
            ) != null) {
                throw rejected("MCP App CSP directive is duplicated");
            }
        }
        if (!directives.keySet().containsAll(REQUIRED_CSP_DIRECTIVES)
                || !Set.of("'none'").equals(directives.get("default-src"))
                || !Set.of("'none'").equals(directives.get("base-uri"))
                || !Set.of("'none'").equals(directives.get("form-action"))
                || !Set.of("'none'").equals(
                directives.get("frame-ancestors")
        )) {
            throw rejected("MCP App CSP does not provide required isolation");
        }
        String normalized = csp.toLowerCase(Locale.ROOT);
        if (normalized.contains("*")
                || normalized.contains("http:")
                || normalized.contains("'unsafe-eval'")
                || normalized.contains("'unsafe-inline'")) {
            throw rejected("MCP App CSP contains a forbidden source");
        }
        Set<String> origins = validateOrigins(manifest.allowedOrigins());
        Set<String> connectSources = directives.get("connect-src");
        if (connectSources == null) {
            throw rejected("MCP App CSP connect-src is required");
        }
        Set<String> declaredOrigins = new HashSet<>();
        for (String source : connectSources) {
            if (source.equals("'self'") || source.equals("'none'")) {
                continue;
            }
            String origin = origin(source);
            if (origin == null || !origins.contains(origin)) {
                throw rejected("MCP App CSP uses a forbidden origin");
            }
            declaredOrigins.add(origin);
        }
        if (!declaredOrigins.containsAll(origins)) {
            throw rejected("MCP App allowed origin is absent from CSP");
        }
    }

    /**
     * 中文说明：执行 validateOrigins 操作；该方法是 {@code McpAppSecurityValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate origins operation; this method is the invocation entry point on {@code McpAppSecurityValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpAppSecurityValidator.validateOrigins(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @return 返回 validateOrigins 的处理结果；returns the result of the operation.
     */
    private Set<String> validateOrigins(Set<String> source) {
        Set<String> result = new HashSet<>();
        for (String value : source) {
            String origin = origin(value);
            if (origin == null || !origin.equals(value.toLowerCase(
                    Locale.ROOT
            ))) {
                throw rejected("MCP App allowed origin is invalid");
            }
            result.add(origin);
        }
        return Set.copyOf(result);
    }

    /**
     * 中文说明：执行 origin 操作；该方法是 {@code McpAppSecurityValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the origin operation; this method is the invocation entry point on {@code McpAppSecurityValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpAppSecurityValidator.origin(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 origin 的处理结果；returns the result of the operation.
     */
    private String origin(String value) {
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null
                    || uri.getRawUserInfo() != null
                    || (uri.getRawPath() != null
                    && !uri.getRawPath().isEmpty())
                    || uri.getRawQuery() != null
                    || uri.getRawFragment() != null) {
                return null;
            }
            return "https://" + uri.getHost().toLowerCase(Locale.ROOT)
                    + (uri.getPort() < 0 ? "" : ":" + uri.getPort());
        } catch (IllegalArgumentException failure) {
            return null;
        }
    }

    /**
     * 中文说明：执行 decode 操作；该方法是 {@code McpAppSecurityValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the decode operation; this method is the invocation entry point on {@code McpAppSecurityValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpAppSecurityValidator.decode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param content 参数 content；parameter content。
     * @return 返回 decode 的处理结果；returns the result of the operation.
     */
    private String decode(byte[] content) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content))
                    .toString();
        } catch (CharacterCodingException failure) {
            throw rejected("MCP App artifact is not valid UTF-8 HTML");
        }
    }

    /**
     * 中文说明：执行 sha256 操作；该方法是 {@code McpAppSecurityValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the sha256 operation; this method is the invocation entry point on {@code McpAppSecurityValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpAppSecurityValidator.sha256(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
     * 中文说明：执行 rejected 操作；该方法是 {@code McpAppSecurityValidator} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the rejected operation; this method is the invocation entry point on {@code McpAppSecurityValidator} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code McpAppSecurityValidator.rejected(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param message 参数 消息；parameter message。
     * @return 返回 rejected 的处理结果；returns the result of the operation.
     */
    private RuntimeException rejected(String message) {
        return McpResourceDriver.rejected(message);
    }

    /**
     * 中文说明：{@code Manifest} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Manifest相关的职责与边界。
     * English summary: {@code Manifest} is an immutable data carrier in the current Gateway module; it owns the manifest-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param appCode 参数 appCode；parameter app code。
     * @param version 参数 version；parameter version。
     * @param resourceUri 参数 资源Uri；parameter resource uri。
     * @param sha256 参数 sha256；parameter sha256。
     * @param sizeBytes 参数 sizeBytes；parameter size bytes。
     * @param mimeType 参数 mimeType；parameter mime type。
     * @param contentSecurityPolicy 参数 content安全策略；parameter content security policy。
     * @param permissions 参数 permissions；parameter permissions。
     * @param allowedOrigins 参数 allowedOrigins；parameter allowed origins。
     */
    public record Manifest(
            /**
             * 中文说明：保存 服务器Code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpAppSecurityValidator.Manifest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by server code; its type is {@code String}, and {@code McpAppSecurityValidator.Manifest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpAppSecurityValidator.Manifest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAppSecurityValidator.Manifest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String serverCode,
            /**
             * 中文说明：保存 appCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpAppSecurityValidator.Manifest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by app code; its type is {@code String}, and {@code McpAppSecurityValidator.Manifest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpAppSecurityValidator.Manifest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAppSecurityValidator.Manifest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String appCode,
            /**
             * 中文说明：保存 version 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpAppSecurityValidator.Manifest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by version; its type is {@code String}, and {@code McpAppSecurityValidator.Manifest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpAppSecurityValidator.Manifest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAppSecurityValidator.Manifest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String version,
            /**
             * 中文说明：保存 资源Uri 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpAppSecurityValidator.Manifest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by resource uri; its type is {@code String}, and {@code McpAppSecurityValidator.Manifest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpAppSecurityValidator.Manifest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAppSecurityValidator.Manifest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String resourceUri,
            /**
             * 中文说明：保存 sha256 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpAppSecurityValidator.Manifest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by sha256; its type is {@code String}, and {@code McpAppSecurityValidator.Manifest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpAppSecurityValidator.Manifest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAppSecurityValidator.Manifest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String sha256,
            /**
             * 中文说明：保存 sizeBytes 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code McpAppSecurityValidator.Manifest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by size bytes; its type is {@code long}, and {@code McpAppSecurityValidator.Manifest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpAppSecurityValidator.Manifest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAppSecurityValidator.Manifest}; do not couple callers to its representation when the owning type exposes an API.
             */
            long sizeBytes,
            /**
             * 中文说明：保存 mimeType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpAppSecurityValidator.Manifest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by mime type; its type is {@code String}, and {@code McpAppSecurityValidator.Manifest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpAppSecurityValidator.Manifest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAppSecurityValidator.Manifest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String mimeType,
            /**
             * 中文说明：保存 content安全策略 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code McpAppSecurityValidator.Manifest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by content security policy; its type is {@code String}, and {@code McpAppSecurityValidator.Manifest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpAppSecurityValidator.Manifest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAppSecurityValidator.Manifest}; do not couple callers to its representation when the owning type exposes an API.
             */
            String contentSecurityPolicy,
            /**
             * 中文说明：保存 permissions 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code McpAppSecurityValidator.Manifest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by permissions; its type is {@code Set<String>}, and {@code McpAppSecurityValidator.Manifest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpAppSecurityValidator.Manifest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAppSecurityValidator.Manifest}; do not couple callers to its representation when the owning type exposes an API.
             */
            Set<String> permissions,
            /**
             * 中文说明：保存 allowedOrigins 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code McpAppSecurityValidator.Manifest} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by allowed origins; its type is {@code Set<String>}, and {@code McpAppSecurityValidator.Manifest} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code McpAppSecurityValidator.Manifest} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code McpAppSecurityValidator.Manifest}; do not couple callers to its representation when the owning type exposes an API.
             */
            Set<String> allowedOrigins
    ) {

        /**
         * 中文说明：创建 {@code McpAppSecurityValidator.Manifest} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code McpAppSecurityValidator.Manifest} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param serverCode 参数 服务器Code；parameter server code。
         * @param appCode 参数 appCode；parameter app code。
         * @param version 参数 version；parameter version。
         * @param resourceUri 参数 资源Uri；parameter resource uri。
         * @param sha256 参数 sha256；parameter sha256。
         * @param sizeBytes 参数 sizeBytes；parameter size bytes。
         * @param mimeType 参数 mimeType；parameter mime type。
         * @param contentSecurityPolicy 参数 content安全策略；parameter content security policy。
         * @param permissions 参数 permissions；parameter permissions。
         * @param allowedOrigins 参数 allowedOrigins；parameter allowed origins。
         */
        public Manifest {
            serverCode = required(serverCode, "serverCode");
            appCode = required(appCode, "appCode");
            version = required(version, "version");
            resourceUri = required(resourceUri, "resourceUri");
            sha256 = required(sha256, "sha256").toLowerCase(Locale.ROOT);
            mimeType = required(mimeType, "mimeType");
            contentSecurityPolicy = required(
                    contentSecurityPolicy,
                    "contentSecurityPolicy"
            );
            permissions = Set.copyOf(permissions == null
                    ? Set.of()
                    : permissions);
            allowedOrigins = Set.copyOf(allowedOrigins == null
                    ? Set.of()
                    : allowedOrigins);
            if (!sha256.matches("[0-9a-f]{64}")
                    || sizeBytes < 1L
                    || sizeBytes > McpAppArtifactStore.MAX_ARTIFACT_BYTES) {
                throw McpResourceDriver.rejected(
                        "MCP App artifact metadata is invalid"
                );
            }
        }

        /**
         * 中文说明：执行 required 操作；该方法是 {@code McpAppSecurityValidator.Manifest} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the required operation; this method is the invocation entry point on {@code McpAppSecurityValidator.Manifest} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code McpAppSecurityValidator.Manifest.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param value 参数 值；parameter value。
         * @param field 参数 field；parameter field。
         * @return 返回 required 的处理结果；returns the result of the operation.
         */
        private static String required(String value, String field) {
            if (value == null || value.isBlank()) {
                throw McpResourceDriver.rejected(
                        "MCP App " + field + " is required"
                );
            }
            return value.trim();
        }
    }
}
