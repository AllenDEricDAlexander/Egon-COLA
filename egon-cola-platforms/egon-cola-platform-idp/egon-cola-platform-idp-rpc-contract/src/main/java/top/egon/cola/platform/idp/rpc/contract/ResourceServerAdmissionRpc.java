package top.egon.cola.platform.idp.rpc.contract;

import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.IssueResourceServerAdmissionRequest;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.IssueResourceServerAdmissionResponse;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.ResourceServerAdmissionServiceGrpc;

import java.net.URI;

/**
 * IdP 内部 Resource Server 启动准入 RPC 契约。
 * 客户端使用静态目标直连 IdP，避免在取得 DDC Admission Ticket 前依赖 DDC 服务发现。
 *
 * <p>Internal IdP RPC contract for Resource Server startup admission. Clients connect directly
 * to a statically configured IdP target so admission does not depend on DDC discovery before a
 * DDC Admission Ticket has been obtained.</p>
 */
@EgonRpcService(
        grpcClass = ResourceServerAdmissionServiceGrpc.class,
        group = ResourceServerAdmissionRpc.GROUP,
        version = ResourceServerAdmissionRpc.VERSION
)
public interface ResourceServerAdmissionRpc {

    /** IdP RPC 服务分组；IdP RPC service group. */
    String GROUP = "idp";

    /** RPC 契约版本；RPC contract version. */
    String VERSION = "1.0.0";

    /**
     * {@code private_key_jwt} 绑定的稳定 RPC Audience。
     *
     * <p>Stable RPC audience to which {@code private_key_jwt} assertions are bound.</p>
     */
    URI AUDIENCE = URI.create(
            "urn:egon:rpc:idp:resource-server-admission:v1"
    );

    /**
     * 验证管理客户端和精确资源实例，并签发短期准入票据。
     *
     * <p>Authenticates the management client and exact resource instance, then issues a
     * short-lived admission ticket.</p>
     *
     * @param request 完整准入请求；complete admission request
     * @return 签发的票据和过期时间；issued ticket and expiration time
     */
    @EgonRpcMethod(name = "IssueAdmission")
    IssueResourceServerAdmissionResponse issueAdmission(
            IssueResourceServerAdmissionRequest request
    );
}
