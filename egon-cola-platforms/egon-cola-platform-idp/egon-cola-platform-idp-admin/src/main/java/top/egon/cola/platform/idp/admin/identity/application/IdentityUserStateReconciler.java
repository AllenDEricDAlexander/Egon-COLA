package top.egon.cola.platform.idp.admin.identity.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.egon.cola.platform.idp.contract.IdentityUserState;
import top.egon.cola.platform.idp.core.identity.IdentityUser;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Restores the Redis identity projection from PostgreSQL before readiness.
 */
@Service
public class IdentityUserStateReconciler {

    private final IdentityUserAdminService.UserDirectory directory;
    private final StateProjection projection;
    private final Clock clock;

    @Autowired
    public IdentityUserStateReconciler(
            IdentityUserAdminService.UserDirectory directory,
            StateProjection projection
    ) {
        this(directory, projection, Clock.systemUTC());
    }

    IdentityUserStateReconciler(
            IdentityUserAdminService.UserDirectory directory,
            StateProjection projection,
            Clock clock
    ) {
        this.directory = Objects.requireNonNull(directory, "directory");
        this.projection = Objects.requireNonNull(projection, "projection");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public int reconcile() {
        List<IdentityUser> users = directory.list();
        Instant now = clock.instant();
        for (IdentityUser user : users) {
            projection.project(new IdentityUserState(
                    user.id(),
                    IdentityUserState.Status.valueOf(user.status().name()),
                    user.tokenVersion(),
                    now
            ));
        }
        return users.size();
    }

    @FunctionalInterface
    public interface StateProjection {
        void project(IdentityUserState state);
    }
}
