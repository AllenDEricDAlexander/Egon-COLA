package top.egon.cola.component.gateway.openapi.customizer;

import io.swagger.v3.oas.models.OpenAPI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springdoc.core.customizers.OpenApiCustomizer;
import top.egon.cola.component.gateway.contract.reporting.openapi.GatewayOpenApiGroupManifestDTO;
import top.egon.cola.component.gateway.openapi.config.GatewayOpenApiProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adds the trusted Provider build and published Group identity to an OpenAPI
 * root document.
 */
@Slf4j
@RequiredArgsConstructor
public class EgonOpenApiCustomizer implements OpenApiCustomizer {

    @Qualifier("gatewayOpenApiProperties")
    private final GatewayOpenApiProperties properties;

    @Override
    public void customise(OpenAPI openAPI) {
        properties.validate();
        if (properties.getPublishedGroups().size() != 1) {
            throw new IllegalArgumentException(
                    "a grouped OpenAPI customizer requires an explicit group"
            );
        }
        customise(openAPI, properties.getPublishedGroups().get(0));
    }

    /**
     * Applies the root identity for one explicit Springdoc Group.
     */
    public void customise(OpenAPI openAPI, String group) {
        if (openAPI == null) {
            throw new IllegalArgumentException("openAPI is required");
        }
        properties.validate();
        if (!properties.getPublishedGroups().contains(group)) {
            throw new IllegalArgumentException(
                    "OpenAPI group is not published: " + group
            );
        }
        if (openAPI.getOpenapi() == null || openAPI.getOpenapi().isBlank()) {
            openAPI.setOpenapi("3.1.0");
        }
        Map<String, Object> extension = new LinkedHashMap<>();
        extension.put("version", 1);
        extension.put("bizCode", properties.getBizCode());
        extension.put("applicationCode", properties.getApplicationCode());
        extension.put("artifactVersion", properties.getArtifactVersion());
        extension.put("buildId", properties.getBuildId());
        extension.put("openapiGroup", group);
        openAPI.addExtension("x-egon-service", extension);
    }
}
