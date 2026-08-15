package top.egon.cola.platform.rbac3.admin.iam.role.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import top.egon.cola.platform.rbac3.admin.iam.business.repository.UserBusinessAccessRepository;
import top.egon.cola.platform.rbac3.admin.iam.business.service.ApplicationCatalogEntry;
import top.egon.cola.platform.rbac3.admin.iam.business.service.DdcCatalogGateway;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleEligibilityServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-15T00:00:00Z");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final UserBusinessAccessRepository businessAccessStore =
            mock(UserBusinessAccessRepository.class);
    private final DdcCatalogGateway catalog = mock(DdcCatalogGateway.class);
    private final RoleEligibilityService service = new RoleEligibilityService(
            entityManager, businessAccessStore, catalog);

    @Test
    void requiresAnEffectiveBusinessGrant() {
        stubApplicationLookup();
        when(businessAccessStore.effectiveBusinessIds(7L, 9L, NOW))
                .thenReturn(Set.of());

        assertThat(service.isEffective("7", "9", "1", NOW)).isFalse();
    }

    @Test
    void acceptsActiveLocalAndDdcScopesWithAnEffectiveBusinessGrant() {
        stubApplicationLookup();
        when(businessAccessStore.effectiveBusinessIds(7L, 9L, NOW))
                .thenReturn(Set.of("biz-1"));
        when(catalog.findApplication("ddc-app-1")).thenReturn(Optional.of(
                new ApplicationCatalogEntry(
                        "ddc-app-1", "biz-1", "orders",
                        "console", "Console", true, true)));

        boolean actual = service.isEffective("7", "9", "1", NOW);

        verify(businessAccessStore).effectiveBusinessIds(7L, 9L, NOW);
        verify(catalog).findApplication("ddc-app-1");
        assertThat(actual).isTrue();
    }

    @Test
    void resolvesEffectiveApplicationScopeWithDdcBizAndAppIdentity() {
        stubApplicationLookup();
        when(businessAccessStore.effectiveBusinessIds(7L, 9L, NOW))
                .thenReturn(Set.of("biz-1"));
        when(catalog.findApplication("ddc-app-1")).thenReturn(Optional.of(
                new ApplicationCatalogEntry(
                        "ddc-app-1", "biz-1", "orders",
                        "console", "Console", true, true)));

        Optional<EffectiveApplicationScope> actual = service.resolveEffectiveScope(
                "7", "9", "1", NOW);

        assertThat(actual).contains(new EffectiveApplicationScope(
                "biz-1", "orders", "ddc-app-1", "console"));
    }

    @ParameterizedTest
    @MethodSource("invalidDdcScopes")
    void rejectsDisabledOrMismatchedDdcScopes(ApplicationCatalogEntry entry) {
        stubApplicationLookup();
        when(businessAccessStore.effectiveBusinessIds(7L, 9L, NOW))
                .thenReturn(Set.of("biz-1"));
        when(catalog.findApplication("ddc-app-1")).thenReturn(Optional.of(entry));

        assertThat(service.resolveEffectiveScope("7", "9", "1", NOW)).isEmpty();
        assertThat(service.isEffective("7", "9", "1", NOW)).isFalse();
    }

    @Test
    void failsClosedWhenDdcLookupIsUnavailable() {
        stubApplicationLookup();
        when(businessAccessStore.effectiveBusinessIds(7L, 9L, NOW))
                .thenReturn(Set.of("biz-1"));
        when(catalog.findApplication("ddc-app-1"))
                .thenThrow(new IllegalStateException("rpc unavailable"));

        assertThat(service.isEffective("7", "9", "1", NOW)).isFalse();
    }

    @Test
    void rejectsRoleAssignmentWhenItsBusinessIsNotEffective() {
        Query roleQuery = mock(Query.class);
        Query applicationQuery = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0, String.class);
            return sql.contains("from rbac3_role") ? roleQuery : applicationQuery;
        });
        when(roleQuery.setParameter(anyString(), any())).thenReturn(roleQuery);
        when(applicationQuery.setParameter(anyString(), any())).thenReturn(applicationQuery);
        when(roleQuery.getResultList()).thenReturn(List.of(1L));
        when(applicationQuery.getResultList()).thenReturn(Collections.singletonList(
                new Object[]{1L, "ddc-app-1", "biz-1", "ACTIVE"}));
        when(businessAccessStore.effectiveBusinessIds(7L, 9L, NOW))
                .thenReturn(Set.of());

        assertThatThrownBy(() -> service.requireEffectiveRole("7", "9", "10", NOW))
                .isInstanceOf(Rbac3RuleViolation.class)
                .extracting(exception -> ((Rbac3RuleViolation) exception).reasonCode())
                .isEqualTo("BUSINESS_ACCESS_REQUIRED");
    }

    private void stubApplicationLookup() {
        Query applicationQuery = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(applicationQuery);
        when(applicationQuery.setParameter(anyString(), any())).thenReturn(applicationQuery);
        when(applicationQuery.getResultList()).thenReturn(Collections.singletonList(
                new Object[]{1L, "ddc-app-1", "biz-1", "ACTIVE"}));
    }

    private static Stream<ApplicationCatalogEntry> invalidDdcScopes() {
        return Stream.of(
                new ApplicationCatalogEntry(
                        "ddc-app-1", "biz-1", "orders",
                        "console", "Console", false, true),
                new ApplicationCatalogEntry(
                        "ddc-app-1", "biz-1", "orders",
                        "console", "Console", true, false),
                new ApplicationCatalogEntry(
                        "ddc-app-1", "biz-2", "orders",
                        "console", "Console", true, true),
                new ApplicationCatalogEntry(
                        "ddc-app-2", "biz-1", "orders",
                        "console", "Console", true, true));
    }
}
