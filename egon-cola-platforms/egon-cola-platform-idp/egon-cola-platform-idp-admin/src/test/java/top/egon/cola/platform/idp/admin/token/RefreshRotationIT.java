package top.egon.cola.platform.idp.admin.token;

import org.junit.jupiter.api.Test;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import top.egon.cola.platform.idp.admin.token.infrastructure.RedisRefreshTokenStore;
import top.egon.cola.platform.idp.core.port.RefreshTokenStore;
import top.egon.cola.platform.idp.core.token.RefreshFamily;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RefreshRotationIT {

    private static final Instant NOW = Instant.parse("2026-08-02T00:00:00Z");
    private static final String DIGEST_A =
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    private static final String DIGEST_B =
            "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB";

    @Test
    void rotationUsesLuaWithDigestStateAndMapsAtomicOutcomes() {
        RedissonClient redisson = mock(RedissonClient.class);
        RScript script = mock(RScript.class);
        when(redisson.getScript(StringCodec.INSTANCE)).thenReturn(script);
        List<String> actions = new ArrayList<>();
        when(script.eval(
                any(RScript.Mode.class),
                anyString(),
                any(RScript.ReturnType.class),
                anyList(),
                any(Object[].class)
        )).thenAnswer(invocation -> {
            String action = invocation.getArgument(4, String.class);
            actions.add(action);
            return "CREATE".equals(action) ? "CREATED" : "ROTATED";
        });
        RedisRefreshTokenStore store = new RedisRefreshTokenStore(
                redisson,
                "identity:v1:"
        );
        RefreshFamily family = family();

        store.create(family);
        RefreshTokenStore.RotationResult result = store.rotate(
                rotation(DIGEST_A, DIGEST_B)
        );

        assertEquals(List.of("CREATE", "ROTATE"), actions);
        assertEquals(RefreshTokenStore.RotationOutcome.ROTATED,
                result.outcome());
        assertFalse(store.scriptSource().contains("rawRefreshToken"));
        assertTrue(store.scriptSource().contains("redis.call"));
    }

    @Test
    void replayRevokedAndMissingResultsRemainDistinct() {
        assertEquals(
                RefreshTokenStore.RotationOutcome.REPLAY,
                storeReturning("REPLAY").rotate(
                        rotation(DIGEST_A, DIGEST_B)
                ).outcome()
        );
        assertEquals(
                RefreshTokenStore.RotationOutcome.REVOKED,
                storeReturning("REVOKED").rotate(
                        rotation(DIGEST_A, DIGEST_B)
                ).outcome()
        );
        assertEquals(
                RefreshTokenStore.RotationOutcome.MISSING,
                storeReturning("MISSING").rotate(
                        rotation(DIGEST_A, DIGEST_B)
                ).outcome()
        );
    }

    private RedisRefreshTokenStore storeReturning(String outcome) {
        RedissonClient redisson = mock(RedissonClient.class);
        RScript script = mock(RScript.class);
        when(redisson.getScript(StringCodec.INSTANCE)).thenReturn(script);
        when(script.eval(
                any(RScript.Mode.class),
                anyString(),
                any(RScript.ReturnType.class),
                anyList(),
                any(Object[].class)
        )).thenReturn(outcome);
        return new RedisRefreshTokenStore(redisson, "identity:v1:");
    }

    private RefreshFamily family() {
        return new RefreshFamily(
                "family-1",
                "alice-sub",
                "tenant-a",
                "family-1",
                "gateway-admin-web",
                7L,
                0L,
                DIGEST_A,
                RefreshFamily.Status.ACTIVE,
                NOW,
                NOW,
                NOW.plusSeconds(3_600)
        );
    }

    private RefreshTokenStore.RotationCommand rotation(
            String currentDigest,
            String nextDigest
    ) {
        return new RefreshTokenStore.RotationCommand(
                "family-1",
                currentDigest,
                nextDigest,
                1L,
                "alice-sub",
                7L,
                NOW.plusSeconds(3_600),
                NOW.plusSeconds(1)
        );
    }
}
