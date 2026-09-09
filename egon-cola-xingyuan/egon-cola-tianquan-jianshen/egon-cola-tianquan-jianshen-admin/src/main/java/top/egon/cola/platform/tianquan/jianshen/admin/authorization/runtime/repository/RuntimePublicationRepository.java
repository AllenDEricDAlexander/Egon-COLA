package top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.repository;

import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.activation.domain.vo.RuntimePublicationVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.domain.dto.PublishCommandDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.domain.vo.PublishResultVO;

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

    /**
     * 失效不可用的用户发布，同时保留单调递增的授权及策略版本水位。
     * Invalidates an unusable user publication while retaining monotonic version watermarks.
     *
     * @param tenantId 租户标识；tenant identifier
     * @param identitySub IdP 身份标识；IdP subject identifier
     * @param userId RBAC 用户标识；RBAC user identifier
     * @param authVersion 当前授权版本；current authorization version
     * @param policyVersion 当前策略版本；current policy version
     */
    void invalidate(String tenantId, String identitySub, String userId,
                    long authVersion, long policyVersion);
}
