package top.egon.cola.platform.idp.admin.tenant.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.idp.admin.identity.repo.IdentityUserDirectory;
import top.egon.cola.platform.idp.admin.tenant.domain.pojo.IdentityTenantEntity;
import top.egon.cola.platform.idp.admin.tenant.domain.pojo.IdentityTenantMembershipEntity;
import top.egon.cola.platform.idp.admin.tenant.repo.IdentityTenantMembershipRepository;
import top.egon.cola.platform.idp.admin.tenant.repo.IdentityTenantRepository;
import top.egon.cola.platform.idp.admin.tenant.service.TenantMembershipService;
import top.egon.cola.platform.idp.core.identity.IdentityUser;
import top.egon.cola.platform.idp.core.identity.IdentityUserStatus;
import top.egon.cola.platform.idp.core.port.TenantMembershipPort;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

/** Default transactional tenant-membership service backed only by IdP state. */
@Service
public class TenantMembershipServiceImpl implements TenantMembershipService {

    private final IdentityTenantRepository tenants;
    private final IdentityTenantMembershipRepository memberships;
    private final IdentityUserDirectory users;
    private final LongIdGenerator ids;
    private final Clock clock;

    public TenantMembershipServiceImpl(
            IdentityTenantRepository tenants,
            IdentityTenantMembershipRepository memberships,
            IdentityUserDirectory users,
            LongIdGenerator ids,
            Clock clock
    ) {
        this.tenants = Objects.requireNonNull(tenants, "tenants");
        this.memberships = Objects.requireNonNull(memberships, "memberships");
        this.users = Objects.requireNonNull(users, "users");
        this.ids = Objects.requireNonNull(ids, "ids");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    @Transactional(readOnly = true)
    public List<MembershipView> listByTenant(String tenantId) {
        IdentityTenantEntity tenant = findTenant(tenantId);
        return memberships.findByTenantIdOrderByUpdatedAtDescIdentitySubAsc(
                        tenant.getId()
                ).stream()
                .map(this::view)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantMembershipProfile> listByIdentity(String identitySub) {
        IdentityUser user = findUser(identitySub);
        return memberships.findByIdentitySubOrderByUpdatedAtDescTenantIdAsc(
                        user.id()
                ).stream()
                .map(membership -> profile(
                        findTenant(membership.getTenantId()),
                        user,
                        membership
                ))
                .toList();
    }

    @Override
    @Transactional
    public MembershipView upsert(UpsertMembershipCommand command) {
        Objects.requireNonNull(command, "command");
        IdentityTenantEntity tenant = findTenant(command.tenantId());
        IdentityUser user = findUser(command.identitySub());
        IdentityTenantMembershipEntity.Status status = Objects.requireNonNull(
                command.status(),
                "status"
        );
        if (status == IdentityTenantMembershipEntity.Status.ACTIVE
                && tenant.getStatus() == IdentityTenantEntity.Status.CLOSED) {
            throw new IllegalStateException(
                    "closed tenant cannot activate membership"
            );
        }
        IdentityTenantMembershipEntity membership = memberships
                .findByTenantIdAndIdentitySub(tenant.getId(), user.id())
                .orElse(null);
        if (membership == null) {
            if (command.expectedVersion() != null) {
                throw new IllegalStateException(
                        "membership version conflict"
                );
            }
            long numericId = ids.nextLongId();
            if (numericId <= 0L) {
                throw new IllegalStateException(
                        "membership id generator returned invalid value"
                );
            }
            membership = IdentityTenantMembershipEntity.create(
                    Long.toString(numericId),
                    tenant.getId(),
                    user.id(),
                    status,
                    command.operatorSub(),
                    clock.instant()
            );
            return view(memberships.save(membership));
        }
        if (command.expectedVersion() == null) {
            throw new IllegalStateException(
                    "membership expectedVersion is required"
            );
        }
        if (membership.getStatus() == status
                && membership.getVersion() == command.expectedVersion()) {
            return view(membership);
        }
        membership.update(
                status,
                command.expectedVersion(),
                command.operatorSub(),
                clock.instant()
        );
        return view(memberships.save(membership));
    }

    @Override
    @Transactional(readOnly = true)
    public TenantMembershipProfile resolve(
            String identitySub,
            String tenantId
    ) {
        IdentityUser user = findUser(identitySub);
        IdentityTenantEntity tenant = findTenant(tenantId);
        IdentityTenantMembershipEntity membership = memberships
                .findByTenantIdAndIdentitySub(tenant.getId(), user.id())
                .orElseThrow(() -> new IllegalStateException(
                        "membership not found"
                ));
        return profile(tenant, user, membership);
    }

    private IdentityTenantEntity findTenant(String tenantId) {
        String value = required(tenantId, "tenantId");
        return tenants.findById(value)
                .orElseThrow(() -> new IllegalStateException(
                        "tenant not found"
                ));
    }

    private IdentityUser findUser(String identitySub) {
        String value = required(identitySub, "identitySub");
        return users.list().stream()
                .filter(user -> user.id().equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "identity user not found"
                ));
    }

    private MembershipView view(IdentityTenantMembershipEntity membership) {
        IdentityUser user = findUser(membership.getIdentitySub());
        return new MembershipView(
                membership.getTenantId(),
                membership.getIdentitySub(),
                user.displayName(),
                membership.getStatus(),
                membership.getVersion(),
                membership.getUpdatedAt()
        );
    }

    private TenantMembershipProfile profile(
            IdentityTenantEntity tenant,
            IdentityUser user,
            IdentityTenantMembershipEntity membership
    ) {
        boolean active = tenant.getStatus() == IdentityTenantEntity.Status.ACTIVE
                && user.status() == IdentityUserStatus.ACTIVE
                && membership.getStatus()
                == IdentityTenantMembershipEntity.Status.ACTIVE;
        return new TenantMembershipProfile(
                user.id(),
                tenant.getId(),
                tenant.getTenantName(),
                user.displayName(),
                tenant.getStatus(),
                user.status(),
                membership.getStatus(),
                active
                        ? TenantMembershipPort.MembershipStatus.ACTIVE
                        : TenantMembershipPort.MembershipStatus.DISABLED,
                membership.getVersion(),
                membership.getUpdatedAt()
        );
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is invalid");
        }
        return value;
    }
}
