package top.egon.cola.platform.rbac3.admin.interfaces.http;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.authorization.application.AuthorizationDecisionService;
import top.egon.cola.platform.rbac3.admin.security.CurrentRbac3ServicePrincipal;
import top.egon.cola.platform.rbac3.admin.snapshot.application.SystemAuthorizationSnapshotService;
import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InternalAuthorizationControllerTest {

    @Test
    void platformServiceCredentialCanLoadItsSystemAcrossTenants() {
        SystemAuthorizationSnapshotService snapshots = mock(
                SystemAuthorizationSnapshotService.class);
        SystemAuthorizationSnapshot expected = new SystemAuthorizationSnapshot(
                "7", "alice-sub", "9", "99", "rbac3-admin",
                1L, 0L, 2L, List.of(),
                Set.of("system:role-activation:read"), Map.of(), Map.of(),
                "empty:0", Instant.parse("2026-08-02T00:00:00Z"),
                Instant.parse("2026-08-02T01:00:00Z"));
        when(snapshots.snapshot("7", "99", "rbac3-admin", "alice-sub"))
                .thenReturn(expected);
        InternalAuthorizationController controller =
                new InternalAuthorizationController(
                        mock(AuthorizationDecisionService.class), snapshots);
        CurrentRbac3ServicePrincipal service =
                new CurrentRbac3ServicePrincipal(
                        "*", "rbac3-admin-service", "rbac3-admin",
                        "local", "default", "credential-1",
                        Set.of("service:authorization:snapshot"));

        ApiEnvelope<SystemAuthorizationSnapshot> response =
                controller.systemSnapshot(
                        "7", "99", "rbac3-admin", "alice-sub", service);

        assertEquals(expected, response.data());
    }
}
