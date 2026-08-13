package top.egon.cola.component.gateway.admin.shared.domain;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.admin.release.domain.GatewayReleaseStateMachine;
import top.egon.cola.component.gateway.admin.release.domain.enums.GatewayReleaseStatus;
import top.egon.cola.component.gateway.admin.routing.domain.GatewayDraftRevision;
import top.egon.cola.component.gateway.admin.shared.domain.exception.GatewayAdminRevisionConflictException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GatewayAdminDomainTest {

    @Test
    void releaseStateMachineRejectsIllegalTransitions() {
        GatewayReleaseStateMachine release =
                new GatewayReleaseStateMachine(GatewayReleaseStatus.CREATED);

        release.transitionTo(GatewayReleaseStatus.VALIDATING);
        release.transitionTo(GatewayReleaseStatus.READY);
        release.transitionTo(GatewayReleaseStatus.PUBLISHING);
        release.transitionTo(GatewayReleaseStatus.SUCCESS);

        assertEquals(GatewayReleaseStatus.SUCCESS, release.status());
        assertThrows(IllegalStateException.class, () ->
                release.transitionTo(GatewayReleaseStatus.FAILED)
        );
    }

    @Test
    void failedReleaseCanRetryButSuccessfulReleaseCanOnlyBeSuperseded() {
        GatewayReleaseStateMachine failed =
                new GatewayReleaseStateMachine(
                        GatewayReleaseStatus.FAILED
                );
        failed.transitionTo(GatewayReleaseStatus.PUBLISHING);
        assertEquals(GatewayReleaseStatus.PUBLISHING, failed.status());

        GatewayReleaseStateMachine success =
                new GatewayReleaseStateMachine(
                        GatewayReleaseStatus.SUCCESS
                );
        success.transitionTo(GatewayReleaseStatus.SUPERSEDED);
        assertEquals(GatewayReleaseStatus.SUPERSEDED, success.status());
    }

    @Test
    void draftRequiresExactExpectedRevision() {
        GatewayDraftRevision revision = new GatewayDraftRevision(3);

        assertEquals(4, revision.advance(3));
        assertThrows(GatewayAdminRevisionConflictException.class, () ->
                revision.advance(3)
        );
    }
}
