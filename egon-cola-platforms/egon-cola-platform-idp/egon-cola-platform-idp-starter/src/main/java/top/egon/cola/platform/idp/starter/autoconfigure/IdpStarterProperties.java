package top.egon.cola.platform.idp.starter.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 描述普通 Servlet 资源服务器接入统一 IdP 时使用的配置。
 * 配置决定受信任的签发方、公钥来源、受众与客户端范围，以及用户实时状态的 Redis 键空间。
 *
 * <p>Describes the configuration used when a regular Servlet resource server integrates with
 * the unified IdP. The settings define the trusted issuer, public-key source, accepted audiences
 * and clients, and the Redis key space containing current user state.</p>
 */
@ConfigurationProperties("egon.cola.platform.idp")
public class IdpStarterProperties {

    /**
     * 是否启用 IdP Starter 自动装配。
     *
     * <p>Whether IdP Starter auto-configuration is enabled.</p>
     */
    private boolean enabled;

    /**
     * 是否把身份过滤器直接注册到 Servlet 容器。
     *
     * <p>Whether the identity filter is registered directly with the Servlet container.</p>
     */
    private boolean registerFilter = true;

    /**
     * 受信任 JWT 的签发方标识。
     *
     * <p>Issuer identifier required on trusted JWTs.</p>
     */
    private String issuer;

    /**
     * 获取 JWT 验签公钥的 JWK Set 地址。
     *
     * <p>JWK Set endpoint used to obtain JWT verification keys.</p>
     */
    private String jwkSetUri;

    /**
     * 当前资源服务器接受的 JWT 受众集合。
     *
     * <p>JWT audiences accepted by the current resource server.</p>
     */
    private Set<String> audiences = new LinkedHashSet<>();

    /**
     * 允许向当前资源服务器提交令牌的 OAuth 客户端标识集合。
     *
     * <p>OAuth client identifiers whose tokens may be submitted to this resource server.</p>
     */
    private Set<String> clientIds = new LinkedHashSet<>();

    /**
     * IdP 用户实时状态在 Redis 中使用的键前缀。
     *
     * <p>Redis key prefix used for current IdP user-state projections.</p>
     */
    private String userStateKeyPrefix = "identity:v1:user:";

    /**
     * 创建使用默认值初始化的 IdP Starter 配置。
     *
     * <p>Creates IdP Starter settings initialized with their defaults.</p>
     */
    public IdpStarterProperties() {
    }

    /**
     * 返回是否启用 Starter。
     *
     * <p>Returns whether the Starter is enabled.</p>
     *
     * @return {@code true} 表示启用；{@code true} when enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用 Starter。
     *
     * <p>Sets whether the Starter is enabled.</p>
     *
     * @param enabled 是否启用；whether to enable the Starter
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 返回是否由 Starter 注册 Servlet Filter。
     *
     * <p>Returns whether the Starter registers its Servlet filter.</p>
     *
     * @return {@code true} 表示自动注册；{@code true} when automatic registration is enabled
     */
    public boolean isRegisterFilter() {
        return registerFilter;
    }

    /**
     * 设置是否由 Starter 注册 Servlet Filter。
     *
     * <p>Sets whether the Starter registers its Servlet filter.</p>
     *
     * @param registerFilter 是否自动注册；whether automatic registration is enabled
     */
    public void setRegisterFilter(boolean registerFilter) {
        this.registerFilter = registerFilter;
    }

    /**
     * 返回受信任签发方。
     *
     * <p>Returns the trusted issuer.</p>
     *
     * @return 签发方标识；issuer identifier
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * 设置受信任签发方。
     *
     * <p>Sets the trusted issuer.</p>
     *
     * @param issuer 签发方标识；issuer identifier
     */
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    /**
     * 返回 JWK Set 地址。
     *
     * <p>Returns the JWK Set endpoint.</p>
     *
     * @return JWK Set 地址；JWK Set endpoint
     */
    public String getJwkSetUri() {
        return jwkSetUri;
    }

    /**
     * 设置 JWK Set 地址。
     *
     * <p>Sets the JWK Set endpoint.</p>
     *
     * @param jwkSetUri JWK Set 地址；JWK Set endpoint
     */
    public void setJwkSetUri(String jwkSetUri) {
        this.jwkSetUri = jwkSetUri;
    }

    /**
     * 返回允许的受众集合。
     *
     * <p>Returns the accepted audience set.</p>
     *
     * @return 允许的受众；accepted audiences
     */
    public Set<String> getAudiences() {
        return audiences;
    }

    /**
     * 设置允许的受众集合。
     *
     * <p>Sets the accepted audience set.</p>
     *
     * @param audiences 允许的受众；accepted audiences
     */
    public void setAudiences(Set<String> audiences) {
        this.audiences = audiences;
    }

    /**
     * 返回允许的 OAuth 客户端标识集合。
     *
     * <p>Returns the accepted OAuth client identifiers.</p>
     *
     * @return 客户端标识集合；accepted client identifiers
     */
    public Set<String> getClientIds() {
        return clientIds;
    }

    /**
     * 设置允许的 OAuth 客户端标识集合。
     *
     * <p>Sets the accepted OAuth client identifiers.</p>
     *
     * @param clientIds 客户端标识集合；accepted client identifiers
     */
    public void setClientIds(Set<String> clientIds) {
        this.clientIds = clientIds;
    }

    /**
     * 返回用户实时状态的 Redis 键前缀。
     *
     * <p>Returns the Redis key prefix for current user state.</p>
     *
     * @return Redis 键前缀；Redis key prefix
     */
    public String getUserStateKeyPrefix() {
        return userStateKeyPrefix;
    }

    /**
     * 设置用户实时状态的 Redis 键前缀。
     *
     * <p>Sets the Redis key prefix for current user state.</p>
     *
     * @param userStateKeyPrefix Redis 键前缀；Redis key prefix
     */
    public void setUserStateKeyPrefix(String userStateKeyPrefix) {
        this.userStateKeyPrefix = userStateKeyPrefix;
    }

    /**
     * 校验启用身份验证所必需的全部配置。
     *
     * <p>Validates all settings required to enable identity verification.</p>
     *
     * @throws IllegalStateException 当必要配置缺失或集合包含空值时；when a required setting is
     *                               missing or a configured set contains a blank value
     */
    public void validate() {
        required(issuer, "issuer");
        required(jwkSetUri, "jwkSetUri");
        required(userStateKeyPrefix, "userStateKeyPrefix");
        requiredValues(audiences, "audiences");
        requiredValues(clientIds, "clientIds");
    }

    /**
     * 校验单个文本配置不为空。
     *
     * <p>Validates that a scalar text setting is not blank.</p>
     *
     * @param value 配置值；setting value
     * @param field 配置字段名；setting field name
     * @throws IllegalStateException 当配置为空时；when the setting is blank
     */
    private void required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "egon.cola.platform.idp." + field + " is required");
        }
    }

    /**
     * 校验配置集合非空且不包含空白元素。
     *
     * <p>Validates that a configured set is non-empty and contains no blank elements.</p>
     *
     * @param values 配置值集合；configured values
     * @param field  配置字段名；setting field name
     * @throws IllegalStateException 当集合为空或包含空白元素时；when the set is empty or contains a
     *                               blank element
     */
    private void requiredValues(Set<String> values, String field) {
        if (values == null || values.isEmpty()
                || values.stream().anyMatch(
                        value -> value == null || value.isBlank())) {
            throw new IllegalStateException(
                    "egon.cola.platform.idp." + field
                            + " must contain non-blank values");
        }
    }
}
