package top.egon.cola.component.ddc.registry;

import top.egon.cola.component.ddc.config.DdcProperties;
import top.egon.cola.component.ddc.model.enums.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;

/**
 * Builds registry service keys from the SDK configuration so business callers
 * do not carry the biz/app scope themselves. The registry identity is always
 * biz-env-app; namespace only controls which authorization views expose it.
 */
public final class DdcServiceKeyFactory {

    private final DdcProperties properties;

    public DdcServiceKeyFactory(DdcProperties properties) {
        this.properties = properties;
    }

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
     * @deprecated namespace is no longer part of the physical service key.
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
     * @deprecated namespace is no longer part of the physical service key.
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
