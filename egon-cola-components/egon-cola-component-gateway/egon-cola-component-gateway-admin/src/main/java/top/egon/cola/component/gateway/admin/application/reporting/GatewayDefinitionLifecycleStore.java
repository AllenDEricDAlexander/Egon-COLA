package top.egon.cola.component.gateway.admin.application.reporting;

import java.time.Instant;
import java.util.Set;

public interface GatewayDefinitionLifecycleStore {

    ReconcileResult reconcile(
            Set<String> activeDefinitionSetIds,
            Instant now);

    record ReconcileResult(
            int activatedDefinitionSets,
            int retiredDefinitionSets,
            int activatedOperations,
            int offlinedOperations
    ) {

        public boolean changed() {
            return activatedDefinitionSets > 0
                    || retiredDefinitionSets > 0
                    || activatedOperations > 0
                    || offlinedOperations > 0;
        }
    }
}
