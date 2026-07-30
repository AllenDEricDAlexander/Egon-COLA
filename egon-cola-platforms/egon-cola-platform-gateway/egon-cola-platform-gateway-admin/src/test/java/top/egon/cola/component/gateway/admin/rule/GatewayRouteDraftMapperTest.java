package top.egon.cola.component.gateway.admin.rule;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.contract.rule.GatewayRequestBodyMode;
import top.egon.cola.component.gateway.contract.rule.GatewayRouteProfile;
import top.egon.cola.component.gateway.contract.rule.GatewayRouteTransportPolicy;
import top.egon.cola.component.gateway.contract.rule.GatewayTransportResponseMode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayRouteDraftMapperTest {

    private final GatewayRouteDraftMapper mapper =
            new GatewayRouteDraftMapper();

    @Test
    void readsLegacyKeysIntoCanonicalRouteFields() {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("host", " ai.example.com ");
        content.put("listener", "public");
        content.put("method", "post");
        content.put("path", " /v1/** ");

        Map<String, Object> canonical = mapper.canonicalize(content);

        assertThat(canonical).containsEntry("host", "ai.example.com")
                .containsEntry("httpMethod", "POST")
                .containsEntry("pathPattern", "/v1/**")
                .containsEntry("accessZones", List.of("PUBLIC"))
                .containsEntry("priority", 0);
        assertThat(canonical).doesNotContainKeys(
                "listener",
                "method",
                "path"
        );
    }

    @Test
    void canonicalKeysWinAndUnknownExtensionsSurviveNormalization() {
        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("profile", "OPENAI_HTTP");
        policy.put("futureOption", false);
        Map<String, Object> extension = Map.of("enabled", false);
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("host", "api.example.com");
        content.put("httpMethod", "PUT");
        content.put("method", "POST");
        content.put("pathPattern", "/canonical/**");
        content.put("path", "/legacy/**");
        content.put("accessZones", List.of("INTERNAL"));
        content.put("listener", "PUBLIC");
        content.put("priority", 3);
        content.put("protocol", "HTTP");
        content.put("fullMethodName", "legacy.Service/Call");
        content.put("providerServiceName", "legacy-provider");
        content.put("operationExternalAccessible", true);
        content.put("customExtension", extension);
        content.put("transportPolicy", policy);

        Map<String, Object> canonical = mapper.canonicalize(content);

        assertThat(canonical).containsEntry("httpMethod", "PUT")
                .containsEntry("pathPattern", "/canonical/**")
                .containsEntry("accessZones", List.of("INTERNAL"))
                .containsEntry("customExtension", extension)
                .containsEntry("transportPolicy", policy);
        assertThat(canonical).doesNotContainKeys(
                "listener",
                "method",
                "path",
                "protocol",
                "fullMethodName",
                "providerServiceName",
                "operationExternalAccessible"
        );
    }

    @Test
    void doesNotInventHostForLegacyDraft() {
        Map<String, Object> canonical = mapper.canonicalize(Map.of(
                "listener", "PUBLIC",
                "method", "POST",
                "path", "/v1/**"
        ));

        assertThat(canonical).doesNotContainKey("host");
        assertThat(canonical.values()).doesNotContain("*");
    }

    @Test
    void mapsOnlyKnownPolicyFieldsOntoTheTypedSnapshotModel() {
        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("profile", "OPENAI_HTTP");
        policy.put("requestBodyMode", "STREAMING");
        policy.put("responseMode", "AUTO_STREAM");
        policy.put("connectTimeoutMs", 10_000);
        policy.put("bodyLogEnabled", false);
        policy.put("retryEnabled", false);
        policy.put("futureOption", false);

        GatewayRouteTransportPolicy mapped = mapper.transportPolicy(Map.of(
                "transportPolicy", policy
        ));

        assertThat(mapped.profile()).isEqualTo(GatewayRouteProfile.OPENAI_HTTP);
        assertThat(mapped.requestBodyMode())
                .isEqualTo(GatewayRequestBodyMode.STREAMING);
        assertThat(mapped.responseMode())
                .isEqualTo(GatewayTransportResponseMode.AUTO_STREAM);
        assertThat(mapped.connectTimeoutMs()).isEqualTo(10_000L);
        assertThat(mapped.bodyLogEnabled()).isFalse();
        assertThat(mapped.retryEnabled()).isFalse();
    }
}
