package top.egon.cola.component.gateway.starter.reporting;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReport;
import top.egon.cola.component.gateway.starter.GatewayReportingProperties;
import top.egon.cola.component.gateway.starter.discovery.GatewayDefinitionContributor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayDefinitionReportFactoryTest {

    @Test
    void scanOrderAndTransportIdentityDoNotChangeDefinitionSet() {
        GatewayDefinitionReportFactory first = factory(
                "2026-07-25T00:00:00Z"
        );
        GatewayDefinitionReportFactory second = factory(
                "2026-07-25T01:00:00Z"
        );

        GatewayDefinitionReportFactory.BuiltReport left =
                first.build(List.of(group("b"), group("a")));
        GatewayDefinitionReportFactory.BuiltReport right =
                second.build(List.of(group("a"), group("b")));

        assertThat(left.report().reportId())
                .isNotEqualTo(right.report().reportId());
        assertThat(left.identity().definitionFingerprint())
                .isEqualTo(right.identity().definitionFingerprint());
        assertThat(left.identity().definitionSetId())
                .isEqualTo(right.identity().definitionSetId());
    }

    @Test
    void keepsExternalAccessFalseWhenDefinitionDeclaresFalse() {
        GatewayDefinitionReportFactory.BuiltReport built =
                factory("2026-07-25T00:00:00Z")
                        .build(List.of(group("orders")));

        assertThat(built.report()
                .businessDomains().getFirst()
                .entityDomains().getFirst()
                .interfaceGroups().getFirst()
                .operations().getFirst()
                .externalAccessible()).isFalse();
    }

    private GatewayDefinitionReportFactory factory(String instant) {
        GatewayReportingProperties properties =
                new GatewayReportingProperties();
        properties.setEnabled(true);
        properties.setAdminBaseUrl("http://admin");
        properties.setApplicationCode("orders");
        properties.setBizCode("test-biz");
        properties.setApplicationName("Orders");
        properties.setEnv("test");
        properties.setNamespace("default");
        properties.setArtifactVersion("1.0.0");
        properties.setBuildId("build-1");
        properties.setAccessKey("access");
        properties.setSecretKey("secret");
        return new GatewayDefinitionReportFactory(
                properties,
                Clock.fixed(Instant.parse(instant), ZoneOffset.UTC)
        );
    }

    private GatewayDefinitionContributor.DiscoveredInterfaceGroup group(
            String code) {
        GatewayInterfaceDefinitionReport.Operation operation =
                new GatewayInterfaceDefinitionReport.Operation(
                        "orders:http:GET:/" + code,
                        "HTTP",
                        "GET /" + code,
                        code,
                        code,
                        null,
                        null,
                        List.of(),
                        false,
                        "SUPPORTED",
                        new GatewayInterfaceDefinitionReport.ProviderService(
                                "test-biz",
                                "test-app",
                                "test",
                                "default",
                                "HTTP",
                                "orders",
                                "default",
                                "1.0.0",
                                "HTTP"
                        ),
                        Map.of(),
                        Map.of(),
                        List.of(),
                        null,
                        Map.of(),
                        false
                );
        return new GatewayDefinitionContributor.DiscoveredInterfaceGroup(
                "trade",
                "Trade",
                null,
                "order",
                "Order",
                null,
                new GatewayInterfaceDefinitionReport.InterfaceGroup(
                        code,
                        code,
                        null,
                        "STARTER",
                        "example." + code,
                        "HTTP",
                        Map.of(),
                        List.of(operation)
                )
        );
    }
}
