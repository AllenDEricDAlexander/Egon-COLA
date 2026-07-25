package top.egon.cola.component.rpc.provider;

import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RpcProviderMetadataMergerTest {

    private static final RpcServiceIdentity SERVICE =
            new RpcServiceIdentity("echo.Service", "default", "1.0.0");

    @Test
    void invokesContributorsInSpringOrderAndAllowsIdempotentValues() {
        List<String> calls = new ArrayList<>();
        RpcProviderMetadataContributor later = contributor(
                20,
                calls,
                "later",
                Map.of("gateway.region", "cn-east")
        );
        RpcProviderMetadataContributor earlier = contributor(
                10,
                calls,
                "earlier",
                Map.of(
                        "gateway.zone", "zone-a",
                        "gateway.weight", "100"
                )
        );
        RpcProviderMetadataMerger merger =
                new RpcProviderMetadataMerger(List.of(later, earlier));

        Map<String, String> metadata = merger.merge(
                SERVICE,
                Map.of(
                        "gateway.weight", "100",
                        "custom.key", "value"
                )
        );

        assertThat(calls).containsExactly("earlier", "later");
        assertThat(metadata)
                .containsEntry("gateway.zone", "zone-a")
                .containsEntry("gateway.region", "cn-east")
                .containsEntry("gateway.weight", "100")
                .containsEntry("custom.key", "value");
        assertThatThrownBy(() -> metadata.put("another", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsDifferentValuesForTheSameKeyAcrossAnySource() {
        RpcProviderMetadataMerger merger = new RpcProviderMetadataMerger(
                List.of(service -> Map.of("gateway.zone", "zone-b"))
        );

        assertThatThrownBy(() -> merger.merge(
                SERVICE,
                Map.of("gateway.zone", "zone-a")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("conflict");
    }

    @Test
    void rejectsFrameworkMetadataFromPropertiesOrContributors() {
        RpcProviderMetadataMerger merger =
                new RpcProviderMetadataMerger(List.of());
        assertThatThrownBy(() -> merger.merge(
                SERVICE,
                Map.of("egon.rpc.transport", "grpc")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reserved");

        RpcProviderMetadataMerger contributed =
                new RpcProviderMetadataMerger(List.of(
                        service -> Map.of(
                                "egon.rpc.runtime-version",
                                "forged"
                        )
                ));
        assertThatThrownBy(() -> contributed.merge(SERVICE, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reserved");
    }

    @Test
    void validatesGatewayWeightTagsAndLocationFields() {
        RpcProviderMetadataMerger merger =
                new RpcProviderMetadataMerger(List.of());

        assertThat(merger.merge(SERVICE, Map.of(
                "gateway.weight", "10000",
                "gateway.tags", "canary=true,tenant=retail",
                "gateway.zone", "cn-east-1a",
                "gateway.region", "cn-east-1",
                "gateway.management-path", "/actuator/health",
                "gateway.protocol-version", "1.0.0",
                "gateway.definition-set-id", "definition-1",
                "gateway.artifact-version", "5.2.3",
                "gateway.build-id", "build+42"
        ))).hasSize(9);

        List<Map<String, String>> invalid = List.of(
                Map.of("gateway.weight", "0"),
                Map.of("gateway.weight", "10001"),
                Map.of("gateway.weight", "1.0"),
                Map.of("gateway.tags", "tenant=retail,canary=true"),
                Map.of("gateway.tags", "tenant"),
                Map.of("gateway.zone", "zone a"),
                Map.of("gateway.region", ""),
                Map.of("gateway.management-path", "actuator/health")
        );
        invalid.forEach(metadata ->
                assertThatThrownBy(() -> merger.merge(SERVICE, metadata))
                        .as("invalid metadata %s", metadata)
                        .isInstanceOf(IllegalArgumentException.class));
    }

    @Test
    void acceptsNullAndEmptyContributorResults() {
        RpcProviderMetadataMerger merger = new RpcProviderMetadataMerger(
                List.of(service -> null, service -> Map.of())
        );

        assertThat(merger.merge(SERVICE, null)).isEmpty();
    }

    private RpcProviderMetadataContributor contributor(
            int order,
            List<String> calls,
            String name,
            Map<String, String> metadata
    ) {
        return new OrderedContributor(order, calls, name, metadata);
    }

    private record OrderedContributor(
            int order,
            List<String> calls,
            String name,
            Map<String, String> metadata
    ) implements RpcProviderMetadataContributor, Ordered {

        @Override
        public Map<String, String> contribute(
                RpcServiceIdentity serviceIdentity
        ) {
            calls.add(name);
            return new LinkedHashMap<>(metadata);
        }

        @Override
        public int getOrder() {
            return order;
        }
    }
}
