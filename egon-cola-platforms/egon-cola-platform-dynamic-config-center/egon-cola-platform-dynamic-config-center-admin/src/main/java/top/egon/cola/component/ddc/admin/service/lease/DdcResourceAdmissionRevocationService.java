package top.egon.cola.component.ddc.admin.service.lease;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.ddc.admin.repository.DdcConfigLeaseRedisRepository;
import top.egon.cola.component.ddc.admin.repository.DdcInstanceRepository;
import top.egon.cola.component.ddc.admin.repository.DdcServiceRegistryRedisRepository;
import top.egon.cola.component.ddc.model.management.DdcResourceAdmissionRevocationRequest;
import top.egon.cola.component.ddc.model.management.DdcResourceAdmissionRevocationResult;

import java.time.Clock;
import java.util.Objects;

/**
 * 编排 Resource Server 停用后的精确三元组租约撤销。
 * / Orchestrates exact-triple lease revocation after a Resource Server is disabled.
 *
 * <p>Redis 删除操作和持久化离线更新本身都是幂等的；事件版本过滤防止旧事件撤销已使用
 * 新 Resource 版本重新建立的租约。</p>
 *
 * <p>Redis deletions and the persisted offline update are idempotent. Event-version filtering
 * prevents stale events from revoking leases re-established with a newer Resource version.</p>
 */
@Service
public class DdcResourceAdmissionRevocationService {

    /** 配置客户端租约仓储；configuration-client lease repository. */
    private final DdcConfigLeaseRedisRepository configLeases;

    /** Provider 租约仓储；provider lease repository. */
    private final DdcServiceRegistryRedisRepository providerLeases;

    /** 配置客户端审计实例仓储；configuration-client audit instance repository. */
    private final DdcInstanceRepository instances;

    /** UTC 业务时钟；UTC business clock. */
    private final Clock clock;

    /**
     * 使用系统 UTC 时钟创建生产撤销服务。
     * / Creates the production revocation service with the system UTC clock.
     */
    @Autowired
    public DdcResourceAdmissionRevocationService(
            DdcConfigLeaseRedisRepository configLeases,
            DdcServiceRegistryRedisRepository providerLeases,
            DdcInstanceRepository instances) {
        this(configLeases, providerLeases, instances, Clock.systemUTC());
    }

    /**
     * 使用可注入时钟创建撤销服务。
     * / Creates a revocation service with an injectable clock.
     */
    DdcResourceAdmissionRevocationService(
            DdcConfigLeaseRedisRepository configLeases,
            DdcServiceRegistryRedisRepository providerLeases,
            DdcInstanceRepository instances,
            Clock clock) {
        this.configLeases = Objects.requireNonNull(
                configLeases, "configLeases"
        );
        this.providerLeases = Objects.requireNonNull(
                providerLeases, "providerLeases"
        );
        this.instances = Objects.requireNonNull(instances, "instances");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * 幂等撤销停用版本覆盖的配置客户端与 Provider 租约。
     * / Idempotently revokes configuration-client and provider leases covered by the disabled
     * Resource version.
     *
     * @param request 精确 Resource 停用命令 / exact Resource disable command
     * @return 本次实际撤销统计 / counts actually revoked by this invocation
     */
    @Transactional
    public DdcResourceAdmissionRevocationResult revoke(
            DdcResourceAdmissionRevocationRequest request) {
        Objects.requireNonNull(request, "request");
        int configCount = configLeases.revokeResourceAdmission(
                request.resourceServerId(),
                request.bizCode(),
                request.env(),
                request.appCode(),
                request.resourceVersion()
        );
        int providerCount = providerLeases.revokeResourceAdmission(
                request.resourceServerId(),
                request.bizCode(),
                request.env(),
                request.appCode(),
                request.resourceVersion()
        );
        int persistedCount = instances.markResourceAdmissionOffline(
                request.resourceServerId(),
                request.bizCode(),
                request.env(),
                request.appCode(),
                request.resourceVersion(),
                clock.instant()
        );
        return new DdcResourceAdmissionRevocationResult(
                configCount,
                providerCount,
                persistedCount
        );
    }
}
