package top.egon.cola.component.gateway.engine.discovery;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.core.provider.ProviderHealthState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActiveHealthTrackerTest {

    @Test
    void changesStateOnlyAfterConfiguredFailureAndSuccessThresholds() {
        ActiveHealthTracker tracker = new ActiveHealthTracker(2, 2);

        assertEquals(
                ProviderHealthState.UNKNOWN,
                tracker.snapshot("provider:lease").state()
        );
        assertTrue(tracker.eligible("provider:lease"));

        tracker.record("provider:lease", false);
        assertEquals(
                ProviderHealthState.UNKNOWN,
                tracker.snapshot("provider:lease").state()
        );
        tracker.record("provider:lease", false);
        assertEquals(
                ProviderHealthState.UNHEALTHY,
                tracker.snapshot("provider:lease").state()
        );

        tracker.record("provider:lease", true);
        assertEquals(
                ProviderHealthState.UNHEALTHY,
                tracker.snapshot("provider:lease").state()
        );
        tracker.record("provider:lease", true);
        assertEquals(
                ProviderHealthState.HEALTHY,
                tracker.snapshot("provider:lease").state()
        );
    }
}
