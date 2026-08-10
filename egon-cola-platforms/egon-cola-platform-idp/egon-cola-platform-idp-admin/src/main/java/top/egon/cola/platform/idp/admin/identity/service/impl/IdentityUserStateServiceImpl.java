package top.egon.cola.platform.idp.admin.identity.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.egon.cola.platform.idp.admin.identity.repo.IdentityUserDirectory;
import top.egon.cola.platform.idp.admin.identity.service.IdentityStateProjection;
import top.egon.cola.platform.idp.admin.identity.service.IdentityUserStateService;
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
public class IdentityUserStateServiceImpl implements IdentityUserStateService {

    private final IdentityUserDirectory directory;
    private final IdentityStateProjection projection;
    private final Clock clock;

    @Autowired
    public IdentityUserStateServiceImpl(
            IdentityUserDirectory directory,
            IdentityStateProjection projection
    ) {
        this(directory, projection, Clock.systemUTC());
    }

    IdentityUserStateServiceImpl(
            IdentityUserDirectory directory,
            IdentityStateProjection projection,
            Clock clock
    ) {
        this.directory = Objects.requireNonNull(directory, "directory");
        this.projection = Objects.requireNonNull(projection, "projection");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
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
}
