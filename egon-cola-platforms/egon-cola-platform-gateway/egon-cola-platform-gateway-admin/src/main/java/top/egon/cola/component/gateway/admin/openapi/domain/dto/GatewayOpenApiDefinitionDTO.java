package top.egon.cola.component.gateway.admin.openapi.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import top.egon.cola.component.gateway.contract.reporting.GatewayDefinitionSourceTypeEnum;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Normalized OpenAPI definition graph used as the MapStruct input to Report
 * v2.
 *
 * <p>中文：该 DTO 隔离 OpenAPI/Jackson transport tree 与 Gateway contract。
 * Adapter 负责图遍历和 schema 归一化，MapStruct 只负责同构结构转换。</p>
 */
public record GatewayOpenApiDefinitionDTO(
        @NotBlank
        @Pattern(regexp = "[0-9a-f]{64}")
        String canonicalSha256,
        @NotBlank String contractVersion,
        @NotBlank String reportId,
        @NotNull Instant reportedAt,
        @Valid @NotNull Application application,
        @Valid @NotNull Build build,
        boolean complete,
        @NotBlank String definitionSetId,
        @NotBlank String definitionFingerprint,
        @NotEmpty List<@Valid BusinessDomain> businessDomains
) {

    public GatewayOpenApiDefinitionDTO {
        canonicalSha256 = requiredHash(canonicalSha256);
        contractVersion = required(contractVersion, "contractVersion");
        if (!"v2".equals(contractVersion)) {
            throw new IllegalArgumentException(
                    "unsupported gateway reporting contract version: "
                            + contractVersion
            );
        }
        reportId = required(reportId, "reportId");
        reportedAt = Objects.requireNonNull(reportedAt, "reportedAt");
        application = Objects.requireNonNull(application, "application");
        build = Objects.requireNonNull(build, "build");
        definitionSetId = required(definitionSetId, "definitionSetId");
        definitionFingerprint = required(
                definitionFingerprint,
                "definitionFingerprint"
        );
        businessDomains = sorted(
                businessDomains,
                BusinessDomain::code,
                "businessDomains"
        );
    }

    /** Normalized report application identity. */
    public record Application(
            @NotBlank String bizCode,
            @NotBlank String applicationCode,
            @NotBlank String name,
            @NotBlank String env,
            @NotBlank String namespace
    ) {

        public Application {
            bizCode = required(bizCode, "application.bizCode");
            applicationCode = required(
                    applicationCode,
                    "application.applicationCode"
            );
            name = required(name, "application.name");
            env = required(env, "application.env");
            namespace = required(namespace, "application.namespace");
        }
    }

    /** Immutable build identity and canonical provenance metadata. */
    public record Build(
            @NotBlank String artifactVersion,
            @NotBlank String buildId,
            @NotNull Map<String, String> metadata
    ) {

        public Build {
            artifactVersion = required(
                    artifactVersion,
                    "build.artifactVersion"
            );
            buildId = required(buildId, "build.buildId");
            metadata = immutableStringMap(metadata, "build.metadata");
        }
    }

    /** Business-domain node in the normalized Report v2 graph. */
    public record BusinessDomain(
            @NotBlank String code,
            @NotBlank String name,
            String description,
            @NotEmpty List<@Valid EntityDomain> entityDomains
    ) {

        public BusinessDomain {
            code = required(code, "businessDomain.code");
            name = required(name, "businessDomain.name");
            entityDomains = sorted(
                    entityDomains,
                    EntityDomain::code,
                    "businessDomain.entityDomains"
            );
        }
    }

    /** Entity-domain node in the normalized Report v2 graph. */
    public record EntityDomain(
            @NotBlank String code,
            @NotBlank String name,
            String description,
            @NotEmpty List<@Valid InterfaceGroup> interfaceGroups
    ) {

        public EntityDomain {
            code = required(code, "entityDomain.code");
            name = required(name, "entityDomain.name");
            interfaceGroups = sorted(
                    interfaceGroups,
                    InterfaceGroup::code,
                    "entityDomain.interfaceGroups"
            );
        }
    }

    /** One OpenAPI Group mapped to the shared Gateway catalog model. */
    public record InterfaceGroup(
            @NotBlank String code,
            @NotBlank String name,
            String description,
            @NotNull GatewayDefinitionSourceTypeEnum sourceType,
            String className,
            @NotBlank String protocol,
            @NotNull Map<String, Object> attributes,
            @NotEmpty List<@Valid Operation> operations
    ) {

        public InterfaceGroup {
            code = required(code, "interfaceGroup.code");
            name = required(name, "interfaceGroup.name");
            sourceType = Objects.requireNonNull(
                    sourceType,
                    "interfaceGroup.sourceType"
            );
            protocol = required(protocol, "interfaceGroup.protocol");
            if (!"HTTP".equals(protocol)) {
                throw new IllegalArgumentException(
                        "OpenAPI definition groups must use HTTP protocol"
                );
            }
            if (sourceType != GatewayDefinitionSourceTypeEnum.OPENAPI31) {
                throw new IllegalArgumentException(
                        "OpenAPI definition groups must use OPENAPI31 source"
                );
            }
            attributes = immutableMap(
                    attributes,
                    "interfaceGroup.attributes"
            );
            operations = sorted(
                    operations,
                    Operation::operationKey,
                    "interfaceGroup.operations"
            );
        }
    }

    /** One normalized HTTP operation and its invocation schemas. */
    public record Operation(
            @NotBlank String operationKey,
            @NotBlank String protocol,
            @NotBlank String methodIdentity,
            @NotBlank String name,
            String summary,
            String description,
            String owner,
            @NotNull List<String> tags,
            boolean externalAccessible,
            @NotBlank String gatewaySupport,
            @Valid @NotNull ProviderService providerService,
            @NotNull Map<String, Object> requestSchema,
            @NotNull Map<String, Object> responseSchema,
            @NotNull List<Map<String, Object>> errorSchema,
            Map<String, Object> descriptorSnapshot,
            @NotNull Map<String, Object> attributes,
            boolean deprecated
    ) {

        public Operation {
            operationKey = required(operationKey, "operation.operationKey");
            protocol = required(protocol, "operation.protocol");
            methodIdentity = required(
                    methodIdentity,
                    "operation.methodIdentity"
            );
            name = required(name, "operation.name");
            gatewaySupport = required(
                    gatewaySupport,
                    "operation.gatewaySupport"
            );
            providerService = Objects.requireNonNull(
                    providerService,
                    "operation.providerService"
            );
            tags = Objects.requireNonNull(tags, "operation.tags")
                    .stream()
                    .map(value -> required(value, "operation.tag"))
                    .sorted()
                    .toList();
            requestSchema = immutableMap(
                    requestSchema,
                    "operation.requestSchema"
            );
            responseSchema = immutableMap(
                    responseSchema,
                    "operation.responseSchema"
            );
            errorSchema = Objects.requireNonNull(
                    errorSchema,
                    "operation.errorSchema"
            ).stream()
                    .map(value -> immutableMap(value, "operation.errorSchema"))
                    .toList();
            descriptorSnapshot = descriptorSnapshot == null
                    ? null
                    : immutableMap(
                            descriptorSnapshot,
                            "operation.descriptorSnapshot"
                    );
            attributes = immutableMap(
                    attributes,
                    "operation.attributes"
            );
        }
    }

    /** Provider service identity carried by the normalized operation. */
    public record ProviderService(
            @NotBlank String bizCode,
            @NotBlank String appCode,
            @NotBlank String env,
            @NotBlank String namespace,
            @NotBlank String protocol,
            @NotBlank String serviceName,
            @NotBlank String group,
            @NotBlank String version,
            @NotBlank String transport
    ) {

        public ProviderService {
            bizCode = required(bizCode, "providerService.bizCode");
            appCode = required(appCode, "providerService.appCode");
            env = required(env, "providerService.env");
            namespace = required(namespace, "providerService.namespace");
            protocol = required(protocol, "providerService.protocol");
            serviceName = required(
                    serviceName,
                    "providerService.serviceName"
            );
            group = required(group, "providerService.group");
            version = required(version, "providerService.version");
            transport = required(transport, "providerService.transport");
        }
    }

    private static String requiredHash(String value) {
        String normalized = required(value, "canonicalSha256");
        if (!normalized.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(
                    "canonicalSha256 must be lowercase hexadecimal SHA-256"
            );
        }
        return normalized;
    }

    private static String required(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }

    private static <T> List<T> sorted(
            List<T> values,
            java.util.function.Function<T, String> key,
            String field) {
        Objects.requireNonNull(values, field);
        List<T> normalized = new ArrayList<>(values);
        normalized.sort(Comparator.comparing(value ->
                required(key.apply(value), field + ".key")));
        return List.copyOf(normalized);
    }

    private static Map<String, String> immutableStringMap(
            Map<String, String> values,
            String field) {
        Objects.requireNonNull(values, field);
        Map<String, String> normalized = new LinkedHashMap<>();
        values.forEach((key, value) -> normalized.put(
                required(key, field + ".key"),
                required(value, field + ".value")
        ));
        return Map.copyOf(normalized);
    }

    private static Map<String, Object> immutableMap(
            Map<String, Object> values,
            String field) {
        Objects.requireNonNull(values, field);
        Map<String, Object> normalized = new LinkedHashMap<>();
        values.forEach((key, value) -> normalized.put(
                required(key, field + ".key"),
                freeze(value, field + ".value")
        ));
        return Map.copyOf(normalized);
    }

    private static Object freeze(Object value, String field) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, nested) -> copy.put(
                    required(String.valueOf(key), field + ".key"),
                    freeze(nested, field + ".value")
            ));
            return Map.copyOf(copy);
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(nested -> freeze(nested, field + ".item"))
                    .toList();
        }
        return value;
    }
}
