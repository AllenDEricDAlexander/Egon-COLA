package top.egon.cola.platform.idp.core.port;

import top.egon.cola.platform.idp.core.resource.ResourceServer;

/**
 * Resource Server 管理状态投影到运行时安全存储的端口。
 *
 * <p>Port projecting Resource Server administrative state into runtime security storage.</p>
 */
public interface ResourceServerRuntimePort {

    /**
     * 写入或更新 Resource 运行态投影。
     *
     * <p>Creates or updates a Resource runtime projection.</p>
     *
     * @param resource Resource Server；Resource Server
     */
    void project(ResourceServer resource);

    /**
     * 移除 Resource 运行态投影。
     *
     * <p>Removes a Resource runtime projection.</p>
     *
     * @param resource Resource Server；Resource Server
     */
    void remove(ResourceServer resource);
}
