package top.egon.cola.component.gateway.contract.reporting;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Starter 发现到的完整 Gateway 接口定义报告。
 *
 * <p>报告按业务域、实体域、接口分组和操作组织，HTTP 与 RPC 共用该目录模型；操作的
 * {@code attributes} 可携带 MCP 暴露元数据等扩展信息。
 */
public record GatewayInterfaceDefinitionReport(
        String contractVersion,
        String reportId,
        Instant reportedAt,
        Application application,
        Build build,
        boolean complete,
        String definitionSetId,
        String definitionFingerprint,
        List<BusinessDomain> businessDomains
) {

    public GatewayInterfaceDefinitionReport {
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
                BusinessDomain::code
        );
    }

    /**
     * 产生接口报告的业务应用身份和部署位置。
     */
    public record Application(
            String bizCode,
            String applicationCode,
            String name,
            String env,
            String namespace
    ) {

        public Application {
            bizCode = required(bizCode, "bizCode");
            applicationCode = required(
                    applicationCode,
                    "applicationCode"
            );
            name = required(name, "name");
            env = required(env, "env");
            namespace = required(namespace, "namespace");
        }
    }

    /**
     * 产生报告的构建版本及可审计构建元数据。
     */
    public record Build(
            String artifactVersion,
            String buildId,
            Map<String, String> metadata
    ) {

        public Build {
            artifactVersion = required(
                    artifactVersion,
                    "artifactVersion"
            );
            buildId = required(buildId, "buildId");
            metadata = Map.copyOf(Objects.requireNonNull(
                    metadata,
                    "metadata"
            ));
        }
    }

    /**
     * 业务域目录节点，包含该域下的实体域。
     */
    public record BusinessDomain(
            String code,
            String name,
            String description,
            List<EntityDomain> entityDomains
    ) {

        public BusinessDomain {
            code = required(code, "businessDomain.code");
            name = required(name, "businessDomain.name");
            entityDomains = sorted(
                    entityDomains,
                    EntityDomain::code
            );
        }
    }

    /**
     * 实体域目录节点，包含接口分组。
     */
    public record EntityDomain(
            String code,
            String name,
            String description,
            List<InterfaceGroup> interfaceGroups
    ) {

        public EntityDomain {
            code = required(code, "entityDomain.code");
            name = required(name, "entityDomain.name");
            interfaceGroups = sorted(
                    interfaceGroups,
                    InterfaceGroup::code
            );
        }
    }

    /**
     * 接口分组节点，描述 Controller 或 RPC Contract 的协议边界及其操作。
     */
    public record InterfaceGroup(
            String code,
            String name,
            String description,
            String sourceType,
            String className,
            String protocol,
            Map<String, Object> attributes,
            List<Operation> operations
    ) {

        public InterfaceGroup {
            code = required(code, "interfaceGroup.code");
            name = required(name, "interfaceGroup.name");
            sourceType = required(sourceType, "interfaceGroup.sourceType");
            protocol = required(protocol, "interfaceGroup.protocol");
            attributes = Map.copyOf(Objects.requireNonNull(
                    attributes,
                    "interfaceGroup.attributes"
            ));
            operations = sorted(operations, Operation::operationKey);
        }
    }

    /**
     * 可被路由和暴露的单个 Gateway 操作定义。
     *
     * <p>这里同时保存接口文档 Schema、provider 身份、访问属性和 RPC Descriptor 快照，
     * 使 HTTP、RPC 以及自动 MCP 工具都引用同一份操作事实。
     */
    public record Operation(
            String operationKey,
            String protocol,
            String methodIdentity,
            String name,
            String summary,
            String description,
            String owner,
            List<String> tags,
            boolean externalAccessible,
            String gatewaySupport,
            ProviderService providerService,
            Map<String, Object> requestSchema,
            Map<String, Object> responseSchema,
            List<Map<String, Object>> errorSchema,
            Map<String, Object> descriptorSnapshot,
            Map<String, Object> attributes,
            boolean deprecated
    ) {

        public Operation {
            operationKey = required(operationKey, "operationKey");
            protocol = required(protocol, "protocol");
            methodIdentity = required(methodIdentity, "methodIdentity");
            gatewaySupport = required(gatewaySupport, "gatewaySupport");
            providerService = Objects.requireNonNull(
                    providerService,
                    "providerService"
            );
            tags = sortedStrings(tags);
            requestSchema = Map.copyOf(Objects.requireNonNull(
                    requestSchema,
                    "requestSchema"
            ));
            responseSchema = Map.copyOf(Objects.requireNonNull(
                    responseSchema,
                    "responseSchema"
            ));
            errorSchema = List.copyOf(Objects.requireNonNull(
                    errorSchema,
                    "errorSchema"
            ));
            descriptorSnapshot = descriptorSnapshot == null
                    ? null
                    : Map.copyOf(descriptorSnapshot);
            attributes = Map.copyOf(Objects.requireNonNull(
                    attributes,
                    "attributes"
            ));
        }
    }

    /**
     * 操作实际提供方服务的定位信息。
     */
    public record ProviderService(
            String bizCode,
            String appCode,
            String env,
            String namespace,
            String protocol,
            String serviceName,
            String group,
            String version,
            String transport
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

    private static <T> List<T> sorted(
            List<T> values,
            java.util.function.Function<T, String> key) {
        return Objects.requireNonNull(values, "definition list")
                .stream()
                .sorted(Comparator.comparing(key))
                .toList();
    }

    private static List<String> sortedStrings(List<String> values) {
        return Objects.requireNonNull(values, "string list")
                .stream()
                .sorted()
                .toList();
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
