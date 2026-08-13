package top.egon.cola.platform.rbac3.admin.auth;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.auth.service.JwtTokenService;
import top.egon.cola.platform.rbac3.admin.auth.service.RefreshFacade;
import top.egon.cola.platform.rbac3.admin.session.service.RefreshTokenService;
import top.egon.cola.platform.rbac3.contract.auth.Rbac3TokenClaims;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.IssuedAccessTokenVO;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.RefreshAuditVO;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.RefreshStateVO;
import top.egon.cola.platform.rbac3.admin.session.domain.vo.RotationResultVO;
import top.egon.cola.platform.rbac3.admin.session.domain.enums.RefreshTokenOutcomeEnum;

class RefreshRuntimePublicationTest {

    private static final Instant NOW = Instant.parse("2026-08-01T06:00:00Z");

    @Test
    void rotatesProjectsAndIssuesWithinOneTransactionBoundary() {
        RefreshTokenService refreshTokens = mock(RefreshTokenService.class);
        JwtTokenService accessTokens = mock(JwtTokenService.class);
        when(refreshTokens.rotate("opaque", NOW)).thenReturn(
                new RotationResultVO(
                        RefreshTokenOutcomeEnum.ROTATED, "next", "family"));
        when(accessTokens.issue(org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.eq(NOW)))
                .thenReturn(new IssuedAccessTokenVO(
                        "access", NOW.plusSeconds(300), claims()));
        RefreshStateVO state = new RefreshStateVO(
                "10", "20", "30", 4, 8, 6,
                NOW.plusSeconds(7200), false, null);
        List<String> order = new ArrayList<>();
        List<RefreshAuditVO> audits = new ArrayList<>();
        RefreshFacade facade = new RefreshFacade(
                refreshTokens,
                familyId -> {
                    order.add("state");
                    return state;
                },
                accessTokens,
                work -> {
                    order.add("transaction-begin");
                    var result = work.get();
                    order.add("transaction-end");
                    return result;
                },
                (current, generatedAt) -> order.add("runtime"),
                audits::add);

        var result = facade.refresh("opaque", NOW);

        assertThat(result.sessionVersion()).isEqualTo(8);
        assertThat(result.refreshToken()).isEqualTo("next");
        assertThat(order).containsExactly(
                "transaction-begin", "state", "runtime", "transaction-end");
        assertThat(audits).singleElement().satisfies(audit -> {
            assertThat(audit.tenantId()).isEqualTo("10");
            assertThat(audit.sessionId()).isEqualTo("30");
            assertThat(audit.sessionVersion()).isEqualTo(8);
        });
    }

    @Test
    void commitsReplayCompromiseBeforeReturningTheStableRejection() {
        RefreshTokenService refreshTokens = mock(RefreshTokenService.class);
        when(refreshTokens.rotate("replayed", NOW)).thenReturn(
                new RotationResultVO(
                        RefreshTokenOutcomeEnum.REPLAY_DETECTED, null, "family"));
        List<String> order = new ArrayList<>();
        RefreshFacade facade = new RefreshFacade(
                refreshTokens,
                ignored -> {
                    throw new AssertionError("state must not be loaded");
                },
                mock(JwtTokenService.class),
                work -> {
                    order.add("transaction-begin");
                    var result = work.get();
                    order.add("transaction-commit");
                    return result;
                },
                (state, generatedAt) -> {
                    throw new AssertionError("runtime is synchronized by replay repository");
                },
                audit -> {
                    throw new AssertionError("replay must not be audited as refresh success");
                });

        assertThatThrownBy(() -> facade.refresh("replayed", NOW))
                .hasMessageContaining("REFRESH_TOKEN_REUSED");
        assertThat(order).containsExactly("transaction-begin", "transaction-commit");
    }

    private Rbac3TokenClaims claims() {
        return new Rbac3TokenClaims(
                "issuer", List.of("audience"), "20", "10", "30",
                4, 8, 6, "jti", NOW, NOW, NOW.plusSeconds(300), "kid");
    }
}
