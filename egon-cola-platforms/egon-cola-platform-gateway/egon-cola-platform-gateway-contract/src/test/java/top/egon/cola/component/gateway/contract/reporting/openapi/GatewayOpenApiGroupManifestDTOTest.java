package top.egon.cola.component.gateway.contract.reporting.openapi;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GatewayOpenApiGroupManifestDTOTest {

    private static final String PATH_TEMPLATE = "/v3/api-docs/{group}";

    @Test
    void normalizesAndProtectsThePublishedGroupManifest() {
        GatewayOpenApiGroupManifestDTO manifest =
                new GatewayOpenApiGroupManifestDTO(
                        List.of(" orders ", "inventory"),
                        PATH_TEMPLATE,
                        "https://provider.example.test",
                        " 1.0.0 ",
                        " build-1 "
                );

        assertEquals(List.of("inventory", "orders"), manifest.groups());
        assertEquals(PATH_TEMPLATE, manifest.pathTemplate());
        assertEquals("1.0.0", manifest.artifactVersion());
        assertEquals("build-1", manifest.buildId());
        assertThrows(
                UnsupportedOperationException.class,
                () -> manifest.groups().add("settlement")
        );
    }

    @Test
    void rejectsInvalidGroupMembershipAndMetadata() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new GatewayOpenApiGroupManifestDTO(
                        List.of(),
                        PATH_TEMPLATE,
                        "https://provider.example.test",
                        "1.0.0",
                        "build-1"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new GatewayOpenApiGroupManifestDTO(
                        List.of("orders", "orders"),
                        PATH_TEMPLATE,
                        "https://provider.example.test",
                        "1.0.0",
                        "build-1"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new GatewayOpenApiGroupManifestDTO(
                        List.of("Orders"),
                        PATH_TEMPLATE,
                        "https://provider.example.test",
                        "1.0.0",
                        "build-1"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new GatewayOpenApiGroupManifestDTO(
                        IntStream.range(0, 17)
                                .mapToObj(index -> "group-" + index)
                                .toList(),
                        PATH_TEMPLATE,
                        "https://provider.example.test",
                        "1.0.0",
                        "build-1"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new GatewayOpenApiGroupManifestDTO(
                        List.of("orders"),
                        "/v3/api-docs",
                        "https://provider.example.test",
                        "1.0.0",
                        "build-1"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new GatewayOpenApiGroupManifestDTO(
                        List.of("orders"),
                        PATH_TEMPLATE,
                        "http://provider.example.test",
                        "1.0.0",
                        "build-1"
                )
        );
    }
}
