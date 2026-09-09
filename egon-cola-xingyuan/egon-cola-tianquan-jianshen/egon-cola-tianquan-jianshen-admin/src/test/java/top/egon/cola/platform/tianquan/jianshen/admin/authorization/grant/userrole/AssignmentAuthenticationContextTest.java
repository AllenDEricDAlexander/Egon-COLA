package top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.userrole;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import top.egon.cola.platform.tianquan.shoubing.contract.AuthenticationContext;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.userrole.controller.AssignmentController;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.userrole.domain.dto.AssignRequestDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.userrole.domain.dto.RoleAssignmentChangeDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.userrole.domain.dto.RoleAssignmentChangeRequestDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.userrole.domain.dto.RoleAssignmentDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.userrole.domain.vo.AssignmentResultVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.userrole.service.AssignmentFacade;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.domain.vo.IdempotencyClaimVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.service.IdempotencyService;
import top.egon.cola.platform.tianquan.jianshen.admin.shared.domain.DatabaseClock;
import top.egon.cola.platform.tianquan.jianshen.admin.shared.tenant.domain.TenantContext;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3UserDetails;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssignmentAuthenticationContextTest {

    @AfterEach
    void clearRequestContext() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @ParameterizedTest
    @ValueSource(strings = {"PASSWORD", "MFA", "STRONG"})
    void assignmentAndStateChangesKeepTheVerifiedIdpAuthenticationStrength(String acr) {
        Instant now = Instant.parse("2026-09-06T05:00:00Z");
        Rbac3UserDetails principal = mock(Rbac3UserDetails.class);
        when(principal.rbac3UserId()).thenReturn("2");
        when(principal.identity()).thenReturn(new IdentityPrincipal(
                "subject-2", "1", "token-1", Set.of("tianquan-jianshen"), now, now.plusSeconds(3600),
                AuthenticationContext.of(acr, now)));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        TenantContext.set(new TenantContext("1", "1", false));

        AssignmentFacade facade = mock(AssignmentFacade.class);
        IdempotencyService idempotency = mock(IdempotencyService.class);
        DatabaseClock clock = mock(DatabaseClock.class);
        when(clock.transactionNow()).thenReturn(now);
        when(idempotency.claim(any())).thenReturn(mock(IdempotencyClaimVO.class));
        AssignmentResultVO result = mock(AssignmentResultVO.class);
        when(result.completed()).thenReturn(true);
        when(facade.assign(any())).thenReturn(result);
        when(facade.change(any())).thenReturn(result);
        var controller = new AssignmentController(facade, idempotency, clock);

        controller.assign("3", new AssignRequestDTO(
                "4", now, now.plusSeconds(3600), "DIRECT", "qa", null, 5), "create-1");
        var change = new RoleAssignmentChangeRequestDTO("qa", null, 1, 5);
        controller.suspend("3", "6", change, "suspend-1");
        controller.resume("3", "6", change, "resume-1");
        controller.revoke("3", "6", change, "revoke-1");

        var assignment = ArgumentCaptor.forClass(RoleAssignmentDTO.class);
        verify(facade).assign(assignment.capture());
        assertEquals(acr, assignment.getValue().authenticationStrength());
        var changes = ArgumentCaptor.forClass(RoleAssignmentChangeDTO.class);
        verify(facade, times(3)).change(changes.capture());
        changes.getAllValues().forEach(command -> assertEquals(acr, command.authenticationStrength()));
    }
}
