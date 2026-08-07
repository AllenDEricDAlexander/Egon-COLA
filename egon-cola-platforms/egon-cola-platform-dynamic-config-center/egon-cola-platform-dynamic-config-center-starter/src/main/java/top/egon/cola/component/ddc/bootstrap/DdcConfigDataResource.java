package top.egon.cola.component.ddc.bootstrap;

import org.springframework.boot.context.config.ConfigDataResource;

import java.util.Objects;

public final class DdcConfigDataResource extends ConfigDataResource {

    private final boolean optional;

    private final String bizCode;

    private final String env;

    private final String namespace;

    private final String appCode;

    private final String resourceName;

    public DdcConfigDataResource(boolean optional,
                                 String bizCode,
                                 String env,
                                 String namespace,
                                 String appCode,
                                 String resourceName) {
        super(optional);
        this.optional = optional;
        this.bizCode = bizCode;
        this.env = env;
        this.namespace = namespace;
        this.appCode = appCode;
        this.resourceName = resourceName;
    }

    public boolean optional() {
        return optional;
    }

    public String bizCode() {
        return bizCode;
    }

    public String env() {
        return env;
    }

    public String namespace() {
        return namespace;
    }

    public String appCode() {
        return appCode;
    }

    public String resourceName() {
        return resourceName;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof DdcConfigDataResource that)) {
            return false;
        }
        return optional == that.optional
                && Objects.equals(bizCode, that.bizCode)
                && Objects.equals(env, that.env)
                && Objects.equals(namespace, that.namespace)
                && Objects.equals(appCode, that.appCode)
                && Objects.equals(resourceName, that.resourceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                optional,
                bizCode,
                env,
                namespace,
                appCode,
                resourceName
        );
    }

    @Override
    public String toString() {
        return "ddc:" + resourceName
                + " [" + bizCode + '/' + env + '/' + namespace + '/' + appCode + ']';
    }
}
