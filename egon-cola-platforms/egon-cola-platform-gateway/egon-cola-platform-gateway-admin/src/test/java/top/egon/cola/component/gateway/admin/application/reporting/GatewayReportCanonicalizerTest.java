package top.egon.cola.component.gateway.admin.application.reporting;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReport;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;

class GatewayReportCanonicalizerTest {

    @Test
    void acceptsStarterCanonicalReportWithNullableMetadata() throws Exception {
        GatewayInterfaceDefinitionReport.Application application =
                new GatewayInterfaceDefinitionReport.Application(
                        "test-biz",
                        "orders", "Orders", "test", "default"
                );
        GatewayInterfaceDefinitionReport.Build build =
                new GatewayInterfaceDefinitionReport.Build(
                        "1.0.0", "build-1", Map.of()
                );
        List<GatewayInterfaceDefinitionReport.BusinessDomain> domains =
                List.of(new GatewayInterfaceDefinitionReport.BusinessDomain(
                        "trade", "Trade", null, List.of()
                ));
        String fingerprint = starterFingerprint(application, build, domains);
        String definitionSetId = sha256(String.join(
                "\n", "test-biz", "orders", "test", "default", "1.0.0",
                "build-1", fingerprint
        ).getBytes(StandardCharsets.UTF_8));
        GatewayInterfaceDefinitionReport report =
                new GatewayInterfaceDefinitionReport(
                        "v2",
                        "report-1",
                        Instant.parse("2026-07-27T00:00:00Z"),
                        application,
                        build,
                        true,
                        definitionSetId,
                        fingerprint,
                        domains
                );

        assertThatCode(() -> new GatewayReportCanonicalizer().verify(report))
                .doesNotThrowAnyException();
    }

    private String starterFingerprint(
            GatewayInterfaceDefinitionReport.Application application,
            GatewayInterfaceDefinitionReport.Build build,
            List<GatewayInterfaceDefinitionReport.BusinessDomain> domains)
            throws Exception {
        JsonMapper mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .build();
        return sha256(mapper.writeValueAsBytes(Map.of(
                "application", application,
                "build", build,
                "businessDomains", domains,
                "complete", true,
                "definitionSchemaVersion", "v2"
        )));
    }

    private String sha256(byte[] value) throws Exception {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value)
        );
    }
}
