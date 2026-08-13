package top.egon.cola.platform.rbac3.admin.interfaces.http;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.idp.starter.security.RequiresServiceScope;
import top.egon.cola.platform.rbac3.admin.authorization.service.AuthorizationDecisionService;
import top.egon.cola.platform.rbac3.admin.snapshot.application.SystemAuthorizationSnapshotService;
import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.net.URI;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import top.egon.cola.platform.rbac3.admin.shared.domain.vo.ApiEnvelopeVO;
import top.egon.cola.platform.rbac3.admin.authorization.domain.dto.DecisionRequestDTO;
import top.egon.cola.platform.rbac3.admin.authorization.domain.dto.ResourceAccessRequestDTO;
import top.egon.cola.platform.rbac3.admin.authorization.domain.vo.ResourceAccessDecisionVO;
import top.egon.cola.platform.rbac3.admin.authorization.domain.dto.AuthorizationFenceRequestDTO;
import top.egon.cola.platform.rbac3.admin.authorization.controller.InternalAuthorizationController;
import top.egon.cola.platform.rbac3.admin.authorization.domain.dto.ResourceAccessDecisionRequestDTO;
import top.egon.cola.platform.rbac3.admin.authorization.domain.vo.ResourceAccessDecisionResponseVO;

class InternalAuthorizationControllerTest {

    @Test
    void serviceTokenCanLoadItsRbacSystemForTheExactTenant() {
        SystemAuthorizationSnapshotService snapshots = mock(
                SystemAuthorizationSnapshotService.class);
        SystemAuthorizationSnapshot expected = new SystemAuthorizationSnapshot(
                "tenant-1", "alice-sub", "9", "99", "rbac3-admin",
                1L, 0L, 2L, List.of(),
                Set.of("system:role-activation:read"), Map.of(), Map.of(),
                "empty:0", Instant.parse("2026-08-02T00:00:00Z"),
                Instant.parse("2026-08-02T01:00:00Z"));
        when(snapshots.snapshot("tenant-1", "99", "rbac3-admin", "alice-sub"))
                .thenReturn(expected);
        InternalAuthorizationController controller =
                new InternalAuthorizationController(
                        mock(AuthorizationDecisionService.class), snapshots);
        ServiceIdentityPrincipal service = service(
                "tenant-1",
                "rbac3",
                "service:authorization:snapshot"
        );

        ApiEnvelopeVO<SystemAuthorizationSnapshot> response =
                controller.systemSnapshot(
                        "tenant-1", "99", "rbac3-admin", "alice-sub", service);

        assertEquals(expected, response.data());
    }

    @Test
    void serviceTokenCannotLoadAnRbacSystemAcrossTenants() {
        InternalAuthorizationController controller =
                new InternalAuthorizationController(
                        mock(AuthorizationDecisionService.class),
                        mock(SystemAuthorizationSnapshotService.class));

        assertThrows(Rbac3RuleViolation.class, () -> controller.systemSnapshot(
                "tenant-1", "99", "rbac3-admin", "alice-sub",
                service("tenant-2", "rbac3", "service:authorization:snapshot")));
    }

    @Test
    void resourceAccessEndpointReturnsOnlyDecisionReasonAndAuthorizationVersions() {
        AuthorizationDecisionService decisions = mock(AuthorizationDecisionService.class);
        ResourceAccessRequestDTO command =
                new ResourceAccessRequestDTO(
                        "alice-sub", "tenant-1", "session-1",
                        "finance-web", "finance:payment:approve");
        ResourceAccessDecisionVO expected =
                new ResourceAccessDecisionVO(
                        top.egon.cola.platform.rbac3.contract.authorization.Decision.ALLOW,
                        "ALLOW", 43L, 2L, 18L,
                        Instant.parse("2026-08-10T00:00:00Z"));
        ServiceIdentityPrincipal principal = service(
                "tenant-1",
                "idp-admin",
                "service:authorization:decide"
        );
        when(decisions.decideResourceAccess(principal, command)).thenReturn(expected);
        SystemAuthorizationSnapshotService snapshots = mock(
                SystemAuthorizationSnapshotService.class);
        InternalAuthorizationController controller = new InternalAuthorizationController(
                decisions, snapshots);

        ApiEnvelopeVO<ResourceAccessDecisionResponseVO> envelope =
                controller.decideResourceAccess(
                        new ResourceAccessDecisionRequestDTO(
                                "alice-sub", "tenant-1", "session-1",
                                "finance-web", "finance:payment:approve"),
                        principal);

        assertThat(envelope.data()).isEqualTo(new ResourceAccessDecisionResponseVO(
                top.egon.cola.platform.rbac3.contract.authorization.Decision.ALLOW,
                "ALLOW", 43L, 2L, 18L,
                Instant.parse("2026-08-10T00:00:00Z")));
        assertThat(Arrays.stream(ResourceAccessDecisionResponseVO.class.getRecordComponents())
                .map(component -> component.getName()))
                .containsExactly("decision", "reasonCode", "authVersion",
                        "sessionVersion", "policyVersion", "decidedAt");
        verify(decisions).decideResourceAccess(principal, command);
        verify(snapshots).snapshot(
                "tenant-1", "session-1", "finance-web", "alice-sub");
    }

    @Test
    void internalAuthorizationOperationsUseIdpScopesAndServicePrincipal()
            throws NoSuchMethodException {
        assertServiceOperation("systemSnapshot", "service:authorization:snapshot",
                String.class, String.class, String.class, String.class,
                ServiceIdentityPrincipal.class);
        assertServiceOperation("snapshot", "service:authorization:snapshot",
                String.class, ServiceIdentityPrincipal.class);
        assertServiceOperation("decide", "service:authorization:decide",
                DecisionRequestDTO.class,
                ServiceIdentityPrincipal.class);
        assertServiceOperation("decideResourceAccess", "service:authorization:decide",
                ResourceAccessDecisionRequestDTO.class, ServiceIdentityPrincipal.class);
        assertServiceOperation("verifyFence", "service:authorization:fence",
                AuthorizationFenceRequestDTO.class,
                ServiceIdentityPrincipal.class);
    }

    private void assertServiceOperation(
            String methodName,
            String expectedScope,
            Class<?>... parameterTypes
    ) throws NoSuchMethodException {
        Method method = InternalAuthorizationController.class.getMethod(
                methodName, parameterTypes);
        assertThat(method.getAnnotation(RequiresServiceScope.class).value())
                .isEqualTo(expectedScope);
        assertThat(method.getParameters()[method.getParameterCount() - 1]
                .getAnnotation(AuthenticationPrincipal.class)).isNotNull();
        assertThat(method.getParameterTypes()[method.getParameterCount() - 1])
                .isEqualTo(ServiceIdentityPrincipal.class);
    }

    private ServiceIdentityPrincipal service(
            String tenantId,
            String sourceApp,
            String scope
    ) {
        Instant issuedAt = Instant.parse("2026-08-10T00:00:00Z");
        return new ServiceIdentityPrincipal(
                "idp-service",
                tenantId,
                "idp-service",
                "service-token-1",
                URI.create("https://api.example/prod/permission/rbac3"),
                12L,
                Set.of(scope),
                "permission",
                sourceApp,
                "prod",
                "credential-1",
                issuedAt,
                issuedAt.plusSeconds(300)
        );
    }
}
