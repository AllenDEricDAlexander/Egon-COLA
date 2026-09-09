package top.egon.cola.component.yuheng.admin.openapi.domain.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * Bounded response handoff from the Provider OpenAPI client to validators.
 *
 * <p>中文：保存原始 bytes、精确 SHA 和解析后的 JSON tree；访问器返回副本，
 * 防止后续适配或校验修改客户端结果。</p>
 */
public record GatewayOpenApiDocumentDTO(
        @Valid
        @NotNull(groups = {Default.class, GatewayOpenApiIngestionGroup.class})
        GatewayOpenApiSyncCandidateDTO candidate,
        @NotEmpty(groups = {Default.class, GatewayOpenApiIngestionGroup.class})
        @Size(
                max = MAX_DOCUMENT_BYTES,
                groups = {Default.class, GatewayOpenApiIngestionGroup.class}
        )
        byte[] rawBytes,
        @NotBlank(groups = {Default.class, GatewayOpenApiIngestionGroup.class})
        @Pattern(
                regexp = "[0-9a-f]{64}",
                groups = {Default.class, GatewayOpenApiIngestionGroup.class}
        )
        String documentSha256,
        @NotNull(groups = {Default.class, GatewayOpenApiIngestionGroup.class})
        JsonNode documentJson,
        @NotBlank(groups = {Default.class, GatewayOpenApiIngestionGroup.class})
        String contentType,
        int statusCode,
        @NotNull(groups = {Default.class, GatewayOpenApiIngestionGroup.class})
        Instant fetchedAt
) {

    public static final int MAX_DOCUMENT_BYTES = 5 * 1024 * 1024;

    public GatewayOpenApiDocumentDTO {
        candidate = Objects.requireNonNull(candidate, "candidate");
        rawBytes = Arrays.copyOf(
                Objects.requireNonNull(rawBytes, "rawBytes"),
                rawBytes.length
        );
        if (rawBytes.length == 0 || rawBytes.length > MAX_DOCUMENT_BYTES) {
            throw new IllegalArgumentException(
                    "rawBytes must contain 1.."
                            + MAX_DOCUMENT_BYTES
                            + " bytes"
            );
        }
        documentSha256 = requiredHash(documentSha256);
        if (!documentSha256.equals(sha256(rawBytes))) {
            throw new IllegalArgumentException(
                    "documentSha256 does not match rawBytes"
            );
        }
        documentJson = Objects.requireNonNull(
                documentJson,
                "documentJson"
        ).deepCopy();
        if (documentJson == null || !documentJson.isObject()) {
            throw new IllegalArgumentException(
                    "documentJson must be a JSON object"
            );
        }
        contentType = required(contentType, "contentType", 256)
                .toLowerCase(Locale.ROOT);
        if (statusCode < 100 || statusCode > 599) {
            throw new IllegalArgumentException(
                    "statusCode must be an HTTP status"
            );
        }
        fetchedAt = Objects.requireNonNull(fetchedAt, "fetchedAt");
    }

    @Override
    public byte[] rawBytes() {
        return Arrays.copyOf(rawBytes, rawBytes.length);
    }

    @Override
    public JsonNode documentJson() {
        return documentJson.deepCopy();
    }

    private static String requiredHash(String value) {
        String normalized = required(value, "documentSha256", 64);
        if (!normalized.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(
                    "documentSha256 must be lowercase hexadecimal SHA-256"
            );
        }
        return normalized;
    }

    private static String required(String value, String field, int max) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty() || normalized.length() > max) {
            throw new IllegalArgumentException(
                    field + " is blank or exceeds " + max + " characters"
            );
        }
        return normalized;
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes)
            );
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    failure
            );
        }
    }
}
