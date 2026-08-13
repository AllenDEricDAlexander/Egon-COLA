package top.egon.cola.platform.rbac3.admin.identity.application;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import top.egon.cola.platform.rbac3.admin.identity.repository.IdentityMappingRepository;
import top.egon.cola.platform.rbac3.admin.identity.service.internal.MappingIdGenerator;
import top.egon.cola.platform.rbac3.admin.identity.domain.vo.MappingVO;
import top.egon.cola.platform.rbac3.admin.identity.domain.vo.ResolvedMembershipVO;
import top.egon.cola.platform.rbac3.admin.identity.domain.vo.TenantMembershipVO;
import top.egon.cola.platform.rbac3.admin.identity.domain.exception.DuplicateIdentityMappingException;
import top.egon.cola.platform.rbac3.admin.identity.service.IdentityMappingFacade;

class IdentityMappingFacadeTest {

    private static final Instant NOW = Instant.parse("2026-08-02T02:00:00Z");

    @Test
    void sameGlobalIdentityMayMapToTwoTenantsButOnlyOncePerTenant() {
        InMemoryStore store = new InMemoryStore();
        IdentityMappingFacade facade = new IdentityMappingFacade(store, new SequenceIds());

        facade.bind("1", "alice-sub", "101", "test", NOW);
        facade.bind("2", "alice-sub", "201", "test", NOW);

        assertThat(facade.tenants("alice-sub", "idp-admin"))
                .extracting(TenantMembershipVO::tenantId)
                .containsExactly("1", "2");
        assertThatThrownBy(() -> facade.bind(
                "1", "alice-sub", "102", "test", NOW))
                .isInstanceOf(DuplicateIdentityMappingException.class);
    }

    @Test
    void repeatedBindingOfSameUserIsIdempotentAndResolveRequiresActiveFacts() {
        InMemoryStore store = new InMemoryStore();
        IdentityMappingFacade facade = new IdentityMappingFacade(store, new SequenceIds());

        MappingVO first = facade.bind(
                "1", "alice-sub", "101", "test", NOW);
        MappingVO repeated = facade.bind(
                "1", "alice-sub", "101", "test", NOW.plusSeconds(1));

        assertThat(repeated).isEqualTo(first);
        assertThat(facade.resolve("alice-sub", "1", "gateway-admin"))
                .get()
                .extracting(ResolvedMembershipVO::rbac3UserId)
                .isEqualTo("101");
        store.active = false;
        assertThat(facade.resolve("alice-sub", "1", "gateway-admin")).isEmpty();
    }

    private static final class SequenceIds implements MappingIdGenerator {

        private long value;

        @Override
        public long nextId() {
            return ++value;
        }
    }

    private static final class InMemoryStore implements IdentityMappingRepository {

        private final List<MappingVO> mappings = new ArrayList<>();
        private boolean active = true;

        @Override
        public Optional<MappingVO> find(
                String tenantId, String identitySub) {
            return mappings.stream()
                    .filter(mapping -> mapping.tenantId().equals(tenantId))
                    .filter(mapping -> mapping.identitySub().equals(identitySub))
                    .findFirst();
        }

        @Override
        public MappingVO create(
                long mappingId,
                String tenantId,
                String identitySub,
                String rbac3UserId,
                String actorId,
                Instant now) {
            MappingVO mapping = new MappingVO(
                    Long.toString(mappingId), tenantId, identitySub, rbac3UserId,
                    true, now);
            mappings.add(mapping);
            return mapping;
        }

        @Override
        public Optional<ResolvedMembershipVO> resolve(
                String tenantId, String identitySub) {
            if (!active) {
                return Optional.empty();
            }
            return find(tenantId, identitySub).map(mapping ->
                    new ResolvedMembershipVO(
                            mapping.tenantId(), "tenant-" + mapping.tenantId(),
                            "Tenant " + mapping.tenantId(), mapping.identitySub(),
                            mapping.rbac3UserId(), "User " + mapping.rbac3UserId(),
                            true, 0, 0));
        }

        @Override
        public List<TenantMembershipVO> tenants(String identitySub) {
            if (!active) {
                return List.of();
            }
            return mappings.stream()
                    .filter(mapping -> mapping.identitySub().equals(identitySub))
                    .map(mapping -> new TenantMembershipVO(
                            mapping.tenantId(), "tenant-" + mapping.tenantId(),
                            "Tenant " + mapping.tenantId(), mapping.rbac3UserId(),
                            "User " + mapping.rbac3UserId()))
                    .toList();
        }
    }
}
