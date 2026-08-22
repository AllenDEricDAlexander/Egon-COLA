package top.egon.cola.platform.idp.admin.tenant.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.idp.admin.tenant.domain.pojo.IdentityTenantEntity;
import top.egon.cola.platform.idp.admin.tenant.repo.IdentityTenantRepository;
import top.egon.cola.platform.idp.admin.tenant.service.TenantService;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Default transactional tenant catalog application service. */
@Service
public class TenantServiceImpl implements TenantService {

    private final IdentityTenantRepository tenants;
    private final LongIdGenerator ids;
    private final Clock clock;

    public TenantServiceImpl(
            IdentityTenantRepository tenants,
            LongIdGenerator ids,
            Clock clock
    ) {
        this.tenants = Objects.requireNonNull(tenants, "tenants");
        this.ids = Objects.requireNonNull(ids, "ids");
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
        long numericId = ids.nextLongId();
        if (numericId <= 0L) {
            throw new IllegalStateException("tenant id generator returned invalid value");
        }
        IdentityTenantEntity tenant = IdentityTenantEntity.create(
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
    public TenantView update(
            String tenantId,
            UpdateTenantCommand command
    ) {
        Objects.requireNonNull(command, "command");
        IdentityTenantEntity tenant = tenants.findByIdForUpdate(
                        required(tenantId, "tenantId")
                )
                .orElseThrow(() -> new IllegalStateException(
                        "tenant not found"
                ));
        IdentityTenantEntity.Status status = Objects.requireNonNull(
                command.status(),
                "status"
        );
        tenant.update(
                command.tenantName(),
                command.settings(),
                status,
                command.expectedVersion(),
                command.operatorSub(),
                clock.instant()
        );
        return view(tenants.save(tenant));
    }

    private IdentityTenantEntity find(String tenantId) {
        return tenants.findById(required(tenantId, "tenantId"))
                .orElseThrow(() -> new IllegalStateException(
                        "tenant not found"
                ));
    }

    private TenantView view(IdentityTenantEntity tenant) {
        return new TenantView(
                tenant.getId(),
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
        return value == null
                ? null
                : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is invalid");
        }
        return value;
    }
}
