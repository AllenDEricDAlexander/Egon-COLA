package top.egon.cola.component.gateway.mcp.app;

import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeApp;
import top.egon.cola.component.gateway.core.mcp.app.McpAppArtifactStore;
import top.egon.cola.component.gateway.mcp.resource.McpResourceDriver;

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
 */
public final class McpAppSecurityValidator {

    private static final Set<String> REQUIRED_CSP_DIRECTIVES = Set.of(
            "default-src",
            "script-src",
            "connect-src",
            "base-uri",
            "form-action",
            "frame-ancestors"
    );

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

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(content)
            );
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 is unavailable", failure);
        }
    }

    private RuntimeException rejected(String message) {
        return McpResourceDriver.rejected(message);
    }

    public record Manifest(
            String serverCode,
            String appCode,
            String version,
            String resourceUri,
            String sha256,
            long sizeBytes,
            String mimeType,
            String contentSecurityPolicy,
            Set<String> permissions,
            Set<String> allowedOrigins
    ) {

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
