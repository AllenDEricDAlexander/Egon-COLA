package top.egon.cola.platform.tianquan.shoubing.admin.tenant.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.repo.IdentityDirectoryContext;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.bo.IdentityTenantBO;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.repo.IdentityTenantRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.service.TenantService;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Default transactional tenant catalog application service. */
@Service
public class TenantServiceImpl implements TenantService {

    private final IdentityTenantRepository tenants;
    private final IdentityDirectoryContext directoryContext;
    private final Clock clock;

    public TenantServiceImpl(
            IdentityTenantRepository tenants,
            IdentityDirectoryContext directoryContext,
            Clock clock) {
        this.tenants = Objects.requireNonNull(tenants, "tenants");
        this.directoryContext = Objects.requireNonNull(directoryContext, "directoryContext");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantView> list() {
        return tenants.findAllByOrderByUpdatedAtDescIdAsc().stream()
                .map(this::view)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TenantView get(String tenantId) {
        return view(find(tenantId));
    }

    @Override
    @Transactional
    public TenantView create(CreateTenantCommand command) {
        Objects.requireNonNull(command, "command");
        String tenantCode = normalizeCode(command.tenantCode());
        if (tenants.existsByTenantCodeIgnoreCase(tenantCode)) {
            throw new IllegalStateException("tenant code already exists");
        }
        long numericId = SnowflakeIdGenerator.nextLongId();
        if (numericId <= 0L) {
            throw new IllegalStateException("tenant id generator returned invalid value");
        }
        if (numericId == directoryContext.directoryPartitionId()) {
            throw new IllegalStateException("tenant id is reserved for platform storage");
        }
        IdentityTenantBO tenant = IdentityTenantBO.create(
                Long.toString(numericId),
                tenantCode,
                command.tenantName(),
                command.settings(),
                command.operatorSub(),
                clock.instant()
        );
        return view(tenants.save(tenant));
    }

    @Override
    @Transactional
    public TenantView update(String tenantId, UpdateTenantCommand command) {
        Objects.requireNonNull(command, "command");
        IdentityTenantBO tenant = tenants.findByIdForUpdate(required(tenantId, "tenantId"))
                .orElseThrow(() -> new IllegalStateException("tenant not found"));
        tenant.update(
                command.tenantName(),
                command.settings(),
                Objects.requireNonNull(command.status(), "status"),
                command.expectedVersion(),
                command.operatorSub(),
                clock.instant()
        );
        return view(tenants.save(tenant));
    }

    private IdentityTenantBO find(String tenantId) {
        return tenants.findById(required(tenantId, "tenantId"))
                .orElseThrow(() -> new IllegalStateException("tenant not found"));
    }

    private TenantView view(IdentityTenantBO tenant) {
        return new TenantView(
                tenant.getIdentityTenantId(),
                tenant.getTenantCode(),
                tenant.getTenantName(),
                tenant.getStatus(),
                tenant.getSettings(),
                tenant.getVersion(),
                tenant.getCreatedAt(),
                tenant.getUpdatedAt()
        );
    }

    private static String normalizeCode(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is invalid");
        }
        return value;
    }
}
