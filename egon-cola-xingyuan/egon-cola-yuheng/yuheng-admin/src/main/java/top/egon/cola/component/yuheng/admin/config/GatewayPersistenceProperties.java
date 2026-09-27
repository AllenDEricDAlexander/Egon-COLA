package top.egon.cola.component.yuheng.admin.config;

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
 * 中文说明：{@code GatewayPersistenceProperties} 是 Admin 进程唯一的持久化部署身份配置：安装租户、身份租户、
 * 服务侧审计主体与被期望的受管 schema 版本/指纹。这里<b>没有</b>任何默认值，尤其没有 {@code tenantId=1} 之类的
 * 兜底租户——缺任一项时绑定阶段即失败关闭，而不是退化成共享租户或空库自举。
 * English summary: {@code GatewayPersistenceProperties} is the Admin process's only persistence deployment identity:
 * the installation tenant, the identity tenant, the service audit principal, and the expected managed schema
 * version/fingerprint. Nothing here carries a default and in particular there is no fallback such as
 * {@code tenantId=1}: a missing value fails closed at binding time instead of degrading into a shared tenant or a
 * self-bootstrapping empty database.
 *
 * 用法 / Usage: 由 {@code GatewayPersistenceConfiguration} 与 {@code GatewayManagedDdlConfiguration} 读取；
 * 业务代码不得据此推断租户，运行期租户始终来自 MDC 的 {@code tenantId}。
 */
@Validated
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@ConfigurationProperties(prefix = "yuheng.persistence", ignoreUnknownFields = false)
public class GatewayPersistenceProperties {

    /** 中文说明：{@code @EnableConfigurationProperties} 生成的 bean 名（{@code prefix-全限定类名}），注入点据此消歧。
     *  English summary: the bean name {@code @EnableConfigurationProperties} generates ({@code prefix-FQCN}) that injection points use to disambiguate. */
    public static final String BEAN_NAME = "yuheng.persistence-top.egon.cola.component.yuheng.admin.config.GatewayPersistenceProperties";

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
