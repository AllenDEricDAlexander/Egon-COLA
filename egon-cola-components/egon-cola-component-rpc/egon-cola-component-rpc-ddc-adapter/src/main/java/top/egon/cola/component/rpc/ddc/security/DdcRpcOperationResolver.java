package top.egon.cola.component.rpc.ddc.security;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 将已发布的 DDC RPC 方法严格映射为鉴权操作。
 * / Strictly maps published DDC RPC methods to authorization operations.
 */
public final class DdcRpcOperationResolver {

    private static final String CONFIG =
            "egon.ddc.v1.DdcConfigRuntimeService/";
    private static final String REGISTRY =
            "egon.ddc.v1.DdcServiceRegistryService/";
    private static final String MANAGEMENT =
            "egon.ddc.v1.DdcManagementService/";

    private final Map<String, DdcRpcOperation> operations = operations();

    /**
     * 解析完整 gRPC 方法名；未知方法拒绝继续处理。
     * / Resolves a full gRPC method name and fails closed for unknown methods.
     */
    public DdcRpcOperation resolve(String fullMethodName) {
        DdcRpcOperation operation = operations.get(fullMethodName);
        if (operation == null) {
            throw new IllegalArgumentException(
                    "Unknown DDC RPC method: " + fullMethodName);
        }
        return operation;
    }

    /** 返回用于审计和测试的裸方法映射。 / Returns the bare-method map for audit and tests. */
    public Map<String, DdcRpcOperation> operationsByBareMethod() {
        Map<String, DdcRpcOperation> result = new LinkedHashMap<>();
        operations.forEach((method, operation) -> result.put(
                method.substring(method.lastIndexOf('/') + 1),
                operation
        ));
        return Map.copyOf(result);
    }

    private Map<String, DdcRpcOperation> operations() {
        Map<String, DdcRpcOperation> values = new LinkedHashMap<>();
        values.put(CONFIG + "RegisterConfigClient", DdcRpcOperation.SDK_REGISTER);
        values.put(CONFIG + "HeartbeatConfigClient", DdcRpcOperation.SDK_HEARTBEAT);
        values.put(CONFIG + "OfflineConfigClient", DdcRpcOperation.SDK_OFFLINE);
        values.put(CONFIG + "PullConfig", DdcRpcOperation.CONFIG_PULL);
        values.put(CONFIG + "AcknowledgePublish", DdcRpcOperation.PUBLISH_ACK);
        values.put(REGISTRY + "RegisterService", DdcRpcOperation.REGISTRY_REGISTER);
        values.put(REGISTRY + "HeartbeatService", DdcRpcOperation.REGISTRY_HEARTBEAT);
        values.put(REGISTRY + "DeregisterService", DdcRpcOperation.REGISTRY_DEREGISTER);
        values.put(REGISTRY + "GetServiceInstances", DdcRpcOperation.REGISTRY_READ);
        values.put(REGISTRY + "GetServices", DdcRpcOperation.REGISTRY_READ);
        values.put(MANAGEMENT + "FindConfig", DdcRpcOperation.MANAGEMENT_CONFIG_READ);
        values.put(MANAGEMENT + "UpsertConfig", DdcRpcOperation.MANAGEMENT_CONFIG_WRITE);
        values.put(MANAGEMENT + "DeleteConfig", DdcRpcOperation.MANAGEMENT_CONFIG_WRITE);
        values.put(MANAGEMENT + "PublishConfig", DdcRpcOperation.MANAGEMENT_PUBLISH);
        values.put(MANAGEMENT + "GetPublishTask", DdcRpcOperation.MANAGEMENT_TASK_READ);
        values.put(MANAGEMENT + "RetryPublishTask", DdcRpcOperation.MANAGEMENT_TASK_RETRY);
        values.put(MANAGEMENT + "GetConfigClients", DdcRpcOperation.MANAGEMENT_INSTANCE_READ);
        values.put(MANAGEMENT + "RevokeResourceAdmission",
                DdcRpcOperation.MANAGEMENT_ADMISSION_REVOKE);
        values.put(MANAGEMENT + "GetScopeBindings", DdcRpcOperation.MANAGEMENT_SCOPE_READ);
        values.put(MANAGEMENT + "GetServiceKeys", DdcRpcOperation.MANAGEMENT_REGISTRY_READ);
        values.put(MANAGEMENT + "GetInstances", DdcRpcOperation.MANAGEMENT_REGISTRY_READ);
        return Map.copyOf(values);
    }
}
