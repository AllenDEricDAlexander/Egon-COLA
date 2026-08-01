package top.egon.cola.component.gateway.contract.reporting;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
            List<Parameter> parameters,
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
            parameters = sorted(parameters, parameter ->
                    parameter.location() + ":" + parameter.name());
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

    public record Parameter(
            String name,
            String location,
            boolean required,
            String javaTypeDisplay,
            Map<String, Object> schema,
            String defaultValue,
            Map<String, Object> constraints,
            String description
    ) {

        public Parameter {
            name = GatewayInterfaceDefinitionReport.required(
                    name,
                    "parameter.name"
            );
            location = GatewayInterfaceDefinitionReport.required(
                    location,
                    "parameter.location"
            );
            javaTypeDisplay = GatewayInterfaceDefinitionReport.required(
                    javaTypeDisplay,
                    "parameter.javaTypeDisplay"
            );
            schema = Map.copyOf(Objects.requireNonNull(schema, "schema"));
            constraints = Map.copyOf(Objects.requireNonNull(
                    constraints,
                    "constraints"
            ));
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
