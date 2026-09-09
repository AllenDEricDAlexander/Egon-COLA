package top.egon.cola.component.yuheng.admin.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import top.egon.cola.component.yuheng.admin.openapi.domain.dto.GatewayOpenApiDocumentDTO;
import top.egon.cola.component.yuheng.admin.openapi.domain.dto.GatewayOpenApiSyncCandidateDTO;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

/** Shared deterministic documents for the Step 8 validation and client tests. */
public final class GatewayOpenApiValidationTestFixture {

    public static final Instant NOW = Instant.parse("2026-08-26T03:00:00Z");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private GatewayOpenApiValidationTestFixture() {
    }

    public static GatewayOpenApiSyncCandidateDTO candidate() {
        return candidate("provider.internal", true);
    }

    public static GatewayOpenApiSyncCandidateDTO candidate(
            String host,
            boolean secure) {
        return new GatewayOpenApiSyncCandidateDTO(
                "app-1",
                "trade",
                "orders",
                "build-1",
                "1.0.0",
                "orders",
                "orders-service",
                "default",
                "1.0.0",
                "instance-1",
                host,
                9443,
                secure,
                "/v3/api-docs/{group}",
                URI.create("https://provider.example/resource"),
                NOW,
                NOW.plusSeconds(60)
        );
    }

    public static GatewayOpenApiDocumentDTO document() {
        return document("""
                {
                  "openapi":"3.1.0",
                  "info":{"title":"Orders","version":"1.0.0"},
                  "paths":{
                    "/orders":{"get":{"operationId":"orders.list",
                      "responses":{"200":{"description":"ok"}}}}
                  },
                  "components":{"schemas":{"Order":{"type":"object"}}},
                  "x-egon-service":{
                    "version":1,"bizCode":"trade","applicationCode":"orders",
                    "artifactVersion":"1.0.0","buildId":"build-1",
                    "openapiGroup":"orders"
                  }
                }
                """);
    }

    public static GatewayOpenApiDocumentDTO document(String json) {
        try {
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            JsonNode node = MAPPER.readTree(bytes);
            return new GatewayOpenApiDocumentDTO(
                    candidate(),
                    bytes,
                    sha256(bytes),
                    node,
                    "application/json",
                    200,
                    NOW
            );
        } catch (Exception failure) {
            throw new AssertionError("invalid test document", failure);
        }
    }

    public static GatewayOpenApiDocumentDTO document(
            GatewayOpenApiSyncCandidateDTO candidate,
            byte[] bytes,
            String contentType,
            int statusCode) {
        try {
            return new GatewayOpenApiDocumentDTO(
                    candidate,
                    bytes,
                    sha256(bytes),
                    MAPPER.readTree(bytes),
                    contentType,
                    statusCode,
                    NOW
            );
        } catch (Exception failure) {
            throw new AssertionError("invalid test document", failure);
        }
    }

    public static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes)
            );
        } catch (NoSuchAlgorithmException failure) {
            throw new AssertionError(failure);
        }
    }
}
