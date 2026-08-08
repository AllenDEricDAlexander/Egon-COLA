package top.egon.cola.component.ddc.management;

import top.egon.cola.component.ddc.management.model.DdcManagementConfig;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigClientInstance;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigDeleteRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementConfigUpsertRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementInstanceQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishRequest;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishResult;
import top.egon.cola.component.ddc.management.model.DdcManagementPublishTask;
import top.egon.cola.component.ddc.management.model.DdcManagementScopeBinding;
import top.egon.cola.component.ddc.management.model.DdcManagementScopeQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceCatalog;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceQuery;
import top.egon.cola.component.ddc.management.model.DdcManagementServiceSnapshot;

import java.util.List;
import java.util.Optional;

/**
 * DDC 管理开放接口的类型化客户端契约。 / Typed client contract for the DDC management OpenAPI.
 */
public interface DdcManagementClient {

    /**
     * 按完整配置作用域查找配置；不支持该能力的实现默认抛出异常。 /
     * Finds a configuration by its complete scope; implementations without this capability throw by default.
     *
     * @param query 配置作用域查询条件 / configuration-scope query
     * @return 找到的配置，未找到时为空 / matching configuration, or empty when none exists
     * @throws UnsupportedOperationException 当实现不支持精确配置查询时 / when exact configuration lookup is unsupported
     */
    default Optional<DdcManagementConfig> findConfig(DdcManagementConfigQuery query) {
        throw new UnsupportedOperationException("Exact config lookup is not supported");
    }

    /**
     * 新增或更新指定作用域的配置。 / Creates or updates the configuration in the requested scope.
     *
     * @param request 配置写入请求 / configuration upsert request
     * @return 写入后的配置 / configuration after the upsert
     */
    DdcManagementConfig upsert(DdcManagementConfigUpsertRequest request);

    /**
     * 删除指定作用域的配置。 / Deletes the configuration in the requested scope.
     *
     * @param request 配置删除请求 / configuration deletion request
     */
    void delete(DdcManagementConfigDeleteRequest request);

    /**
     * 发布指定作用域的配置内容。 / Publishes configuration content to the requested scope.
     *
     * @param request 配置发布请求 / configuration publication request
     * @return 发布受理或执行结果 / publication acceptance or execution result
     */
    DdcManagementPublishResult publish(DdcManagementPublishRequest request);

    /**
     * 查询指定变更标识对应的发布任务。 / Retrieves the publication task for a change identifier.
     *
     * @param changeId 发布变更标识 / publication change identifier
     * @return 发布任务详情 / publication task details
     */
    DdcManagementPublishTask getPublishTask(String changeId);

    /**
     * 重试指定变更标识对应的发布任务。 / Retries the publication task for a change identifier.
     *
     * @param changeId 发布变更标识 / publication change identifier
     * @return 重试后的发布结果 / publication result after retrying
     */
    DdcManagementPublishResult retry(String changeId);

    /**
     * 查询配置客户端租约实例。 / Lists configuration-client lease instances.
     *
     * @param query 配置客户端筛选条件 / configuration-client filters
     * @return 匹配的配置客户端实例 / matching configuration-client instances
     */
    List<DdcManagementConfigClientInstance> getConfigClients(
            DdcManagementInstanceQuery query);

    /**
     * 查询作用域绑定；不支持该能力的实现默认抛出异常。 /
     * Lists scope bindings; implementations without this capability throw by default.
     *
     * @param query 作用域绑定筛选条件 / scope-binding filters
     * @return 匹配的作用域绑定 / matching scope bindings
     * @throws UnsupportedOperationException 当实现不支持作用域绑定查询时 / when scope-binding lookup is unsupported
     */
    default List<DdcManagementScopeBinding> getScopeBindings(
            DdcManagementScopeQuery query) {
        throw new UnsupportedOperationException(
                "Scope binding lookup is not supported");
    }

    /**
     * 查询匹配的服务键目录。 / Retrieves the catalog of matching service keys.
     *
     * @param query 服务筛选条件 / service filters
     * @return 服务键目录及其观测代次 / service-key catalog and its observed generation
     */
    DdcManagementServiceCatalog getServiceKeys(DdcManagementServiceQuery query);

    /**
     * 查询指定服务的实例快照。 / Retrieves the instance snapshot for a service.
     *
     * @param query 服务筛选与定位条件 / service filters and identity criteria
     * @return 服务实例快照 / service-instance snapshot
     */
    DdcManagementServiceSnapshot getInstances(DdcManagementServiceQuery query);
}
