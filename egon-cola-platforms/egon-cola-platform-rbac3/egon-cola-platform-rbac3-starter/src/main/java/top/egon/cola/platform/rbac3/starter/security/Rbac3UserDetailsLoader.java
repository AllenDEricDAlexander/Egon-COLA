package top.egon.cola.platform.rbac3.starter.security;

import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.starter.cache.SingleFlightSnapshotLoader;
import top.egon.cola.platform.rbac3.starter.client.Rbac3AuthorizationClient;

import java.util.Objects;

/**
 * Assembles a request-scoped {@link Rbac3UserDetails} from the existing authorization snapshot
 * cache. The cache stores authorization facts only; the current AT identity is always supplied
 * by the caller.
 */
public final class Rbac3UserDetailsLoader {

    private final SingleFlightSnapshotLoader snapshots;

    public Rbac3UserDetailsLoader(SingleFlightSnapshotLoader snapshots) {
        this.snapshots = Objects.requireNonNull(snapshots, "snapshots");
    }

    public Rbac3UserDetails load(IdentityPrincipal identity) {
        Objects.requireNonNull(identity, "identity");
        SystemAuthorizationSnapshot snapshot = snapshots.load(identity);
        if (snapshot == null
                || !identity.subject().equals(snapshot.identitySub())
                || !identity.tenantId().equals(snapshot.tenantId())) {
            throw new Rbac3AuthorizationClient.AuthorizationDeniedException(
                    "RBAC3_AUTHORIZATION_BINDING_MISMATCH");
        }
        return new Rbac3UserDetails(identity, snapshot);
    }
}
