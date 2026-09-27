package top.egon.cola.component.yuheng.mcp.engine.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 中文说明：{@code McpPersistenceProperties} 是 MCP 数据面（受管 schema 的消费方）的持久化部署身份配置：
 * 安装租户、身份租户与技术主体，以及它所期望对齐的受管 schema 版本/指纹。数据面<b>永不</b>执行 DDL，
 * 只在指纹与已应用历史匹配时才认为自身就绪；这里没有默认租户，缺项即失败关闭。
 * English summary: {@code McpPersistenceProperties} is the persistence deployment identity of the MCP data plane (a
 * consumer of the managed schema): installation tenant, identity tenant and technical principal, plus the managed
 * schema version/fingerprint it expects to align with. The data plane never runs DDL and only considers itself ready
 * once the fingerprint matches the applied history; there is no default tenant and a missing value fails closed.
 *
 * 用法 / Usage: 由 {@code McpPersistenceConfiguration} 读取；运行期租户仍来自 MDC 的 {@code tenantId}，
 * 本配置只描述部署身份与就绪判据。
 */
@Validated
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@ConfigurationProperties(prefix = "yuheng.persistence", ignoreUnknownFields = false)
public class McpPersistenceProperties {

    /** 中文说明：{@code @EnableConfigurationProperties} 生成的 bean 名（{@code prefix-全限定类名}），注入点据此消歧。
     *  English summary: the bean name {@code @EnableConfigurationProperties} generates ({@code prefix-FQCN}) that injection points use to disambiguate. */
    public static final String BEAN_NAME = "yuheng.persistence-top.egon.cola.component.yuheng.mcp.engine.config.McpPersistenceProperties";

    /** 中文说明：本安装域的租户 id，是写入 MDC 的唯一事实来源。 English summary: this installation's tenant id, the single source of truth written into MDC. */
    @NotNull
    @Positive
    private Long tenantId;

    /** 中文说明：被信任的身份租户，必须与已验证令牌的声明一致。 English summary: the trusted identity tenant a verified token claim must match. */
    @NotNull
    @Positive
    private Long identityTenantId;

    /** 中文说明：服务侧审计主体。 English summary: the service-side audit principal. */
    @NotBlank
    @Size(max = 128)
    private String serviceUserId;

    /** 中文说明：被期望的受管 schema 版本。 English summary: the expected managed schema version. */
    @NotBlank
    @Size(max = 64)
    private String expectedSchemaVersion;

    /** 中文说明：被期望的受管 schema 指纹。 English summary: the expected managed schema fingerprint. */
    @NotBlank
    @Pattern(regexp = "[0-9a-f]{64}")
    private String expectedSchemaSha256;

    /** 中文说明：本进程是否持有受管 DDL 职责，只有 Admin 可以为真。 English summary: whether this process owns managed DDL; only Admin may be true. */
    @NotNull
    private Boolean managedDdlEnabled;
}
