package top.egon.cola.component.gateway.admin.domain;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class GatewayReleaseStateMachine {

    private static final Map<GatewayReleaseStatus, Set<GatewayReleaseStatus>>
            TRANSITIONS = Map.of(
            GatewayReleaseStatus.CREATED,
            Set.of(GatewayReleaseStatus.VALIDATING),
            GatewayReleaseStatus.VALIDATING,
            Set.of(
                    GatewayReleaseStatus.READY,
                    GatewayReleaseStatus.FAILED
            ),
            GatewayReleaseStatus.READY,
            Set.of(GatewayReleaseStatus.PUBLISHING),
            GatewayReleaseStatus.PUBLISHING,
            Set.of(
                    GatewayReleaseStatus.SUCCESS,
                    GatewayReleaseStatus.FAILED,
                    GatewayReleaseStatus.TIMEOUT,
                    GatewayReleaseStatus.UNKNOWN
            ),
            GatewayReleaseStatus.SUCCESS,
            Set.of(GatewayReleaseStatus.SUPERSEDED),
            GatewayReleaseStatus.FAILED,
            Set.of(GatewayReleaseStatus.PUBLISHING),
            GatewayReleaseStatus.TIMEOUT,
            Set.of(GatewayReleaseStatus.PUBLISHING),
            GatewayReleaseStatus.UNKNOWN,
            Set.of(GatewayReleaseStatus.PUBLISHING),
            GatewayReleaseStatus.SUPERSEDED,
            Set.of()
    );

    private GatewayReleaseStatus status;

    public GatewayReleaseStateMachine(GatewayReleaseStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public GatewayReleaseStatus status() {
        return status;
    }

    public void transitionTo(GatewayReleaseStatus target) {
        Objects.requireNonNull(target, "target");
        if (!TRANSITIONS.getOrDefault(status, Set.of()).contains(target)) {
            throw new IllegalStateException(
                    "illegal release transition " + status + " -> " + target
            );
        }
        status = target;
    }
}
