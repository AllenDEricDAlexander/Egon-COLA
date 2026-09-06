package top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.service.RoleResourceGrantService;
import top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.domain.dto.ReplaceRoleResourcesCommandDTO;
import top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.domain.dto.ReplaceRoleResourcesRequestDTO;
import top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.domain.vo.RoleResourceGrantMutationVO;
import top.egon.cola.platform.rbac3.admin.authorization.grant.roleresource.repository.RoleResourceGrantRepository;
import top.egon.cola.platform.rbac3.admin.authorization.resource.apibinding.repository.ResourceApiBindingRepository;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.repository.AuthorizationEventPublisher;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.domain.vo.AuthorizationEventVO;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.state.repository.TenantAuthorizationStateRepository;
import top.egon.cola.platform.rbac3.admin.authorization.runtime.state.domain.po.TenantAuthorizationStatePO;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RoleResourceGrantServiceTest {

    @Test
    void changedGrantsPersistARepairEventWithTheNewPolicyVersion() {
        var events = mock(AuthorizationEventPublisher.class);
        var service = service(events, 1L);
        service.replace(command());
        var event = ArgumentCaptor.forClass(AuthorizationEventVO.class);
        verify(events).enqueue(event.capture());
        assertEquals("ROLE_RESOURCE_CHANGED", event.getValue().eventType());
        assertEquals("200", event.getValue().tenantId());
        assertEquals("301", event.getValue().aggregateId());
        assertEquals("1", event.getValue().safePayload().get("policyVersion"));
    }

    @Test
    void noOpGrantReplacementDoesNotCreateAnotherRepairEvent() {
        var events = mock(AuthorizationEventPublisher.class);
        service(events, 0L).replace(command());
        verifyNoInteractions(events);
    }

    @Test
    void eventPersistenceFailureIsNotReportedAsSuccessfulReplacement() {
        var events = mock(AuthorizationEventPublisher.class);
        when(events.enqueue(any())).thenThrow(new IllegalStateException("outbox unavailable"));
        assertThrows(IllegalStateException.class, () -> service(events, 1L).replace(command()));
    }

    private RoleResourceGrantService service(AuthorizationEventPublisher events, long added) {
        var grants = mock(RoleResourceGrantRepository.class);
        var bindings = mock(ResourceApiBindingRepository.class);
        var state = mock(TenantAuthorizationStateRepository.class);
        var tenant = new TenantAuthorizationStatePO(200L, "actor", command().validFrom());
        tenant.incrementPolicyVersion("actor", command().validFrom());
        when(state.require(200L)).thenReturn(tenant);
        when(state.increment(200L, "actor")).thenReturn(1L);
        when(grants.replace(any())).thenReturn(new RoleResourceGrantRepository.ReplaceResult(Set.of(501L), 9L, added, 0L));
        return new RoleResourceGrantService(grants, bindings, state, events);
    }

    private ReplaceRoleResourcesCommandDTO command() {
        return new ReplaceRoleResourcesCommandDTO(200L, 71L, 301L, Set.of(501L),
                Instant.parse("2026-08-25T00:00:00Z"), null, 8L, "actor");
    }

    @Test
    void allPublicGrantOperationsKeepRepositoryLocksInsideATransaction() throws Exception {
        var transactions = new AnnotationTransactionAttributeSource();
        for (var method : List.of(
                RoleResourceGrantService.class.getMethod(
                        "tree", String.class, String.class, Instant.class),
                RoleResourceGrantService.class.getMethod(
                        "replace", String.class, String.class,
                        ReplaceRoleResourcesRequestDTO.class, String.class, Instant.class),
                RoleResourceGrantService.class.getMethod(
                        "replace", ReplaceRoleResourcesCommandDTO.class))) {
            var attribute = transactions.getTransactionAttribute(method, RoleResourceGrantService.class);
            assertNotNull(attribute, method.toString());
            assertFalse(attribute.isReadOnly(), "PostgreSQL locking reads require a writable transaction");
        }
    }

    @Test
    void requestAcceptsUniqueResourceIdsAndRejectsDuplicates() {
        ReplaceRoleResourcesRequestDTO request = new ReplaceRoleResourcesRequestDTO(
                List.of("501", "502"), null, null, 8L);
        assertEquals(List.of("501", "502"), request.resourceIds());
        assertThrows(IllegalArgumentException.class,
                () -> new ReplaceRoleResourcesRequestDTO(
                        List.of("501", "501"), null, null, 8L));
    }

    @Test
    void commandAndMutationNeverCarryPermissionCharacters() throws Exception {
        ReplaceRoleResourcesCommandDTO command = new ReplaceRoleResourcesCommandDTO(
                200L, 71L, 301L, Set.of(501L),
                Instant.parse("2026-08-25T00:00:00Z"), null, 8L, "actor");
        RoleResourceGrantMutationVO mutation = RoleResourceGrantMutationVO.success(
                command.roleId(), 9L, Set.of(501L), Set.of(701L),
                Set.of(501L, 701L), 1L, 0L, 2L, 12L, 4L);

        assertEquals("301", mutation.roleId());
        assertEquals(List.of("501"), mutation.directResourceIds());
        assertEquals(List.of("701"), mutation.derivedApiResourceIds());
        assertThrows(NoSuchMethodException.class,
                () -> command.getClass().getDeclaredMethod("permissionCode"));
    }
}
