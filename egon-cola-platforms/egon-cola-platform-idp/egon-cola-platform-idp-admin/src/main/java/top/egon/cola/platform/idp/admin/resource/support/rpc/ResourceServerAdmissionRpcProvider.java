package top.egon.cola.platform.idp.admin.resource.support.rpc;

import com.google.protobuf.Timestamp;
import top.egon.cola.component.rpc.annotation.EgonRpcProvider;
import top.egon.cola.platform.idp.admin.resource.service.impl.ResourceServerAdmissionServiceImpl;
import top.egon.cola.platform.idp.core.resource.AdmissionRequest;
import top.egon.cola.platform.idp.rpc.contract.ResourceServerAdmissionRpc;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.IssueResourceServerAdmissionRequest;
import top.egon.cola.platform.idp.rpc.contract.proto.v1.IssueResourceServerAdmissionResponse;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

/**
 * 将 IdP Resource Server 准入 Egon-RPC 契约适配到本地准入签发服务。
 * Provider 只负责协议映射，管理客户端认证、三元组许可和票据签名仍由应用服务及领域策略完成。
 *
 * <p>Adapts the IdP Resource Server admission Egon-RPC contract to the local admission issuance
 * service. The provider performs protocol mapping only; management-client authentication,
 * business/app/environment authorization, and ticket signing remain in the application service
 * and domain policy.</p>
 */
@EgonRpcProvider
public final class ResourceServerAdmissionRpcProvider
        implements ResourceServerAdmissionRpc {

    /** Resource Server 准入签发服务；Resource Server admission issuance service. */
    private final ResourceServerAdmissionServiceImpl admissions;

    /**
     * 创建 Resource Server 准入 RPC Provider。
     *
     * <p>Creates the Resource Server admission RPC provider.</p>
     *
     * @param admissions 准入签发服务；admission issuance service
     */
    public ResourceServerAdmissionRpcProvider(
            ResourceServerAdmissionServiceImpl admissions
    ) {
        this.admissions = Objects.requireNonNull(admissions, "admissions");
    }

    /**
     * 映射并签发精确 Resource Server 实例的短期 Admission Ticket。
     *
     * <p>Maps the request and issues a short-lived Admission Ticket for the exact Resource Server
     * instance.</p>
     *
     * @param request 完整 RPC 准入请求；complete RPC admission request
     * @return 票据和过期时间；ticket and expiration time
     */
    @Override
    public IssueResourceServerAdmissionResponse issueAdmission(
            IssueResourceServerAdmissionRequest request
    ) {
        Objects.requireNonNull(request, "request");
        ResourceServerAdmissionServiceImpl.IssuedAdmissionTicket issued =
                admissions.issue(
                        request.getClientAssertionType(),
                        request.getClientId(),
                        request.getClientAssertion(),
                        new AdmissionRequest(
                                request.getResourceServerId(),
                                URI.create(request.getResource()),
                                request.getBiz(),
                                request.getApp(),
                                request.getEnv(),
                                request.getInstanceId()
                        )
                );
        return IssueResourceServerAdmissionResponse.newBuilder()
                .setTicket(issued.ticket())
                .setExpiresAt(timestamp(issued.expiresAt()))
                .build();
    }

    /**
     * 将领域时间转换为 Protobuf Timestamp。
     *
     * <p>Converts a domain instant to a Protobuf Timestamp.</p>
     *
     * @param value 票据过期时间；ticket expiration time
     * @return Protobuf 时间戳；Protobuf timestamp
     */
    private static Timestamp timestamp(Instant value) {
        Objects.requireNonNull(value, "value");
        return Timestamp.newBuilder()
                .setSeconds(value.getEpochSecond())
                .setNanos(value.getNano())
                .build();
    }
}
