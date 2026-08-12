package top.egon.cola.platform.idp.starter.admission;

import top.egon.cola.component.ddc.api.extension.DdcAdmissionTicketSupplier;
import top.egon.cola.component.ddc.model.admission.DdcAdmissionRequest;
import top.egon.cola.component.ddc.model.admission.DdcAdmissionTicket;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Function;

/**
 * 仅为配置的精确 Resource 实例缓存并提前续签 Admission Ticket。
 *
 * <p>Caches and renews Admission Tickets ahead of expiration only for the configured exact
 * Resource instance.</p>
 *
 * <p>IdP 暂时不可用时，已认证实例最多继续使用现有票据到其过期时间；新实例或已过期实例
 * 始终 Fail Closed。</p>
 *
 * <p>When IdP is temporarily unavailable, an authenticated instance may use its current ticket
 * only until expiration. New or expired instances always fail closed.</p>
 */
public final class CachingDdcAdmissionTicketSupplier
        implements DdcAdmissionTicketSupplier {

    /** 远程 Admission Client 调用；remote Admission Client call. */
    private final Function<DdcAdmissionRequest, DdcAdmissionTicket> client;

    /** 配置允许的唯一 Resource 实例身份；sole configured Resource instance identity. */
    private final DdcAdmissionRequest expectedRequest;

    /** 提前续签窗口；renewal-ahead window. */
    private final Duration renewalSkew;

    /** UTC 业务时钟；UTC business clock. */
    private final Clock clock;

    /** 当前缓存票据；currently cached ticket. */
    private volatile DdcAdmissionTicket cached;

    /**
     * 创建生产 Admission Ticket 缓存。
     *
     * <p>Creates the production Admission Ticket cache.</p>
     *
     * @param client IdP Admission RPC 客户端；IdP Admission RPC client
     * @param expectedRequest 配置的精确 Resource 实例；configured exact Resource instance
     * @param renewalSkew 提前续签窗口；renewal-ahead window
     * @param clock UTC 业务时钟；UTC business clock
     */
    public CachingDdcAdmissionTicketSupplier(
            RpcResourceServerAdmissionClient client,
            DdcAdmissionRequest expectedRequest,
            Duration renewalSkew,
            Clock clock
    ) {
        this(
                Objects.requireNonNull(client, "client")::request,
                expectedRequest,
                renewalSkew,
                clock
        );
    }

    /**
     * 使用可控远程调用创建缓存，供同包测试验证续签语义。
     *
     * <p>Creates the cache with a controllable remote call for package-level renewal tests.</p>
     *
     * @param client 远程调用；remote call
     * @param expectedRequest 配置的精确 Resource 实例；configured exact Resource instance
     * @param renewalSkew 提前续签窗口；renewal-ahead window
     * @param clock UTC 业务时钟；UTC business clock
     */
    CachingDdcAdmissionTicketSupplier(
            Function<DdcAdmissionRequest, DdcAdmissionTicket> client,
            DdcAdmissionRequest expectedRequest,
            Duration renewalSkew,
            Clock clock
    ) {
        this.client = Objects.requireNonNull(client, "client");
        this.expectedRequest = Objects.requireNonNull(
                expectedRequest,
                "expectedRequest"
        );
        this.renewalSkew = Objects.requireNonNull(
                renewalSkew,
                "renewalSkew"
        );
        if (renewalSkew.isNegative() || renewalSkew.isZero()) {
            throw new IllegalArgumentException(
                    "renewalSkew must be positive"
            );
        }
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * 返回未过期且精确绑定的票据，并在续签窗口内尝试刷新。
     *
     * <p>Returns an unexpired, exactly bound ticket and attempts renewal inside the renewal
     * window.</p>
     *
     * @param bizCode 生产者实际业务域编码；actual producer business-domain code
     * @param appCode 生产者实际应用编码；actual producer application code
     * @param environment 生产者实际环境编码；actual producer environment code
     * @param instanceId 生产者实际实例标识；actual producer instance identifier
     * @return 当前可用票据；currently usable ticket
     */
    @Override
    public synchronized DdcAdmissionTicket getTicket(
            String bizCode,
            String appCode,
            String environment,
            String instanceId
    ) {
        DdcAdmissionRequest request = new DdcAdmissionRequest(
                expectedRequest.resourceServerId(),
                expectedRequest.resourceUri(),
                bizCode,
                appCode,
                environment,
                instanceId
        );
        if (!expectedRequest.equals(request)) {
            throw new IllegalArgumentException(
                    "admission request does not match configuration"
            );
        }
        Instant now = clock.instant();
        DdcAdmissionTicket current = cached;
        if (current != null
                && current.expiresAt().isAfter(now.plus(renewalSkew))) {
            return current;
        }
        try {
            DdcAdmissionTicket renewed = Objects.requireNonNull(
                    client.apply(request),
                    "admission ticket"
            );
            if (!renewed.matches(request)
                    || !renewed.expiresAt().isAfter(
                            now.plus(renewalSkew))) {
                throw new IllegalStateException(
                        "IdP admission ticket is invalid or too short-lived"
                );
            }
            cached = renewed;
            return renewed;
        } catch (RuntimeException exception) {
            Instant fallbackNow = clock.instant();
            if (current != null
                    && current.expiresAt().isAfter(fallbackNow)) {
                return current;
            }
            throw exception;
        }
    }
}
