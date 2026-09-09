package top.egon.cola.component.tianshu.api.client;

import top.egon.cola.component.tianshu.model.config.DdcAckRequest;
import top.egon.cola.component.tianshu.model.config.DdcHeartbeatRequest;
import top.egon.cola.component.tianshu.model.config.DdcInstanceRegisterRequest;
import top.egon.cola.component.tianshu.model.config.DdcConfigValue;
import top.egon.cola.component.tianshu.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.tianshu.model.lease.DdcLeaseSession;

import java.util.List;

/**
 * 定义 Starter 与 Tianshu 管理端之间的实例租约及配置交互。 Defines instance-lease and configuration interactions between the starter and Tianshu management service.
 */
public interface DdcConfigClient {

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
     * 拉取指定资源版本，用于通知省略内容时回源读取准备中的精确快照。
     * Pulls an exact resource version when a notification defers its content.
     *
     * @param resourceName 资源名 / resource name
     * @param targetVersion 目标版本 / target version
     * @return 匹配的配置值，不存在时为空列表 / matching value, or an empty list when unavailable
     */
    default List<DdcConfigValue> pull(String resourceName, long targetVersion) {
        if (resourceName == null || resourceName.isBlank() || targetVersion <= 0) {
            return List.of();
        }
        return pull().stream()
                .filter(value -> value != null
                        && resourceName.equals(value.getResourceName())
                        && Long.valueOf(targetVersion).equals(value.getVersion()))
                .toList();
    }

    /**
     * 向管理端确认一次配置发布结果。 Acknowledges a configuration publication result to the management service.
     *
     * @param request 发布确认请求。 publication acknowledgement request
     */
    void ack(DdcAckRequest request);
}
