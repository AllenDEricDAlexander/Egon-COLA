package top.egon.cola.component.gateway.admin.application.reporting;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReport;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

public final class GatewayReportCanonicalizer {

    private final com.fasterxml.jackson.databind.ObjectMapper mapper =
            JsonMapper.builder()
                    .addModule(new JavaTimeModule())
                    .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                    .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                    .disable(
                            SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
                    )
                    .serializationInclusion(JsonInclude.Include.NON_NULL)
                    .build();

    public void verify(GatewayInterfaceDefinitionReport report) {
        String fingerprint = sha256(bytes(Map.of(
                "application", report.application(),
                "build", report.build(),
                "businessDomains", report.businessDomains(),
                "complete", report.complete(),
                "definitionSchemaVersion", "v1"
        )));
        if (!fingerprint.equals(report.definitionFingerprint())) {
            throw new IllegalArgumentException(
                    "definitionFingerprint does not match canonical report"
            );
        }
        String definitionSetId = sha256(String.join(
                "\n",
                report.application().bizCode(),
                report.application().applicationCode(),
                report.application().env(),
                report.application().namespace(),
                report.build().artifactVersion(),
                report.build().buildId(),
                fingerprint
        ).getBytes(StandardCharsets.UTF_8));
        if (!definitionSetId.equals(report.definitionSetId())) {
            throw new IllegalArgumentException(
                    "definitionSetId does not match canonical identity"
            );
        }
    }

    public String payloadSha256(GatewayInterfaceDefinitionReport report) {
        return sha256(bytes(report));
    }

    private byte[] bytes(Object value) {
        try {
            return mapper.writeValueAsBytes(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException(
                    "gateway report cannot be canonicalized",
                    failure
            );
        }
    }

    private String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value)
            );
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
