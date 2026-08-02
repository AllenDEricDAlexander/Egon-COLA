package top.egon.cola.platform.rbac3.admin.identity.application;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentityMappingFacadeTest {

    private static final Instant NOW = Instant.parse("2026-08-02T02:00:00Z");

    @Test
    void sameGlobalIdentityMayMapToTwoTenantsButOnlyOncePerTenant() {
        InMemoryStore store = new InMemoryStore();
        IdentityMappingFacade facade = new IdentityMappingFacade(store, new SequenceIds());

        facade.bind("1", "alice-sub", "101", "test", NOW);
        facade.bind("2", "alice-sub", "201", "test", NOW);

        assertThat(facade.tenants("alice-sub", "idp-admin"))
                .extracting(IdentityMappingFacade.TenantMembership::tenantId)
                .containsExactly("1", "2");
        assertThatThrownBy(() -> facade.bind(
                "1", "alice-sub", "102", "test", NOW))
                .isInstanceOf(IdentityMappingFacade.DuplicateIdentityMappingException.class);
    }

    @Test
    void repeatedBindingOfSameUserIsIdempotentAndResolveRequiresActiveFacts() {
        InMemoryStore store = new InMemoryStore();
        IdentityMappingFacade facade = new IdentityMappingFacade(store, new SequenceIds());

        IdentityMappingFacade.Mapping first = facade.bind(
                "1", "alice-sub", "101", "test", NOW);
        IdentityMappingFacade.Mapping repeated = facade.bind(
                "1", "alice-sub", "101", "test", NOW.plusSeconds(1));

        assertThat(repeated).isEqualTo(first);
        assertThat(facade.resolve("alice-sub", "1", "gateway-admin"))
                .get()
                .extracting(IdentityMappingFacade.ResolvedMembership::rbac3UserId)
                .isEqualTo("101");
        store.active = false;
        assertThat(facade.resolve("alice-sub", "1", "gateway-admin")).isEmpty();
    }

    private static final class SequenceIds implements IdentityMappingFacade.MappingIdGenerator {

        private long value;

        @Override
        public long nextId() {
            return ++value;
        }
    }

    private static final class InMemoryStore implements IdentityMappingFacade.MappingStore {

        private final List<IdentityMappingFacade.Mapping> mappings = new ArrayList<>();
        private boolean active = true;

        @Override
        public Optional<IdentityMappingFacade.Mapping> find(
                String tenantId, String identitySub) {
            return mappings.stream()
                    .filter(mapping -> mapping.tenantId().equals(tenantId))
                    .filter(mapping -> mapping.identitySub().equals(identitySub))
                    .findFirst();
        }

        @Override
        public IdentityMappingFacade.Mapping create(
                long mappingId,
                String tenantId,
                String identitySub,
                String rbac3UserId,
                String actorId,
                Instant now) {
            IdentityMappingFacade.Mapping mapping = new IdentityMappingFacade.Mapping(
                    Long.toString(mappingId), tenantId, identitySub, rbac3UserId,
                    true, now);
            mappings.add(mapping);
            return mapping;
        }

        @Override
        public Optional<IdentityMappingFacade.ResolvedMembership> resolve(
                String tenantId, String identitySub) {
            if (!active) {
                return Optional.empty();
            }
            return find(tenantId, identitySub).map(mapping ->
                    new IdentityMappingFacade.ResolvedMembership(
                            mapping.tenantId(), "tenant-" + mapping.tenantId(),
                            "Tenant " + mapping.tenantId(), mapping.identitySub(),
                            mapping.rbac3UserId(), "User " + mapping.rbac3UserId(),
                            true));
        }

        @Override
        public List<IdentityMappingFacade.TenantMembership> tenants(String identitySub) {
            if (!active) {
                return List.of();
            }
            return mappings.stream()
                    .filter(mapping -> mapping.identitySub().equals(identitySub))
                    .map(mapping -> new IdentityMappingFacade.TenantMembership(
                            mapping.tenantId(), "tenant-" + mapping.tenantId(),
                            "Tenant " + mapping.tenantId(), mapping.rbac3UserId(),
                            "User " + mapping.rbac3UserId()))
                    .toList();
        }
    }
}
