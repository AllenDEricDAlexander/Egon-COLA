package top.egon.cola.component.ddc.client;

import top.egon.cola.component.ddc.model.dto.DdcAckRequest;
import top.egon.cola.component.ddc.model.dto.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.dto.DdcInstanceRegisterRequest;
import top.egon.cola.component.ddc.model.vo.DdcConfigValue;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;

import java.util.List;

/**
 * 定义 Starter 与 DDC 管理端之间的实例租约及配置交互。 Defines instance-lease and configuration interactions between the starter and DDC management service.
 */
public interface DdcAdminClient {

    /**
     * 注册配置实例并取得租约会话。 Registers a configuration instance and obtains its lease session.
     *
     * @param request 实例注册请求。 instance registration request
     * @return 新建或续接的租约会话。 newly created or resumed lease session
     */
    DdcLeaseSession register(DdcInstanceRegisterRequest request);

    /**
     * 为现有实例租约发送心跳。 Sends a heartbeat for an existing instance lease.
     *
     * @param request 携带实例与租约标识的心跳请求。 heartbeat request containing instance and lease identifiers
     * @return 租约操作结果。 lease operation result
     */
    DdcLeaseOperationResult heartbeat(DdcHeartbeatRequest request);

    /**
     * 将现有实例租约标记为下线。 Marks an existing instance lease offline.
     *
     * @param request 携带实例与租约标识的下线请求。 offline request containing instance and lease identifiers
     * @return 租约操作结果。 lease operation result
     */
    DdcLeaseOperationResult offline(DdcHeartbeatRequest request);

    /**
     * 拉取当前客户端作用域内的远程配置。 Pulls remote configurations in the current client scope.
     *
     * @return 配置值列表，不存在配置时为空列表。 configuration values, or an empty list when none exist
     */
    List<DdcConfigValue> pull();

    /**
     * 向管理端确认一次配置发布结果。 Acknowledges a configuration publication result to the management service.
     *
     * @param request 发布确认请求。 publication acknowledgement request
     */
    void ack(DdcAckRequest request);
}
