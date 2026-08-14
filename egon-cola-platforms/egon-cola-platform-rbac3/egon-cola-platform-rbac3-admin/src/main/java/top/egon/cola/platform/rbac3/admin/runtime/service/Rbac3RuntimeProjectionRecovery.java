package top.egon.cola.platform.rbac3.admin.runtime.service;

import org.springframework.stereotype.Component;
import top.egon.cola.platform.rbac3.admin.runtime.domain.dto.MutationWorkDTO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.EventEnvelopeVO;

/**
 * Stateless runtime recovery boundary.
 *
 * Authorization publications are rebuilt from the user-scoped activation flow;
 * no identity rows are queried or recreated here. Event replay remains a no-op
 * until a user-scoped event carries enough facts to rebuild a publication.
 */
@Component
public final class Rbac3RuntimeProjectionRecovery implements
        RuntimeProjectionExecutor,
        RuntimeSnapshotRebuildService {

    @Override
    public void project(MutationWorkDTO mutation) {
        // The activation facade publishes the immutable user snapshot in-band.
    }

    @Override
    public void rebuild(EventEnvelopeVO event) {
        // User-scoped event replay is deliberately fail-closed until its payload is complete.
    }
}
