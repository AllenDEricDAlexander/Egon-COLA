package top.egon.cola.component.gateway.test.rpc.provider;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import top.egon.cola.component.gateway.contract.definition
        .GatewayDefinitionIdentity;
import top.egon.cola.component.rpc.provider.RpcProviderMetadataContributor;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
public class RpcProviderMetadataConfiguration {

    @Bean
    public RpcProviderMetadataContributor gatewayTestMetadata(
            ObjectProvider<GatewayDefinitionIdentity> definitions,
            Environment environment) {
        return ignored -> {
            Map<String, String> metadata = new LinkedHashMap<>();
            metadata.put(
                    "gateway.zone",
                    environment.getProperty("gateway.test.zone", "zone-a")
            );
            metadata.put(
                    "gateway.weight",
                    environment.getProperty("gateway.test.weight", "100")
            );
            GatewayDefinitionIdentity definitionIdentity =
                    definitions.getIfAvailable();
            if (definitionIdentity != null) {
                metadata.put(
                        "gateway.definition-set-id",
                        definitionIdentity.definitionSetId()
                );
                metadata.put(
                        "gateway.artifact-version",
                        definitionIdentity.artifactVersion()
                );
                metadata.put(
                        "gateway.build-id",
                        definitionIdentity.buildId()
                );
            }
            return Map.copyOf(metadata);
        };
    }
}
