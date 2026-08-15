package top.egon.cola.platform.rbac3.admin.runtime.repository;

import top.egon.cola.platform.rbac3.admin.iam.role.activation.domain.vo.RuntimePublicationVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.dto.PublishCommandDTO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.PublishResultVO;

/**
 * 用户授权运行时发布端口。 User authorization runtime publication port.
 */
public interface RuntimePublicationRepository {

    /**
     * 发布登录或最小运行时快照。
     * Publishes a login or minimum runtime snapshot.
     *
     * @param command 发布命令；publication command
     * @return 发布结果；publication result
     */
    PublishResultVO publish(PublishCommandDTO command);

    /**
     * 发布角色激活运行时快照。
     * Publishes a role-activation runtime snapshot.
     *
     * @param publication 发布数据；publication data
     */
    void publish(RuntimePublicationVO publication);
}
