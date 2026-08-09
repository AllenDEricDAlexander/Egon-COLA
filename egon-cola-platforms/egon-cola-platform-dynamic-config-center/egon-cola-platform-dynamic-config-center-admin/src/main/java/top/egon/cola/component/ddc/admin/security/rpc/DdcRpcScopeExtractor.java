package top.egon.cola.component.ddc.admin.security.rpc;

import com.google.protobuf.Message;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.AcknowledgePublishRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DeleteConfigRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DeregisterServiceRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.FindConfigRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetConfigClientsRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetInstancesRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetScopeBindingsRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetServiceInstancesRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetServiceKeysRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetServicesRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.HeartbeatConfigClientRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.HeartbeatServiceRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.OfflineConfigClientRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.PublishConfigRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.PullConfigRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.RegisterConfigClientRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.RegisterServiceRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.UpsertConfigRequest;

/**
 * 从已发布的 DDC unary protobuf 请求中提取鉴权作用域。
 * / Extracts authorization scope from published DDC unary protobuf requests.
 */
public final class DdcRpcScopeExtractor {

    private static final String CONFIG =
            "egon.ddc.v1.DdcConfigRuntimeService/";
    private static final String REGISTRY =
            "egon.ddc.v1.DdcServiceRegistryService/";
    private static final String MANAGEMENT =
            "egon.ddc.v1.DdcManagementService/";

    /** 严格提取方法对应的客户端类型和作用域。 / Strictly extracts client type and scope. */
    public Scope extract(String fullMethodName, Message request) {
        if (fullMethodName == null || request == null) {
            throw new IllegalArgumentException(
                    "fullMethodName and request are required");
        }
        return switch (fullMethodName) {
            case CONFIG + "RegisterConfigClient" -> configScope(
                    type(request, RegisterConfigClientRequest.class).getScope());
            case CONFIG + "HeartbeatConfigClient" -> configScope(
                    type(request, HeartbeatConfigClientRequest.class).getScope());
            case CONFIG + "OfflineConfigClient" -> configScope(
                    type(request, OfflineConfigClientRequest.class).getScope());
            case CONFIG + "PullConfig" -> configScope(
                    type(request, PullConfigRequest.class).getScope());
            case CONFIG + "AcknowledgePublish" -> configScope(
                    type(request, AcknowledgePublishRequest.class).getScope());
            case REGISTRY + "RegisterService" -> registryScope(
                    type(request, RegisterServiceRequest.class)
                            .getServiceKey().getScope());
            case REGISTRY + "HeartbeatService" -> registryScope(
                    type(request, HeartbeatServiceRequest.class)
                            .getServiceKey().getScope());
            case REGISTRY + "DeregisterService" -> registryScope(
                    type(request, DeregisterServiceRequest.class)
                            .getServiceKey().getScope());
            case REGISTRY + "GetServiceInstances" -> registryScope(
                    type(request, GetServiceInstancesRequest.class)
                            .getServiceKey().getScope());
            case REGISTRY + "GetServices" -> registryQuery(
                    type(request, GetServicesRequest.class));
            case MANAGEMENT + "FindConfig" -> managementScope(
                    type(request, FindConfigRequest.class).getScope());
            case MANAGEMENT + "UpsertConfig" -> managementScope(
                    type(request, UpsertConfigRequest.class).getScope());
            case MANAGEMENT + "DeleteConfig" -> managementScope(
                    type(request, DeleteConfigRequest.class).getScope());
            case MANAGEMENT + "PublishConfig" -> managementScope(
                    type(request, PublishConfigRequest.class).getScope());
            case MANAGEMENT + "GetPublishTask",
                 MANAGEMENT + "RetryPublishTask" -> Scope.unscoped(
                    "MANAGEMENT");
            case MANAGEMENT + "GetConfigClients" -> managementScope(
                    type(request, GetConfigClientsRequest.class).getScope());
            case MANAGEMENT + "GetScopeBindings" -> scopeBindingQuery(
                    type(request, GetScopeBindingsRequest.class));
            case MANAGEMENT + "GetServiceKeys" -> managementQuery(
                    type(request, GetServiceKeysRequest.class));
            case MANAGEMENT + "GetInstances" -> managementQuery(
                    type(request, GetInstancesRequest.class));
            default -> throw new IllegalArgumentException(
                    "Unknown DDC RPC method");
        };
    }

    private Scope configScope(
            top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcScope scope) {
        return required("SDK", scope);
    }

    private Scope registryScope(
            top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcScope scope) {
        return required("REGISTRY", scope);
    }

    private Scope managementScope(
            top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcScope scope) {
        return required("MANAGEMENT", scope);
    }

    private Scope required(
            String clientType,
            top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcScope scope) {
        if (scope == null
                || !text(scope.getBizCode())
                || !text(scope.getEnv())
                || !text(scope.getAppCode())) {
            throw new IllegalArgumentException(
                    "bizCode, env and appCode scope are required");
        }
        return new Scope(
                clientType,
                scope.getAppCode(),
                scope.getEnv(),
                scope.getBizCode()
        );
    }

    private Scope registryQuery(GetServicesRequest request) {
        if (!request.hasQuery()) {
            throw new IllegalArgumentException("service query is required");
        }
        var query = request.getQuery();
        return new Scope(
                "REGISTRY",
                query.hasAppCode() ? query.getAppCode() : null,
                query.hasEnv() ? query.getEnv() : null,
                query.hasBizCode() ? query.getBizCode() : null
        );
    }

    private Scope scopeBindingQuery(GetScopeBindingsRequest request) {
        if (!request.hasQuery()) {
            throw new IllegalArgumentException("scope query is required");
        }
        var query = request.getQuery();
        return new Scope(
                "MANAGEMENT",
                query.hasAppCode() ? query.getAppCode() : null,
                query.hasEnv() ? query.getEnv() : null,
                query.hasBizCode() ? query.getBizCode() : null
        );
    }

    private Scope managementQuery(GetServiceKeysRequest request) {
        if (!request.hasQuery()) {
            throw new IllegalArgumentException("service query is required");
        }
        var query = request.getQuery();
        return new Scope(
                "MANAGEMENT",
                query.hasAppCode() ? query.getAppCode() : null,
                query.hasEnv() ? query.getEnv() : null,
                query.hasBizCode() ? query.getBizCode() : null
        );
    }

    private Scope managementQuery(GetInstancesRequest request) {
        if (!request.hasQuery()) {
            throw new IllegalArgumentException("service query is required");
        }
        var query = request.getQuery();
        return new Scope(
                "MANAGEMENT",
                query.hasAppCode() ? query.getAppCode() : null,
                query.hasEnv() ? query.getEnv() : null,
                query.hasBizCode() ? query.getBizCode() : null
        );
    }

    private <T extends Message> T type(Message value, Class<T> expected) {
        if (!expected.isInstance(value)) {
            throw new IllegalArgumentException("Unexpected DDC RPC request type");
        }
        return expected.cast(value);
    }

    private boolean text(String value) {
        return value != null && !value.isBlank();
    }

    /** 鉴权使用的传输无关作用域。 / Transport-neutral scope used for authorization. */
    public record Scope(
            String clientType,
            String appCode,
            String env,
            String bizCode) {

        private static Scope unscoped(String clientType) {
            return new Scope(clientType, null, null, null);
        }
    }
}
