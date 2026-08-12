package top.egon.cola.component.gateway.admin.application.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.gateway.admin.application.GatewayAdminIdempotencyConflictException;
import top.egon.cola.component.gateway.admin.application.GatewayAdminNotFoundException;
import top.egon.cola.component.gateway.admin.application.IdempotencyStore;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReport;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReportResult;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 中文说明：{@code GatewayDefinitionReportService} 是服务组件，位于当前 Gateway 模块的相关包中，负责网关定义报告服务相关的职责与边界。
 * English summary: {@code GatewayDefinitionReportService} is a gateway definition report service service in the current Gateway module; it owns the gateway definition report service-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@Service
public class GatewayDefinitionReportService {

    /**
     * 中文说明：表示 IDEMPOTENCYSCOPE 这一固定值；它属于 {@code GatewayDefinitionReportService} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value idempotency scope; it is a state, type, or protocol value of {@code GatewayDefinitionReportService} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDefinitionReportService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDefinitionReportService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final String IDEMPOTENCY_SCOPE =
            "GATEWAY_DEFINITION_REPORT";

    /**
     * 中文说明：表示 MCPPERMISSION 这一固定值；它属于 {@code GatewayDefinitionReportService} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value mcp permission; it is a state, type, or protocol value of {@code GatewayDefinitionReportService} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDefinitionReportService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDefinitionReportService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Pattern MCP_PERMISSION = Pattern.compile(
            "^[a-z][a-z0-9._-]*(?::[A-Za-z0-9._*-]+)+$"
    );

    /**
     * 中文说明：保存 reports 对应的状态、依赖或配置值；字段类型为 {@code GatewayDefinitionReportStore}，由 {@code GatewayDefinitionReportService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by reports; its type is {@code GatewayDefinitionReportStore}, and {@code GatewayDefinitionReportService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDefinitionReportService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDefinitionReportService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayDefinitionReportStore reports;

    /**
     * 中文说明：保存 idempotency 对应的状态、依赖或配置值；字段类型为 {@code IdempotencyStore}，由 {@code GatewayDefinitionReportService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by idempotency; its type is {@code IdempotencyStore}, and {@code GatewayDefinitionReportService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDefinitionReportService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDefinitionReportService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final IdempotencyStore idempotency;

    /**
     * 中文说明：保存 object映射器 对应的状态、依赖或配置值；字段类型为 {@code ObjectMapper}，由 {@code GatewayDefinitionReportService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by object mapper; its type is {@code ObjectMapper}, and {@code GatewayDefinitionReportService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDefinitionReportService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDefinitionReportService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：保存 模式校验器 对应的状态、依赖或配置值；字段类型为 {@code GatewayOperationSchemaValidator}，由 {@code GatewayDefinitionReportService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by schema validator; its type is {@code GatewayOperationSchemaValidator}, and {@code GatewayDefinitionReportService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDefinitionReportService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDefinitionReportService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayOperationSchemaValidator schemaValidator;

    /**
     * 中文说明：保存 canonicalizer 对应的状态、依赖或配置值；字段类型为 {@code GatewayReportCanonicalizer}，由 {@code GatewayDefinitionReportService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by canonicalizer; its type is {@code GatewayReportCanonicalizer}, and {@code GatewayDefinitionReportService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDefinitionReportService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDefinitionReportService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayReportCanonicalizer canonicalizer =
            new GatewayReportCanonicalizer();

    /**
     * 中文说明：保存 clock 对应的状态、依赖或配置值；字段类型为 {@code Clock}，由 {@code GatewayDefinitionReportService} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clock; its type is {@code Clock}, and {@code GatewayDefinitionReportService} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayDefinitionReportService} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDefinitionReportService}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Clock clock;

    /**
     * 中文说明：创建 {@code GatewayDefinitionReportService} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayDefinitionReportService} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param reports 参数 reports；parameter reports。
     * @param idempotency 参数 idempotency；parameter idempotency。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     */
    @Autowired
    public GatewayDefinitionReportService(
            GatewayDefinitionReportStore reports,
            IdempotencyStore idempotency,
            ObjectMapper objectMapper) {
        this(reports, idempotency, objectMapper, Clock.systemUTC());
    }

    /**
     * 中文说明：创建 {@code GatewayDefinitionReportService} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayDefinitionReportService} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param reports 参数 reports；parameter reports。
     * @param idempotency 参数 idempotency；parameter idempotency。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @param clock 参数 clock；parameter clock。
     */
    GatewayDefinitionReportService(
            GatewayDefinitionReportStore reports,
            IdempotencyStore idempotency,
            ObjectMapper objectMapper,
            Clock clock) {
        this.reports = reports;
        this.idempotency = idempotency;
        this.objectMapper = objectMapper;
        this.schemaValidator = new GatewayOperationSchemaValidator(objectMapper);
        this.clock = clock;
    }

    /**
     * 中文说明：执行 accept 操作；该方法是 {@code GatewayDefinitionReportService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the accept operation; this method is the invocation entry point on {@code GatewayDefinitionReportService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionReportService.accept(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param authentication 参数 authentication；parameter authentication。
     * @param report 参数 报告；parameter report。
     * @param headerReportId 参数 header报告Id；parameter header report id。
     * @param headerContractVersion 参数 headerContractVersion；parameter header contract version。
     * @return 返回 accept 的处理结果；returns the result of the operation.
     */
    @Transactional
    public GatewayInterfaceDefinitionReportResult accept(
            GatewayReportAuthentication authentication,
            GatewayInterfaceDefinitionReport report,
            String headerReportId,
            String headerContractVersion) {
        validate(
                authentication,
                report,
                headerReportId,
                headerContractVersion
        );
        canonicalizer.verify(report);
        String payloadSha = canonicalizer.payloadSha256(report);
        IdempotencyStore.Record previous = idempotency.find(
                        IDEMPOTENCY_SCOPE,
                        authentication.applicationId(),
                        report.reportId()
                )
                .orElse(null);
        if (previous != null) {
            if (!previous.payloadSha256().equals(payloadSha)) {
                throw new GatewayAdminIdempotencyConflictException();
            }
            return objectMapper.convertValue(
                    previous.response(),
                    GatewayInterfaceDefinitionReportResult.class
            );
        }
        reports.findBuildFingerprint(
                        authentication.applicationId(),
                        report.build().buildId()
                )
                .filter(fingerprint -> !fingerprint.equals(
                        report.definitionFingerprint()
                ))
                .ifPresent(conflict -> {
                    throw new IllegalStateException(
                            "GATEWAY_ADMIN_IMMUTABLE_BUILD_CONFLICT: "
                                    + report.build().buildId()
                    );
                });
        GatewayInterfaceDefinitionReportResult result =
                reports.definitionSetExists(
                        authentication.applicationId(),
                        report.definitionSetId()
                )
                        ? alreadyVerified(authentication, report)
                        : ingest(authentication, report);
        idempotency.save(new IdempotencyStore.Record(
                IDEMPOTENCY_SCOPE,
                authentication.applicationId(),
                report.reportId(),
                payloadSha,
                report.definitionSetId(),
                objectMapper.convertValue(result, Map.class),
                result.receivedAt(),
                null
        ));
        return result;
    }

    /**
     * 中文说明：执行 find 操作；该方法是 {@code GatewayDefinitionReportService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find operation; this method is the invocation entry point on {@code GatewayDefinitionReportService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionReportService.find(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param authentication 参数 authentication；parameter authentication。
     * @param reportId 参数 报告Id；parameter report id。
     * @return 返回 find 的处理结果；returns the result of the operation.
     */
    @Transactional(readOnly = true)
    public GatewayInterfaceDefinitionReportResult find(
            GatewayReportAuthentication authentication,
            String reportId) {
        return idempotency.find(
                        IDEMPOTENCY_SCOPE,
                        authentication.applicationId(),
                        reportId
                )
                .map(record -> objectMapper.convertValue(
                        record.response(),
                        GatewayInterfaceDefinitionReportResult.class
                ))
                .orElseThrow(() -> new GatewayAdminNotFoundException(
                        "gateway interface report "
                                + reportId
                                + " was not found"
                ));
    }

    /**
     * 中文说明：执行 ingest 操作；该方法是 {@code GatewayDefinitionReportService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the ingest operation; this method is the invocation entry point on {@code GatewayDefinitionReportService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionReportService.ingest(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param authentication 参数 authentication；parameter authentication。
     * @param report 参数 报告；parameter report。
     * @return 返回 ingest 的处理结果；returns the result of the operation.
     */
    private GatewayInterfaceDefinitionReportResult ingest(
            GatewayReportAuthentication authentication,
            GatewayInterfaceDefinitionReport report) {
        int previousCount = reports.countStarterOperations(
                authentication.applicationId()
        );
        Instant now = clock.instant();
        GatewayDefinitionReportStore.StoredReport stored = reports.ingest(
                authentication.applicationId(),
                report,
                now
        );
        DefinitionCounts counts = counts(report);
        int missing = Math.max(0, previousCount - counts.operations);
        return new GatewayInterfaceDefinitionReportResult(
                report.reportId(),
                report.definitionSetId(),
                GatewayInterfaceDefinitionReportResult.Status.ACCEPTED,
                authentication.applicationId(),
                new GatewayInterfaceDefinitionReportResult.Counts(
                        counts.businesses,
                        counts.entities,
                        counts.groups,
                        counts.operations,
                        stored.created(),
                        stored.updated(),
                        missing
                ),
                stored.operationRefs(),
                missing == 0
                        ? List.of()
                        : List.of(warning(
                        "MISSING_FROM_THIS_SET",
                        "interfaces absent from this set are offlined after "
                                + "providers activate it"
                )),
                now
        );
    }

    /**
     * 中文说明：执行 alreadyVerified 操作；该方法是 {@code GatewayDefinitionReportService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the already verified operation; this method is the invocation entry point on {@code GatewayDefinitionReportService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionReportService.alreadyVerified(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param authentication 参数 authentication；parameter authentication。
     * @param report 参数 报告；parameter report。
     * @return 返回 alreadyVerified 的处理结果；returns the result of the operation.
     */
    private GatewayInterfaceDefinitionReportResult alreadyVerified(
            GatewayReportAuthentication authentication,
            GatewayInterfaceDefinitionReport report) {
        DefinitionCounts counts = counts(report);
        return new GatewayInterfaceDefinitionReportResult(
                report.reportId(),
                report.definitionSetId(),
                GatewayInterfaceDefinitionReportResult.Status
                        .ACCEPTED_WITH_WARNINGS,
                authentication.applicationId(),
                new GatewayInterfaceDefinitionReportResult.Counts(
                        counts.businesses,
                        counts.entities,
                        counts.groups,
                        counts.operations,
                        0,
                        0,
                        0
                ),
                List.of(),
                List.of(warning(
                        "DEFINITION_SET_ALREADY_VERIFIED",
                        "the immutable definition set already exists"
                )),
                clock.instant()
        );
    }

    /**
     * 中文说明：执行 validate 操作；该方法是 {@code GatewayDefinitionReportService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate operation; this method is the invocation entry point on {@code GatewayDefinitionReportService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionReportService.validate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param authentication 参数 authentication；parameter authentication。
     * @param report 参数 报告；parameter report。
     * @param headerReportId 参数 header报告Id；parameter header report id。
     * @param headerContractVersion 参数 headerContractVersion；parameter header contract version。
     */
    private void validate(
            GatewayReportAuthentication authentication,
            GatewayInterfaceDefinitionReport report,
            String headerReportId,
            String headerContractVersion) {
        if (!"v2".equals(headerContractVersion)
                || !"v2".equals(report.contractVersion())) {
            throw new IllegalArgumentException(
                    "unsupported gateway reporting contract version"
            );
        }
        if (!report.reportId().equals(headerReportId)) {
            throw new IllegalArgumentException(
                    "X-Gateway-Report-Id does not match request body"
            );
        }
        if (!authentication.bizCode().equals(report.application().bizCode())
                || !authentication.applicationCode().equals(
                report.application().applicationCode()
        )
                || !authentication.env().equals(report.application().env())
                || !authentication.namespace().equals(
                report.application().namespace()
        )) {
            throw new IllegalArgumentException(
                    "credential scope does not match report application"
            );
        }
        if (!report.complete()) {
            throw new IllegalArgumentException(
                    "only complete definition reports are supported"
            );
        }
        Set<String> operationKeys = new HashSet<>();
        report.businessDomains().forEach(business ->
                business.entityDomains().forEach(entity ->
                        entity.interfaceGroups().forEach(group ->
                                group.operations().forEach(operation -> {
                                    if (!operationKeys.add(
                                            operation.operationKey()
                                    )) {
                                        throw new IllegalArgumentException(
                                                "duplicate operationKey "
                                                        + operation
                                                        .operationKey()
                                        );
                                    }
                                    if (operation.externalAccessible()
                                            && !"SUPPORTED".equals(
                                            operation.gatewaySupport()
                                    )) {
                                        throw new IllegalArgumentException(
                                                "unsupported operation cannot "
                                                        + "be externally "
                                                        + "accessible"
                                        );
                                    }
                                    validateMcpExposure(operation);
                                    try {
                                        schemaValidator.validate(operation);
                                    } catch (IllegalArgumentException failure) {
                                        if (registeredForMcp(operation)) {
                                            invalidMcp(
                                                    operation,
                                                    failure.getMessage()
                                            );
                                        }
                                        throw failure;
                                    }
                                }))));
        validateCodes(report);
    }

    /**
     * 中文说明：执行 validateMCPExposure 操作；该方法是 {@code GatewayDefinitionReportService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate mcp exposure operation; this method is the invocation entry point on {@code GatewayDefinitionReportService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionReportService.validateMcpExposure(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operation 参数 操作；parameter operation。
     */
    private void validateMcpExposure(
            GatewayInterfaceDefinitionReport.Operation operation) {
        Object reported = operation.attributes().get("mcpExposure");
        if (reported == null) {
            return;
        }
        if (!(reported instanceof Map<?, ?>)) {
            invalidMcp(operation, "mcpExposure must be an object");
        }
        Map<?, ?> exposure = (Map<?, ?>) reported;
        if (!Boolean.TRUE.equals(exposure.get("registerMcp"))) {
            invalidMcp(operation, "registerMcp must be true");
        }
        requiredMcp(exposure, "mcpServerCode", operation);
        requiredMcp(exposure, "mcpName", operation);
        Object permissions = exposure.get("requiredPermissions");
        if (!(permissions instanceof List<?>)) {
            invalidMcp(operation, "requiredPermissions must be an array");
        }
        List<?> values = (List<?>) permissions;
        for (Object value : values) {
            if (!(value instanceof String permission)
                    || !MCP_PERMISSION.matcher(permission).matches()) {
                invalidMcp(operation, "invalid requiredPermissions value");
            }
        }
        Object risk = exposure.get("riskLevel");
        if (!(risk instanceof String riskName)
                || !Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL")
                .contains(riskName)) {
            invalidMcp(operation, "invalid riskLevel");
        }
        if (!(exposure.get("idempotent") instanceof Boolean)) {
            invalidMcp(operation, "idempotent must be boolean");
        }
        if (Boolean.TRUE.equals(operation.attributes().get("streaming"))) {
            invalidMcp(operation, "streaming operations are unsupported");
        }
    }

    /**
     * 中文说明：执行 registeredForMCP 操作；该方法是 {@code GatewayDefinitionReportService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the registered for mcp operation; this method is the invocation entry point on {@code GatewayDefinitionReportService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionReportService.registeredForMcp(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operation 参数 操作；parameter operation。
     * @return 返回 registeredForMCP 的处理结果；returns the result of the operation.
     */
    private boolean registeredForMcp(
            GatewayInterfaceDefinitionReport.Operation operation) {
        Object reported = operation.attributes().get("mcpExposure");
        return reported instanceof Map<?, ?> exposure
                && Boolean.TRUE.equals(exposure.get("registerMcp"));
    }

    /**
     * 中文说明：执行 requiredMCP 操作；该方法是 {@code GatewayDefinitionReportService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required mcp operation; this method is the invocation entry point on {@code GatewayDefinitionReportService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionReportService.requiredMcp(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param exposure 参数 exposure；parameter exposure。
     * @param field 参数 field；parameter field。
     * @param operation 参数 操作；parameter operation。
     */
    private void requiredMcp(
            Map<?, ?> exposure,
            String field,
            GatewayInterfaceDefinitionReport.Operation operation) {
        Object value = exposure.get(field);
        if (!(value instanceof String text) || text.isBlank()) {
            invalidMcp(operation, field + " is required");
        }
    }

    /**
     * 中文说明：执行 invalidMCP 操作；该方法是 {@code GatewayDefinitionReportService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the invalid mcp operation; this method is the invocation entry point on {@code GatewayDefinitionReportService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionReportService.invalidMcp(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param operation 参数 操作；parameter operation。
     * @param message 参数 消息；parameter message。
     */
    private void invalidMcp(
            GatewayInterfaceDefinitionReport.Operation operation,
            String message) {
        throw new IllegalArgumentException(
                "invalid mcpExposure for " + operation.operationKey()
                        + ": " + message
        );
    }

    /**
     * 中文说明：执行 validateCodes 操作；该方法是 {@code GatewayDefinitionReportService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate codes operation; this method is the invocation entry point on {@code GatewayDefinitionReportService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionReportService.validateCodes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param report 参数 报告；parameter report。
     */
    private void validateCodes(GatewayInterfaceDefinitionReport report) {
        String pattern = "[a-zA-Z0-9][a-zA-Z0-9._-]{0,127}";
        report.businessDomains().forEach(business -> {
            if (!business.code().matches(pattern)) {
                throw new IllegalArgumentException(
                        "invalid business domain code " + business.code()
                );
            }
            Set<String> entities = new LinkedHashSet<>();
            business.entityDomains().forEach(entity -> {
                if (!entity.code().matches(pattern)
                        || !entities.add(entity.code())) {
                    throw new IllegalArgumentException(
                            "invalid or duplicate entity domain code "
                                    + entity.code()
                    );
                }
                Set<String> groups = new LinkedHashSet<>();
                entity.interfaceGroups().forEach(group -> {
                    if (group.code().length() > 256
                            || !groups.add(group.code())) {
                        throw new IllegalArgumentException(
                                "invalid or duplicate interface group code "
                                        + group.code()
                        );
                    }
                });
            });
        });
    }

    /**
     * 中文说明：执行 counts 操作；该方法是 {@code GatewayDefinitionReportService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the counts operation; this method is the invocation entry point on {@code GatewayDefinitionReportService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionReportService.counts(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param report 参数 报告；parameter report。
     * @return 返回 counts 的处理结果；returns the result of the operation.
     */
    private DefinitionCounts counts(
            GatewayInterfaceDefinitionReport report) {
        int entities = 0;
        int groups = 0;
        int operations = 0;
        for (GatewayInterfaceDefinitionReport.BusinessDomain business
                : report.businessDomains()) {
            entities += business.entityDomains().size();
            for (GatewayInterfaceDefinitionReport.EntityDomain entity
                    : business.entityDomains()) {
                groups += entity.interfaceGroups().size();
                for (GatewayInterfaceDefinitionReport.InterfaceGroup group
                        : entity.interfaceGroups()) {
                    operations += group.operations().size();
                }
            }
        }
        return new DefinitionCounts(
                report.businessDomains().size(),
                entities,
                groups,
                operations
        );
    }

    /**
     * 中文说明：执行 warning 操作；该方法是 {@code GatewayDefinitionReportService} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the warning operation; this method is the invocation entry point on {@code GatewayDefinitionReportService} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionReportService.warning(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param code 参数 code；parameter code。
     * @param message 参数 消息；parameter message。
     * @return 返回 warning 的处理结果；returns the result of the operation.
     */
    private GatewayInterfaceDefinitionReportResult.Warning warning(
            String code,
            String message) {
        return new GatewayInterfaceDefinitionReportResult.Warning(
                "$",
                code,
                message
        );
    }

    /**
     * 中文说明：{@code DefinitionCounts} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责定义Counts相关的职责与边界。
     * English summary: {@code DefinitionCounts} is an immutable data carrier in the current Gateway module; it owns the definition counts-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param businesses 参数 businesses；parameter businesses。
     * @param entities 参数 entities；parameter entities。
     * @param groups 参数 groups；parameter groups。
     * @param operations 参数 operations；parameter operations。
     */
    private record DefinitionCounts(
            /**
             * 中文说明：保存 businesses 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayDefinitionReportService.DefinitionCounts} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by businesses; its type is {@code int}, and {@code GatewayDefinitionReportService.DefinitionCounts} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDefinitionReportService.DefinitionCounts} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDefinitionReportService.DefinitionCounts}; do not couple callers to its representation when the owning type exposes an API.
             */
            int businesses,
            /**
             * 中文说明：保存 entities 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayDefinitionReportService.DefinitionCounts} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by entities; its type is {@code int}, and {@code GatewayDefinitionReportService.DefinitionCounts} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDefinitionReportService.DefinitionCounts} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDefinitionReportService.DefinitionCounts}; do not couple callers to its representation when the owning type exposes an API.
             */
            int entities,
            /**
             * 中文说明：保存 groups 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayDefinitionReportService.DefinitionCounts} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by groups; its type is {@code int}, and {@code GatewayDefinitionReportService.DefinitionCounts} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDefinitionReportService.DefinitionCounts} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDefinitionReportService.DefinitionCounts}; do not couple callers to its representation when the owning type exposes an API.
             */
            int groups,
            /**
             * 中文说明：保存 operations 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayDefinitionReportService.DefinitionCounts} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by operations; its type is {@code int}, and {@code GatewayDefinitionReportService.DefinitionCounts} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDefinitionReportService.DefinitionCounts} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDefinitionReportService.DefinitionCounts}; do not couple callers to its representation when the owning type exposes an API.
             */
            int operations
    ) {
    }
}
