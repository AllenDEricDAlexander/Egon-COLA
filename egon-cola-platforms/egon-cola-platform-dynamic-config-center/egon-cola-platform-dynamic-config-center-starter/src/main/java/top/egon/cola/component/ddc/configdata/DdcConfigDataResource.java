package top.egon.cola.component.ddc.configdata;

import org.springframework.boot.context.config.ConfigDataResource;

import java.util.Objects;

/**
 * 描述由业务、环境、命名空间和应用共同定位的 DDC ConfigData 资源。 Describes a DDC ConfigData resource identified by business, environment, namespace, and application.
 */
public final class DdcConfigDataResource extends ConfigDataResource {

    /**
     * 资源不存在时是否允许继续启动。 Whether startup may continue when the resource is absent.
     */
    private final boolean optional;

    /**
     * 资源所属业务编码。 Business code owning the resource.
     */
    private final String bizCode;

    /**
     * 资源所属环境。 Environment owning the resource.
     */
    private final String env;

    /**
     * 资源所属命名空间。 Namespace owning the resource.
     */
    private final String namespace;

    /**
     * 资源所属应用编码。 Application code owning the resource.
     */
    private final String appCode;

    /**
     * 远程配置资源名。 Remote configuration resource name.
     */
    private final String resourceName;

    /**
     * 创建 DDC ConfigData 资源描述。 Creates a DDC ConfigData resource descriptor.
     *
     * @param optional     资源缺失时是否允许继续。 whether absence is allowed
     * @param bizCode      业务编码。 business code
     * @param env          环境编码。 environment code
     * @param namespace    命名空间编码。 namespace code
     * @param appCode      应用编码。 application code
     * @param resourceName 资源名。 resource name
     */
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

    /**
     * 返回资源是否允许缺失。 Returns whether the resource may be absent.
     *
     * @return 资源是否为可选。 whether the resource is optional
     */
    public boolean optional() {
        return optional;
    }

    /**
     * 返回资源所属业务编码。 Returns the business code owning the resource.
     *
     * @return 业务编码。 business code
     */
    public String bizCode() {
        return bizCode;
    }

    /**
     * 返回资源所属环境编码。 Returns the environment code owning the resource.
     *
     * @return 环境编码。 environment code
     */
    public String env() {
        return env;
    }

    /**
     * 返回资源所属命名空间编码。 Returns the namespace code owning the resource.
     *
     * @return 命名空间编码。 namespace code
     */
    public String namespace() {
        return namespace;
    }

    /**
     * 返回资源所属应用编码。 Returns the application code owning the resource.
     *
     * @return 应用编码。 application code
     */
    public String appCode() {
        return appCode;
    }

    /**
     * 返回远程配置资源名。 Returns the remote configuration resource name.
     *
     * @return 远程资源名。 remote resource name
     */
    public String resourceName() {
        return resourceName;
    }

    /**
     * 按可选标志和完整作用域比较资源。 Compares resources by the optional flag and full scope.
     *
     * @param object 待比较对象。 object to compare
     * @return 表示同一资源时为 {@code true}。 {@code true} when both describe the same resource
     */
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

    /**
     * 计算与完整资源作用域相等性一致的哈希值。 Computes a hash code consistent with full resource-scope equality.
     *
     * @return 与相等性字段一致的哈希值。 hash code consistent with the equality fields
     */
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

    /**
     * 返回便于诊断的 DDC 位置和作用域文本。 Returns diagnostic DDC location and scope text.
     *
     * @return DDC 位置和作用域文本。 DDC location and scope text
     */
    @Override
    public String toString() {
        return "ddc:" + resourceName
                + " [" + bizCode + '/' + env + '/' + namespace + '/' + appCode + ']';
    }
}
