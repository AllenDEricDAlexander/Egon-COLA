package top.egon.cola.platform.idp.admin.identity.application;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.contract.IdentityUserState;
import top.egon.cola.platform.idp.core.identity.IdentityUser;
import top.egon.cola.platform.idp.core.identity.IdentityUserStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityUserStateReconcilerTest {

    private static final Instant NOW =
            Instant.parse("2026-08-02T00:00:00Z");

    @Test
    void restoresEveryPersistentUserStateAtStartup() {
        List<IdentityUserState> projected = new ArrayList<>();
        IdentityUserStateReconciler reconciler =
                new IdentityUserStateReconciler(
                        () -> List.of(
                                user("alice", IdentityUserStatus.ACTIVE, 4L),
                                user("bob", IdentityUserStatus.DISABLED, 7L)
                        ),
                        projected::add,
                        Clock.fixed(NOW, ZoneOffset.UTC)
                );

        int count = reconciler.reconcile();

        assertThat(count).isEqualTo(2);
        assertThat(projected)
                .extracting(
                        IdentityUserState::subject,
                        IdentityUserState::status,
                        IdentityUserState::tokenVersion,
                        IdentityUserState::updatedAt
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                "alice",
                                IdentityUserState.Status.ACTIVE,
                                4L,
                                NOW
                        ),
                        org.assertj.core.groups.Tuple.tuple(
                                "bob",
                                IdentityUserState.Status.DISABLED,
                                7L,
                                NOW
                        )
                );
    }

    private IdentityUser user(
            String id,
            IdentityUserStatus status,
            long tokenVersion
    ) {
        return new IdentityUser(
                id,
                id,
                id,
                id,
                status,
                tokenVersion,
                0,
                null,
                null,
                0L
        );
    }
}
