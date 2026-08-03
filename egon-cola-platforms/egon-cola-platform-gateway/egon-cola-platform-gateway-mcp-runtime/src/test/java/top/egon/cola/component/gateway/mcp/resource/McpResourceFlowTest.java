package top.egon.cola.component.gateway.mcp.resource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.mcp.protocol.McpProtocolException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpResourceFlowTest {

    @TempDir
    Path storageRoot;

    private final McpResourceUriValidator validator =
            new McpResourceUriValidator();

    @Test
    void localOperationDriverUsesControlPlaneDriverType() {
        assertEquals("LOCAL_OPERATION", OperationResourceDriver.DRIVER_TYPE);
    }

    @Test
    void resourceUriCannotSelectNetworkOrEscapeStorageRoot() throws Exception {
        assertEquals("ui", validator.validate(
                "ui://finance/dashboard/1.0.0"
        ).getScheme());
        assertRejected("https://169.254.169.254/latest/meta-data");
        assertRejected("file:///etc/passwd");
        assertRejected("egon://finance/../../secret");
        assertRejected("egon://finance/%2e%2e/secret");
        assertRejected("egon://finance/path%2fescape");

        Path outside = Files.createTempFile("mcp-outside", ".txt");
        Path link = storageRoot.resolve("link.txt");
        Files.createSymbolicLink(link, outside);
        ObjectStorageResourceDriver driver =
                new ObjectStorageResourceDriver(validator);
        McpProtocolException failure = assertThrows(
                McpProtocolException.class,
                () -> Mono.from(driver.read(request(
                        "egon://finance/link.txt",
                        Map.of("root", storageRoot.toString())
                ))).block()
        );
        assertEquals(McpErrorCode.MCP_RESOURCE_REJECTED, failure.code());
    }

    @Test
    void staticAndObjectStorageDriversEnforceMimeAndMaximumBytes()
            throws Exception {
        StaticTextResourceDriver text = new StaticTextResourceDriver();
        McpResourceDriver.Content content = Mono.from(text.read(new
                McpResourceDriver.ReadRequest(
                        "finance",
                        "policy",
                        "egon://finance/policy",
                        "text/plain",
                        null,
                        Map.of("content", "approved"),
                        Map.of(),
                        16,
                        Map.of()
                ))).block();
        assertEquals("approved", content.text());

        McpResourceDriver.Content fixtureContent = Mono.from(text.read(new
                McpResourceDriver.ReadRequest(
                        "finance",
                        "fixture-policy",
                        "egon://finance/fixture-policy",
                        "text/plain",
                        null,
                        Map.of("text", "fixture-approved"),
                        Map.of(),
                        32,
                        Map.of()
                ))).block();
        assertEquals("fixture-approved", fixtureContent.text());

        Files.writeString(
                storageRoot.resolve("report.txt"),
                "daily-report",
                StandardCharsets.UTF_8
        );
        ObjectStorageResourceDriver storage =
                new ObjectStorageResourceDriver(validator);
        McpResourceDriver.Content stored = Mono.from(storage.read(request(
                "egon://finance/report.txt",
                Map.of("root", storageRoot.toString())
        ))).block();
        assertArrayEquals(
                "daily-report".getBytes(StandardCharsets.UTF_8),
                stored.data()
        );

        McpProtocolException tooLarge = assertThrows(
                McpProtocolException.class,
                () -> Mono.from(storage.read(new McpResourceDriver.ReadRequest(
                        "finance",
                        "report",
                        "egon://finance/report.txt",
                        "text/plain",
                        null,
                        Map.of("root", storageRoot.toString()),
                        Map.of(),
                        4,
                        Map.of()
                ))).block()
        );
        assertEquals(McpErrorCode.MCP_RESOURCE_REJECTED, tooLarge.code());
    }

    @Test
    void databaseSchemaDriverReadsOnlyConfiguredSchema() {
        DatabaseSchemaResourceDriver driver =
                new DatabaseSchemaResourceDriver(
                        (schema, objectName) -> "schema:" + schema + ':'
                                + objectName,
                        validator
                );
        McpResourceDriver.Content content = Mono.from(driver.read(new
                McpResourceDriver.ReadRequest(
                        "finance",
                        "orders-schema",
                        "egon://finance/schema/public/orders",
                        "application/json",
                        null,
                        Map.of("allowedSchemas", "public,audit"),
                        Map.of(),
                        128,
                        Map.of()
                ))).block();
        assertEquals("schema:public:orders", content.text());

        assertThrows(
                McpProtocolException.class,
                () -> Mono.from(driver.read(new McpResourceDriver.ReadRequest(
                        "finance",
                        "secret-schema",
                        "egon://finance/schema/private/users",
                        "application/json",
                        null,
                        Map.of("allowedSchemas", "public,audit"),
                        Map.of(),
                        128,
                        Map.of()
                ))).block()
        );
    }

    private McpResourceDriver.ReadRequest request(
            String uri,
            Map<String, String> configuration) {
        return new McpResourceDriver.ReadRequest(
                "finance",
                "report",
                uri,
                "text/plain",
                null,
                configuration,
                Map.of(),
                1024,
                Map.of()
        );
    }

    private void assertRejected(String uri) {
        McpProtocolException failure = assertThrows(
                McpProtocolException.class,
                () -> validator.validate(uri)
        );
        assertEquals(McpErrorCode.MCP_RESOURCE_REJECTED, failure.code());
    }
}
