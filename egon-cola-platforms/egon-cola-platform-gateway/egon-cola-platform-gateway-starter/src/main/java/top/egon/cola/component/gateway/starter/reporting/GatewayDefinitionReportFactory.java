package top.egon.cola.component.gateway.starter.reporting;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.contract.reporting.GatewayDefinitionIdentity;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReport;
import top.egon.cola.component.gateway.starter.GatewayReportingProperties;
import top.egon.cola.component.gateway.starter.discovery.GatewayDefinitionContributor;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GatewayDefinitionReportFactory {

    private final GatewayReportingProperties properties;

    private final Clock clock;

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .build();

    public GatewayDefinitionReportFactory(
            GatewayReportingProperties properties) {
        this(properties, Clock.systemUTC());
    }

    GatewayDefinitionReportFactory(
            GatewayReportingProperties properties,
            Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public BuiltReport build(
            List<GatewayDefinitionContributor.DiscoveredInterfaceGroup>
                    groups) {
        properties.validate();
        GatewayInterfaceDefinitionReport.Application application =
                new GatewayInterfaceDefinitionReport.Application(
                        properties.getBizCode(),
                        properties.getApplicationCode(),
                        properties.getApplicationName(),
                        properties.getEnv(),
                        properties.getNamespace()
                );
        GatewayInterfaceDefinitionReport.Build build =
                new GatewayInterfaceDefinitionReport.Build(
                        properties.getArtifactVersion(),
                        properties.getBuildId(),
                        Map.of(
                                "javaVersion",
                                System.getProperty(
                                        "java.specification.version",
                                        "unknown"
                                ),
                                "starter", "egon-cola-gateway"
                        )
                );
        List<GatewayInterfaceDefinitionReport.BusinessDomain> domains =
                domains(groups);
        String fingerprint = sha256(bytes(Map.of(
                "application", application,
                "build", build,
                "businessDomains", domains,
                "complete", true,
                "definitionSchemaVersion", "v2"
        )));
        String definitionSetId = sha256(
                String.join(
                        "\n",
                        application.bizCode(),
                        application.applicationCode(),
                        application.env(),
                        application.namespace(),
                        build.artifactVersion(),
                        build.buildId(),
                        fingerprint
                ).getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
        GatewayInterfaceDefinitionReport report =
                new GatewayInterfaceDefinitionReport(
                        "v2",
                        UuidV7.simpleString(),
                        clock.instant(),
                        application,
                        build,
                        true,
                        definitionSetId,
                        fingerprint,
                        domains
                );
        return new BuiltReport(
                report,
                new GatewayDefinitionIdentity(
                        definitionSetId,
                        fingerprint,
                        build.artifactVersion(),
                        build.buildId()
                ),
                bytes(report)
        );
    }

    private List<GatewayInterfaceDefinitionReport.BusinessDomain> domains(
            List<GatewayDefinitionContributor.DiscoveredInterfaceGroup>
                    groups) {
        Map<String, MutableBusiness> businesses = new LinkedHashMap<>();
        groups.stream()
                .sorted(java.util.Comparator.comparing(group ->
                        group.businessDomainCode()
                                + "/"
                                + group.entityDomainCode()
                                + "/"
                                + group.interfaceGroup().code()))
                .forEach(group -> {
                    MutableBusiness business = businesses.computeIfAbsent(
                            group.businessDomainCode(),
                            ignored -> new MutableBusiness(
                                    group.businessDomainCode(),
                                    group.businessDomainName(),
                                    group.businessDomainDescription()
                            )
                    );
                    MutableEntity entity = business.entities.computeIfAbsent(
                            group.entityDomainCode(),
                            ignored -> new MutableEntity(
                                    group.entityDomainCode(),
                                    group.entityDomainName(),
                                    group.entityDomainDescription()
                            )
                    );
                    if (entity.groups.putIfAbsent(
                            group.interfaceGroup().code(),
                            group.interfaceGroup()
                    ) != null) {
                        throw new IllegalArgumentException(
                                "duplicate interface group "
                                        + group.interfaceGroup().code()
                        );
                    }
                });
        return businesses.values()
                .stream()
                .map(MutableBusiness::freeze)
                .toList();
    }

    public byte[] bytes(Object value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException(
                    "gateway interface report cannot be serialized",
                    failure
            );
        }
    }

    public ObjectMapper objectMapper() {
        return objectMapper.copy();
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

    public record BuiltReport(
            GatewayInterfaceDefinitionReport report,
            GatewayDefinitionIdentity identity,
            byte[] payload
    ) {

        public BuiltReport {
            payload = payload.clone();
        }

        @Override
        public byte[] payload() {
            return payload.clone();
        }
    }

    private static final class MutableBusiness {

        private final String code;

        private final String name;

        private final String description;

        private final Map<String, MutableEntity> entities =
                new LinkedHashMap<>();

        private MutableBusiness(
                String code,
                String name,
                String description) {
            this.code = code;
            this.name = name;
            this.description = description;
        }

        private GatewayInterfaceDefinitionReport.BusinessDomain freeze() {
            return new GatewayInterfaceDefinitionReport.BusinessDomain(
                    code,
                    name,
                    description,
                    entities.values().stream()
                            .map(MutableEntity::freeze)
                            .toList()
            );
        }
    }

    private static final class MutableEntity {

        private final String code;

        private final String name;

        private final String description;

        private final Map<String,
                GatewayInterfaceDefinitionReport.InterfaceGroup> groups =
                new LinkedHashMap<>();

        private MutableEntity(
                String code,
                String name,
                String description) {
            this.code = code;
            this.name = name;
            this.description = description;
        }

        private GatewayInterfaceDefinitionReport.EntityDomain freeze() {
            return new GatewayInterfaceDefinitionReport.EntityDomain(
                    code,
                    name,
                    description,
                    new ArrayList<>(groups.values())
            );
        }
    }
}
