package top.egon.cola.component.yuheng.openapi.registration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.tianshu.http.registration.DdcHttpRegistrationContributor;
import top.egon.cola.component.yuheng.contract.reporting.openapi.GatewayOpenApiGroupManifestDTO;
import top.egon.cola.component.yuheng.openapi.config.GatewayOpenApiProperties;

import java.util.Map;

/**
 * Publishes only the bounded OpenAPI capability locator to Tianshu.
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
                "yuheng.definition-source", "OPENAPI31",
                "yuheng.openapi.enabled", "true",
                "yuheng.openapi.path-template", manifest.pathTemplate(),
                "yuheng.openapi.spec", "3.1",
                "yuheng.openapi.groups", String.join(",", manifest.groups()),
                "yuheng.openapi.resource-uri", manifest.resourceUri(),
                "yuheng.artifact-version", manifest.artifactVersion(),
                "yuheng.build-id", manifest.buildId()
        );
    }
}
