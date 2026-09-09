package top.egon.cola.component.yuheng.openapi.registration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.tianshu.http.registration.DdcHttpRegistrationContributor;
import top.egon.cola.component.yuheng.contract.reporting.openapi.GatewayOpenApiGroupManifestDTO;
import top.egon.cola.component.yuheng.openapi.config.GatewayOpenApiProperties;

import java.util.Map;

/**
 * Publishes only the bounded OpenAPI capability locator to DDC.
 */
@Slf4j
@RequiredArgsConstructor
public class GatewayOpenApiRegistrationContributor
        implements DdcHttpRegistrationContributor {

    @Qualifier("gatewayOpenApiProperties")
    private final GatewayOpenApiProperties properties;

    @Override
    public String serviceVersion() {
        properties.validate();
        return properties.getArtifactVersion();
    }

    @Override
    public Map<String, String> metadata() {
        properties.validate();
        GatewayOpenApiGroupManifestDTO manifest =
                properties.groupManifest();
        return Map.of(
                "gateway.definition-source", "OPENAPI31",
                "gateway.openapi.enabled", "true",
                "gateway.openapi.path-template", manifest.pathTemplate(),
                "gateway.openapi.spec", "3.1",
                "gateway.openapi.groups", String.join(",", manifest.groups()),
                "gateway.openapi.resource-uri", manifest.resourceUri(),
                "gateway.artifact-version", manifest.artifactVersion(),
                "gateway.build-id", manifest.buildId()
        );
    }
}
