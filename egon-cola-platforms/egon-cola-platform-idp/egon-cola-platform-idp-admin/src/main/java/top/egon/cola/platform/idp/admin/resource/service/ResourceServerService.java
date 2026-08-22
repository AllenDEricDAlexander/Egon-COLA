package top.egon.cola.platform.idp.admin.resource.service;

import top.egon.cola.platform.idp.admin.resource.domain.dto.BatchClientResourceGrantDTO;
import top.egon.cola.platform.idp.admin.resource.domain.dto.BatchResourceServerActionDTO;
import top.egon.cola.platform.idp.admin.resource.domain.dto.CreateResourceServerDTO;
import top.egon.cola.platform.idp.admin.resource.domain.dto.DeleteClientResourceGrantDTO;
import top.egon.cola.platform.idp.admin.resource.domain.dto.ResourceVersionDTO;
import top.egon.cola.platform.idp.admin.resource.domain.dto.UpsertClientResourceGrantDTO;
import top.egon.cola.platform.idp.admin.resource.domain.vo.ClientResourceGrantVO;
import top.egon.cola.platform.idp.admin.resource.domain.vo.ResourceServerVO;

import java.util.List;

/**
 * Resource Server 和 Client Grant 的管理用例入口。
 *
 * <p>Management use-case entry point for Resource Servers and Client Grants.</p>
 */
public interface ResourceServerService {

    /** @return 全部 Resource Server；all Resource Servers */
    List<ResourceServerVO> list();

    /**
     * 查询 Resource Server 详情。
     *
     * <p>Gets Resource Server details.</p>
     *
     * @param resourceServerId Resource Server 标识；Resource Server identifier
     * @return Resource Server 详情；Resource Server details
     */
    ResourceServerVO detail(String resourceServerId);

    /**
     * 创建 Resource Server。
     *
     * <p>Creates a Resource Server.</p>
     *
     * @param command 创建输入；create input
     * @return 新 Resource Server；new Resource Server
     */
    ResourceServerVO create(CreateResourceServerDTO command);

    /**
     * 启用 Resource Server。
     *
     * <p>Enables a Resource Server.</p>
     *
     * @param resourceServerId Resource Server 标识；Resource Server identifier
     * @param command 版本输入；version input
     * @return 更新结果；updated view
     */
    ResourceServerVO enable(
            String resourceServerId,
            ResourceVersionDTO command
    );

    /**
     * 禁用 Resource Server。
     *
     * <p>Disables a Resource Server.</p>
     *
     * @param resourceServerId Resource Server 标识；Resource Server identifier
     * @param command 版本输入；version input
     * @return 更新结果；updated view
     */
    ResourceServerVO disable(
            String resourceServerId,
            ResourceVersionDTO command
    );

    /**
     * 新建或更新应用级 Client Grant。
     *
     * <p>Creates or updates an application-level Client Grant.</p>
     *
     * @param clientId Client 标识；Client identifier
     * @param resourceServerId Resource Server 标识；Resource Server identifier
     * @param command Grant 输入；Grant input
     * @return Grant 视图；Grant view
     */
    ClientResourceGrantVO putGrant(
            String clientId,
            String resourceServerId,
            UpsertClientResourceGrantDTO command
    );

    /**
     * 删除应用级 Client Grant。
     *
     * <p>Deletes an application-level Client Grant.</p>
     *
     * @param clientId Client 标识；Client identifier
     * @param resourceServerId Resource Server 标识；Resource Server identifier
     * @param command 删除输入；delete input
     */
    void deleteGrant(
            String clientId,
            String resourceServerId,
            DeleteClientResourceGrantDTO command
    );

    /**
     * 批量修改明确应用的 Resource 状态。
     *
     * <p>Batch-changes Resource status for explicit applications.</p>
     *
     * @param command 批量输入；batch input
     * @return 逐应用结果；per-application results
     */
    List<ResourceServerVO> batch(BatchResourceServerActionDTO command);

    /**
     * 批量增删明确应用的 Client Grant。
     *
     * <p>Batch-adds or deletes Client Grants for explicit applications.</p>
     *
     * @param clientId Client 标识；Client identifier
     * @param command 批量输入；batch input
     * @return 逐应用 Grant 结果；per-application Grant results
     */
    List<ClientResourceGrantVO> batchGrants(
            String clientId,
            BatchClientResourceGrantDTO command
    );
}
