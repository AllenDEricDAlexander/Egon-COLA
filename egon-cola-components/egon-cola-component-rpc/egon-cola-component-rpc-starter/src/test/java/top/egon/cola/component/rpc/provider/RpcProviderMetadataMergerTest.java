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
    void leavesAdapterSpecificMetadataValidationOutsideRpcCore() {
        RpcProviderMetadataMerger merger =
                new RpcProviderMetadataMerger(List.of());

        assertThat(merger.merge(SERVICE, Map.of(
                "gateway.weight", "adapter-defined",
                "ddc.extension", "adapter-owned",
                "egon.internal.extension", "application-owned"
        ))).containsExactly(
                Map.entry("ddc.extension", "adapter-owned"),
                Map.entry("egon.internal.extension", "application-owned"),
                Map.entry("gateway.weight", "adapter-defined")
        );
    }

    @Test
    void rejectsBlankKeysAndValues() {
        RpcProviderMetadataMerger merger =
                new RpcProviderMetadataMerger(List.of());

        assertThatThrownBy(() -> merger.merge(SERVICE, Map.of(" ", "value")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
        assertThatThrownBy(() -> merger.merge(SERVICE, Map.of("custom", " ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
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
