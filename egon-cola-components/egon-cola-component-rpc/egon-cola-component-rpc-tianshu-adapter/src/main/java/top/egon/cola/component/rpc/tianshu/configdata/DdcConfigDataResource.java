package top.egon.cola.component.rpc.tianshu.configdata;

import org.springframework.boot.context.config.ConfigDataResource;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * 描述由业务、环境、命名空间和应用共同定位的 Tianshu ConfigData 资源。 Describes a Tianshu ConfigData resource identified by business, environment, namespace, and application.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public final class DdcConfigDataResource extends ConfigDataResource {

    /**
     * 资源不存在时是否允许继续启动。 Whether startup may continue when the resource is absent.
     */
    private final boolean optional;

    /** Registered storage tenant fixed during ConfigData bootstrap. */
    private final long tenantId;

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

    /** Explicit optional bootstrap mode; the initial dynamic source has version zero. */
    private final boolean deferInitialLoad;

    /**
     * 创建 Tianshu ConfigData 资源描述。 Creates a Tianshu ConfigData resource descriptor.
     *
     * @param optional     资源缺失时是否允许继续。 whether absence is allowed
     * @param tenantId     已登记存储租户。 registered storage tenant
     * @param bizCode      业务编码。 business code
     * @param env          环境编码。 environment code
     * @param namespace    命名空间编码。 namespace code
     * @param appCode      应用编码。 application code
     * @param resourceName 资源名。 resource name
     */
    public DdcConfigDataResource(boolean optional,
                                 long tenantId,
                                 String bizCode,
                                 String env,
                                 String namespace,
                                 String appCode,
                                 String resourceName) {
        this(optional, tenantId, bizCode, env, namespace, appCode, resourceName, false);
    }

    public DdcConfigDataResource(boolean optional,
                                 long tenantId,
                                 String bizCode,
                                 String env,
                                 String namespace,
                                 String appCode,
                                 String resourceName,
                                 boolean deferInitialLoad) {
        super(optional);
        if (tenantId <= 0) {
            throw new IllegalArgumentException("tenantId must be positive");
        }
        if (deferInitialLoad && !optional) {
            throw new IllegalArgumentException("defer-initial-load requires optional:tianshu config");
        }
        this.optional = optional;
        this.tenantId = tenantId;
        this.bizCode = bizCode;
        this.env = env;
        this.namespace = namespace;
        this.appCode = appCode;
        this.resourceName = resourceName;
        this.deferInitialLoad = deferInitialLoad;
    }

    public boolean deferInitialLoad() {
        return deferInitialLoad;
    }

    /**
     * 返回资源是否允许缺失。 Returns whether the resource may be absent.
     *
     * @return 资源是否为可选。 whether the resource is optional
     */
    public boolean optional() {
        return optional;
    }

    public long tenantId() {
        return tenantId;
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

}
