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

/**
 * Builds deterministic Gateway interface definition reports and their stable
 * identities from discovered interface groups.
 *
 * <p>中文：根据发现的接口分组构建确定性的网关接口定义报告及其稳定
 * 身份。
 */
public final class GatewayDefinitionReportFactory {

    /**
     * Application and build metadata included in generated reports.
     * 报告中包含的应用及构建元数据。
     */
    private final GatewayReportingProperties properties;

    /** Time source for report creation timestamps. 生成报告时间戳的时间源。 */
    private final Clock clock;

    /**
     * Deterministically configured mapper used for payload fingerprints.
     * 用于计算载荷指纹的确定性 JSON 映射器。
     */
    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .build();

    /**
     * Creates a report factory using the UTC system clock.
     * 中文：使用 UTC 系统时钟创建报告工厂。
     *
     * @param properties reporting application and build metadata
     */
    public GatewayDefinitionReportFactory(
            GatewayReportingProperties properties) {
        this(properties, Clock.systemUTC());
    }

    /**
     * Creates a report factory with an injectable time source.
     * 中文：使用可注入的时间源创建报告工厂，便于测试。
     *
     * @param properties reporting application and build metadata
     * @param clock report creation clock
     */
    GatewayDefinitionReportFactory(
            GatewayReportingProperties properties,
            Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Validates configuration and builds a deterministic complete report.
     * 中文：校验配置并构建完整且确定性的接口定义报告。
     *
     * @param groups discovered interface groups to aggregate by business and
     *               entity domain
     * @return report, stable identity, and serialized payload
     * @throws IllegalArgumentException if configuration, group uniqueness, or
     *                                  serialization is invalid
     */
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

    /**
     * Groups discovered interfaces into sorted business and entity domains.
     * 中文：将发现的接口按业务域和实体域分组并排序。
     *
     * @param groups discovered interface groups
     * @return immutable business domain definitions
     * @throws IllegalArgumentException if an entity contains a duplicate group
     *                                  code
     */
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

    /**
     * Serializes a value using the deterministic report mapper.
     * 中文：使用确定性映射器将对象序列化为报告载荷。
     *
     * @param value value to serialize
     * @return serialized JSON bytes
     * @throws IllegalArgumentException if the value cannot be serialized
     */
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

    /**
     * Returns an independent copy of the deterministic report mapper.
     * 中文：返回确定性报告映射器的独立副本。
     *
     * @return copied object mapper
     */
    public ObjectMapper objectMapper() {
        return objectMapper.copy();
    }

    /**
     * Computes a lowercase hexadecimal SHA-256 digest.
     * 中文：计算小写十六进制形式的 SHA-256 摘要。
     *
     * @param value bytes to digest
     * @return hexadecimal digest
     */
    private String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value)
            );
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    /**
     * Complete generated report together with its identity and JSON payload.
     * 中文：封装生成的完整报告、稳定身份以及 JSON 载荷。
     *
     * @param report structured report
     * @param identity stable definition identity
     * @param payload serialized report payload
     */
    public record BuiltReport(
            GatewayInterfaceDefinitionReport report,
            GatewayDefinitionIdentity identity,
            byte[] payload
    ) {

        /**
         * Defensively copies the serialized payload at construction time.
         * 在构造时对序列化载荷执行防御性复制。
         */
        public BuiltReport {
            payload = payload.clone();
        }

        /**
         * Returns a defensive copy of the serialized report payload.
         * 中文：返回序列化报告载荷的防御性副本。
         *
         * @return copied payload bytes
         */
        @Override
        public byte[] payload() {
            return payload.clone();
        }
    }

    /**
     * Mutable aggregation node used while assembling a business domain.
     * 构建业务域时使用的可变聚合节点。
     */
    private static final class MutableBusiness {

        /** Stable business domain code. 稳定的业务域编码。 */
        private final String code;

        /** Human-readable business domain name. 可读的业务域名称。 */
        private final String name;

        /** Business domain description. 业务域描述。 */
        private final String description;

        /**
         * Entity domains indexed by stable code in encounter order.
         * 按稳定编码索引且保持发现顺序的实体域。
         */
        private final Map<String, MutableEntity> entities =
                new LinkedHashMap<>();

        /**
         * Creates a mutable business aggregation node.
         * 中文：创建业务域的可变聚合节点。
         *
         * @param code stable business domain code
         * @param name human-readable business domain name
         * @param description business domain description
         */
        private MutableBusiness(
                String code,
                String name,
                String description) {
            this.code = code;
            this.name = name;
            this.description = description;
        }

        /**
         * Freezes the accumulated entity domains into the report contract.
         * 中文：将已聚合的实体域冻结为报告契约对象。
         *
         * @return immutable business domain definition
         */
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

    /**
     * Mutable aggregation node used while assembling an entity domain.
     * 构建实体域时使用的可变聚合节点。
     */
    private static final class MutableEntity {

        /** Stable entity domain code. 稳定的实体域编码。 */
        private final String code;

        /** Human-readable entity domain name. 可读的实体域名称。 */
        private final String name;

        /** Entity domain description. 实体域描述。 */
        private final String description;

        /**
         * Interface groups indexed by stable code in encounter order.
         * 按稳定编码索引且保持发现顺序的接口分组。
         */
        private final Map<String,
                GatewayInterfaceDefinitionReport.InterfaceGroup> groups =
                new LinkedHashMap<>();

        /**
         * Creates a mutable entity aggregation node.
         * 中文：创建实体域的可变聚合节点。
         *
         * @param code stable entity domain code
         * @param name human-readable entity domain name
         * @param description entity domain description
         */
        private MutableEntity(
                String code,
                String name,
                String description) {
            this.code = code;
            this.name = name;
            this.description = description;
        }

        /**
         * Freezes the accumulated groups into the report contract.
         * 中文：将已聚合的接口分组冻结为报告契约对象。
         *
         * @return immutable entity domain definition
         */
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
