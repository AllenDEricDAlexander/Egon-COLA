package top.egon.cola.component.yuheng.test.rpc.provider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import top.egon.cola.component.rpc.provider.metadata.RpcProviderMetadataContributor;

import java.util.Map;

@Configuration(proxyBeanMethods = false)
public class RpcProviderMetadataConfiguration {

    @Bean
    public RpcProviderMetadataContributor gatewayTestMetadata(
            Environment environment) {
        return ignored -> Map.of(
                "yuheng.zone",
                environment.getProperty("yuheng.test.zone", "zone-a"),
                "yuheng.weight",
                environment.getProperty("yuheng.test.weight", "100")
        );
    }
}
