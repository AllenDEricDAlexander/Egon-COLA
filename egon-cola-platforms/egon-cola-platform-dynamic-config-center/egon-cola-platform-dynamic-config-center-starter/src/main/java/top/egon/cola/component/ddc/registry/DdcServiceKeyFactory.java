package top.egon.cola.component.ddc.registry;

import top.egon.cola.component.ddc.config.DdcProperties;
import top.egon.cola.component.ddc.model.enums.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;

/**
 * 基于 SDK 配置构建注册中心服务键，使业务调用方无需重复传递业务与应用范围。
 * 注册中心物理身份始终为 biz-env-app；namespace 仅控制授权视图的可见性。
 * / Builds registry service keys from SDK configuration so business callers do
 * not repeat the business and application scope. Registry physical identity is
 * always biz-env-app; namespace only controls authorization-view visibility.
 */
public final class DdcServiceKeyFactory {

    /**
     * 提供当前客户端默认范围的 DDC 配置。 / DDC properties providing the current client's default scope.
     */
    private final DdcProperties properties;

    /**
     * 创建服务键工厂。
     * / Creates a service-key factory.
     *
     * @param properties 当前客户端的 DDC 配置 / DDC properties for the current client
     */
    public DdcServiceKeyFactory(DdcProperties properties) {
        this.properties = properties;
    }

    /**
     * 使用配置中的业务、环境和应用范围创建服务键。
     * / Creates a service key with the business, environment, and application scope from configuration.
     *
     * @param serviceKind 服务类型 / service kind
     * @param serviceName 服务名称 / service name
     * @param group       服务分组 / service group
     * @param version     服务版本 / service version
     * @param protocol    传输协议 / transport protocol
     * @return 已规范化服务键 / normalized service key
     * @throws IllegalStateException    配置缺少业务编码时抛出 / if the configured business code is missing
     * @throws IllegalArgumentException 服务键字段无效时抛出 / if a service-key field is invalid
     */
    public DdcServiceKey fromScope(
            DdcServiceKind serviceKind,
            String serviceName,
            String group,
            String version,
            String protocol) {
        return new DdcServiceKey(
                requireBizCode(),
                properties.getEnv(),
                properties.getAppCode(),
                serviceKind,
                serviceName,
                group,
                version,
                protocol
        );
    }

    /**
     * 使用配置中的业务与应用范围及指定环境创建服务键。
     * / Creates a service key with configured business and application scope and the specified environment.
     *
     * @param env         目标环境 / target environment
     * @param serviceKind 服务类型 / service kind
     * @param serviceName 服务名称 / service name
     * @param group       服务分组 / service group
     * @param version     服务版本 / service version
     * @param protocol    传输协议 / transport protocol
     * @return 已规范化服务键 / normalized service key
     * @throws IllegalStateException    配置缺少业务编码时抛出 / if the configured business code is missing
     * @throws IllegalArgumentException 服务键字段无效时抛出 / if a service-key field is invalid
     */
    public DdcServiceKey fromScope(
            String env,
            DdcServiceKind serviceKind,
            String serviceName,
            String group,
            String version,
            String protocol) {
        return new DdcServiceKey(
                requireBizCode(),
                env,
                properties.getAppCode(),
                serviceKind,
                serviceName,
                group,
                version,
                protocol
        );
    }

    /**
     * 使用显式目标范围创建服务键。
     * / Creates a service key with an explicit target scope.
     *
     * @param bizCode     目标业务编码 / target business code
     * @param appCode     目标应用编码 / target application code
     * @param env         目标环境 / target environment
     * @param serviceKind 服务类型 / service kind
     * @param serviceName 服务名称 / service name
     * @param group       服务分组 / service group
     * @param version     服务版本 / service version
     * @param protocol    传输协议 / transport protocol
     * @return 已规范化服务键 / normalized service key
     * @throws IllegalArgumentException 服务键字段无效时抛出 / if a service-key field is invalid
     */
    public DdcServiceKey fromTargetScope(
            String bizCode,
            String appCode,
            String env,
            DdcServiceKind serviceKind,
            String serviceName,
            String group,
            String version,
            String protocol) {
        return new DdcServiceKey(
                bizCode,
                env,
                appCode,
                serviceKind,
                serviceName,
                group,
                version,
                protocol
        );
    }

    /**
     * 使用旧版 namespace 参数和指定环境创建服务键；namespace 会被忽略。
     * / Creates a service key with the legacy namespace parameter and a specified environment;
     * the namespace is ignored.
     *
     * @param env         目标环境 / target environment
     * @param namespace   已忽略的授权视图 / ignored authorization view
     * @param serviceKind 服务类型 / service kind
     * @param serviceName 服务名称 / service name
     * @param group       服务分组 / service group
     * @param version     服务版本 / service version
     * @param protocol    传输协议 / transport protocol
     * @return 已规范化服务键 / normalized service key
     * @throws IllegalStateException    配置缺少业务编码时抛出 / if the configured business code is missing
     * @throws IllegalArgumentException 服务键字段无效时抛出 / if a service-key field is invalid
     * @deprecated namespace 不再属于物理服务键。 / namespace is no longer part of the physical service key.
     */
    @Deprecated(forRemoval = true)
    public DdcServiceKey fromScope(
            String env,
            String namespace,
            DdcServiceKind serviceKind,
            String serviceName,
            String group,
            String version,
            String protocol) {
        return fromScope(env, serviceKind, serviceName, group, version, protocol);
    }

    /**
     * 使用旧版 namespace 参数和显式目标范围创建服务键；namespace 会被忽略。
     * / Creates a service key with the legacy namespace parameter and an explicit target scope;
     * the namespace is ignored.
     *
     * @param bizCode     目标业务编码 / target business code
     * @param appCode     目标应用编码 / target application code
     * @param env         目标环境 / target environment
     * @param namespace   已忽略的授权视图 / ignored authorization view
     * @param serviceKind 服务类型 / service kind
     * @param serviceName 服务名称 / service name
     * @param group       服务分组 / service group
     * @param version     服务版本 / service version
     * @param protocol    传输协议 / transport protocol
     * @return 已规范化服务键 / normalized service key
     * @throws IllegalArgumentException 服务键字段无效时抛出 / if a service-key field is invalid
     * @deprecated namespace 不再属于物理服务键。 / namespace is no longer part of the physical service key.
     */
    @Deprecated(forRemoval = true)
    public DdcServiceKey fromTargetScope(
            String bizCode,
            String appCode,
            String env,
            String namespace,
            DdcServiceKind serviceKind,
            String serviceName,
            String group,
            String version,
            String protocol) {
        return fromTargetScope(
                bizCode, appCode, env, serviceKind, serviceName, group, version, protocol
        );
    }

    /**
     * 获取并规范化必填业务编码。
     * / Obtains and normalizes the required business code.
     *
     * @return 去除首尾空白的业务编码 / trimmed business code
     * @throws IllegalStateException 配置缺少业务编码时抛出 / if the configured business code is missing
     */
    private String requireBizCode() {
        String bizCode = properties.getBizCode();
        if (bizCode == null || bizCode.isBlank()) {
            throw new IllegalStateException(
                    "DDC biz-code is required: egon.cola.component.ddc.biz-code"
            );
        }
        return bizCode.trim();
    }
}
