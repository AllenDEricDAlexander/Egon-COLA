package top.egon.cola.component.yuheng.admin.reporting.service;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import top.egon.cola.component.yuheng.contract.reporting.GatewayInterfaceDefinitionReport;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.List;
import java.util.Objects;

/**
 * 中文说明：{@code GatewayReportCanonicalizer} 是类型，位于当前 Gateway 模块的相关包中，负责网关报告Canonicalizer相关的职责与边界。
 * English summary: {@code GatewayReportCanonicalizer} is a type in the current Gateway module; it owns the gateway report canonicalizer-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayReportCanonicalizer {

    /**
     * 中文说明：保存 映射器 对应的状态、依赖或配置值；字段类型为 {@code com.fasterxml.jackson.databind.ObjectMapper}，由 {@code GatewayReportCanonicalizer} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by mapper; its type is {@code com.fasterxml.jackson.databind.ObjectMapper}, and {@code GatewayReportCanonicalizer} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayReportCanonicalizer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayReportCanonicalizer}; do not couple callers to its representation when the owning type exposes an API.
     */
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

    /**
     * 中文说明：执行 verify 操作；该方法是 {@code GatewayReportCanonicalizer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the verify operation; this method is the invocation entry point on {@code GatewayReportCanonicalizer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReportCanonicalizer.verify(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param report 参数 报告；parameter report。
     */
    public void verify(GatewayInterfaceDefinitionReport report) {
        Objects.requireNonNull(report, "report");
        String fingerprint = definitionFingerprint(
                report.application(),
                report.build(),
                report.complete(),
                report.businessDomains()
        );
        if (!fingerprint.equals(report.definitionFingerprint())) {
            throw new IllegalArgumentException(
                    "definitionFingerprint does not match canonical report"
            );
        }
        String definitionSetId = definitionSetId(
                report.application(),
                report.build(),
                fingerprint
        );
        if (!definitionSetId.equals(report.definitionSetId())) {
            throw new IllegalArgumentException(
                    "definitionSetId does not match canonical identity"
            );
        }
    }

    /**
     * Computes the canonical normalized Report v2 definition fingerprint.
     * Keeping this algorithm public lets aggregate coordinators derive one
     * immutable identity without duplicating Jackson canonicalization rules.
     */
    public String definitionFingerprint(
            GatewayInterfaceDefinitionReport.Application application,
            GatewayInterfaceDefinitionReport.Build build,
            boolean complete,
            List<GatewayInterfaceDefinitionReport.BusinessDomain>
                    businessDomains) {
        Objects.requireNonNull(application, "application");
        Objects.requireNonNull(build, "build");
        Objects.requireNonNull(businessDomains, "businessDomains");
        return sha256(bytes(Map.of(
                "application", application,
                "build", build,
                "businessDomains", businessDomains,
                "complete", complete,
                "definitionSchemaVersion", "v2"
        )));
    }

    /** Computes the canonical Definition Set id for an application/build. */
    public String definitionSetId(
            GatewayInterfaceDefinitionReport.Application application,
            GatewayInterfaceDefinitionReport.Build build,
            String fingerprint) {
        Objects.requireNonNull(application, "application");
        Objects.requireNonNull(build, "build");
        String normalizedFingerprint = Objects.requireNonNull(
                fingerprint,
                "fingerprint"
        );
        return sha256(String.join(
                "\n",
                application.bizCode(),
                application.applicationCode(),
                application.env(),
                application.namespace(),
                build.artifactVersion(),
                build.buildId(),
                normalizedFingerprint
        ).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 中文说明：执行 payloadSha256 操作；该方法是 {@code GatewayReportCanonicalizer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the payload sha256 operation; this method is the invocation entry point on {@code GatewayReportCanonicalizer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReportCanonicalizer.payloadSha256(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param report 参数 报告；parameter report。
     * @return 返回 payloadSha256 的处理结果；returns the result of the operation.
     */
    public String payloadSha256(GatewayInterfaceDefinitionReport report) {
        return sha256(bytes(report));
    }

    /**
     * 中文说明：执行 bytes 操作；该方法是 {@code GatewayReportCanonicalizer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the bytes operation; this method is the invocation entry point on {@code GatewayReportCanonicalizer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReportCanonicalizer.bytes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 bytes 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 sha256 操作；该方法是 {@code GatewayReportCanonicalizer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the sha256 operation; this method is the invocation entry point on {@code GatewayReportCanonicalizer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayReportCanonicalizer.sha256(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 sha256 的处理结果；returns the result of the operation.
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
}
