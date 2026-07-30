package top.egon.cola.platform.rbac3.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.contract.manifest.ResourceManifest;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ManifestContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void fieldDefinitionRequiresJsonPath() {
        assertThrows(
                IllegalArgumentException.class,
                () -> fieldDefinition(" ")
        );
    }

    @Test
    void manifestKeepsExactJsonFieldsAndRoundTrips() throws Exception {
        ResourceManifest manifest = new ResourceManifest(
                "rbac3-resource-manifest/v1",
                "finance-web",
                "Finance Web",
                "5.3.2",
                "build-20260730",
                7L,
                Instant.parse("2026-07-30T08:00:00Z"),
                "sha256:manifest",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(fieldDefinition("$.customer.mobile"))
        );

        JsonNode json = objectMapper.valueToTree(manifest);

        assertEquals(Set.of(
                "schemaVersion",
                "applicationCode",
                "applicationName",
                "artifactVersion",
                "buildId",
                "manifestVersion",
                "generatedAt",
                "checksum",
                "apps",
                "menus",
                "routes",
                "actions",
                "apis",
                "fieldDefinitions"
        ), json.properties().stream().map(java.util.Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet()));
        JsonNode field = json.path("fieldDefinitions").get(0);
        assertEquals(Set.of(
                "resourceCode",
                "fieldCode",
                "jsonPath",
                "dataType",
                "sensitivity",
                "defaultAccess",
                "maskingStrategy",
                "writable",
                "exportable"
        ), field.properties().stream().map(java.util.Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet()));
        assertEquals("$.customer.mobile", field.path("jsonPath").textValue());
        assertEquals(
                manifest,
                objectMapper.treeToValue(json, ResourceManifest.class)
        );
    }

    private static ResourceManifest.FieldDefinition fieldDefinition(
            String jsonPath) {
        return new ResourceManifest.FieldDefinition(
                "finance:customer",
                "mobile",
                jsonPath,
                "STRING",
                "PERSONAL",
                "MASK",
                "MOBILE",
                false,
                false
        );
    }
}
