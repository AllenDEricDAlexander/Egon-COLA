package top.egon.cola.component.accessguard.store.redisson;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.redisson.client.codec.Codec;
import top.egon.cola.component.accessguard.store.GuardStoreContract;
import top.egon.cola.component.accessguard.store.PenaltyKey;
import top.egon.cola.component.accessguard.store.PenaltyState;
import top.egon.cola.component.accessguard.store.RateLimitDecision;
import top.egon.cola.component.accessguard.store.RateLimitRequest;
import top.egon.cola.component.accessguard.store.StoreOperationException;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedissonStoreContractTest {

    private static final String HASH = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @Nested
    class AllowListContract implements GuardStoreContract {

        @Override
        public ListStoreFixture fixture() {
            FakeListScripts scripts = new FakeListScripts();
            return RedissonStoreContractTest.fixture(
                    new RedissonAllowListStore(scripts.client(), keyFactory()), scripts);
        }
    }

    @Nested
    class DenyListContract implements GuardStoreContract {

        @Override
        public ListStoreFixture fixture() {
            FakeListScripts scripts = new FakeListScripts();
            return RedissonStoreContractTest.fixture(
                    new RedissonDenyListStore(scripts.client(), keyFactory()), scripts);
        }
    }

    @Test
    void backendWrapsRedisFailureInsteadOfReturningRateLimited() {
        RedissonClient client = mock(RedissonClient.class);
        RScript script = mock(RScript.class);
        when(client.getScript(any(Codec.class))).thenReturn(script);
        when(script.eval(any(), any(), any(), any(), any(Object[].class)))
                .thenThrow(new RedisException("down"));
        RedissonRateLimitBackend backend = new RedissonRateLimitBackend(client, keyFactory(), Duration.ofMinutes(10));

        assertThatThrownBy(() -> backend.acquire(new RateLimitRequest(
                "draw", "v1", HASH, 1, 1, Duration.ofSeconds(1), 1)))
                .isInstanceOf(StoreOperationException.class)
                .hasMessage("RATE_LIMIT_STORE_FAILED")
                .hasCauseInstanceOf(RedisException.class);
    }

    @Test
    void penaltyScriptReturnsTheAtomicThresholdState() {
        RedissonClient client = mock(RedissonClient.class);
        RScript script = mock(RScript.class);
        when(client.getScript(any(Codec.class))).thenReturn(script);
        when(script.eval(any(), anyString(), any(), anyList(), any(Object[].class)))
                .thenReturn(List.of(3L, 1L, 1_800_000L, 2_400_000L));
        RedissonPenaltyStore store = new RedissonPenaltyStore(client, keyFactory());

        PenaltyState state = store.recordViolation(
                new PenaltyKey("draw", "state-v1", HASH),
                3,
                Duration.ofMinutes(1),
                Duration.ofMinutes(10));

        org.mockito.ArgumentCaptor<String> lua = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(script).eval(
                eq(RScript.Mode.READ_WRITE),
                lua.capture(),
                eq(RScript.ReturnType.MULTI),
                anyList(),
                any(Object[].class));
        org.assertj.core.api.Assertions.assertThat(state.active()).isTrue();
        org.assertj.core.api.Assertions.assertThat(state.violations()).isEqualTo(3L);
        org.assertj.core.api.Assertions.assertThat(lua.getValue())
                .contains("redis.call('TIME')", "redis.call('HSET'", "redis.call('PEXPIRE'");
    }

    @Test
    void tokenBucketUsesRedisTimeAndReturnsRemainingAndRetryAfter() {
        RedissonClient client = mock(RedissonClient.class);
        RScript script = mock(RScript.class);
        when(client.getScript(any(Codec.class))).thenReturn(script);
        when(script.eval(any(), anyString(), any(), anyList(), any(Object[].class)))
                .thenReturn(List.of(0L, 2L, 500L));
        RedissonRateLimitBackend backend = new RedissonRateLimitBackend(client, keyFactory(), Duration.ofMinutes(10));

        RateLimitDecision decision = backend.acquire(new RateLimitRequest(
                "draw", "v1", HASH, 10, 2, Duration.ofSeconds(1), 3));

        org.mockito.ArgumentCaptor<String> lua = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(script).eval(
                eq(RScript.Mode.READ_WRITE),
                lua.capture(),
                eq(RScript.ReturnType.MULTI),
                anyList(),
                any(Object[].class));
        org.assertj.core.api.Assertions.assertThat(decision.allowed()).isFalse();
        org.assertj.core.api.Assertions.assertThat(decision.remainingTokens()).isEqualTo(2L);
        org.assertj.core.api.Assertions.assertThat(decision.retryAfter()).isEqualTo(Duration.ofMillis(500));
        org.assertj.core.api.Assertions.assertThat(lua.getValue())
                .contains("redis.call('TIME')", "redis.call('HSET'", "redis.call('PEXPIRE'");
    }

    private static GuardStoreContract.ListStoreFixture fixture(
            top.egon.cola.component.accessguard.store.AllowListStore store,
            FakeListScripts scripts
    ) {
        return new GuardStoreContract.ListStoreFixture() {
            @Override
            public boolean contains(String ruleId, String dataVersion, String keyHash) {
                return store.contains(ruleId, dataVersion, keyHash);
            }

            @Override
            public void add(String ruleId, String dataVersion, String keyHash, Duration ttl) {
                store.add(ruleId, dataVersion, keyHash, ttl);
            }

            @Override
            public void replace(String ruleId, String dataVersion, Set<String> keyHashes, Duration ttl) {
                store.replace(ruleId, dataVersion, keyHashes, ttl);
            }

            @Override
            public void advance(Duration duration) {
                scripts.advance(duration);
            }
        };
    }

    private static GuardStoreContract.ListStoreFixture fixture(
            top.egon.cola.component.accessguard.store.DenyListStore store,
            FakeListScripts scripts
    ) {
        return new GuardStoreContract.ListStoreFixture() {
            @Override
            public boolean contains(String ruleId, String dataVersion, String keyHash) {
                return store.contains(ruleId, dataVersion, keyHash);
            }

            @Override
            public void add(String ruleId, String dataVersion, String keyHash, Duration ttl) {
                store.add(ruleId, dataVersion, keyHash, ttl);
            }

            @Override
            public void replace(String ruleId, String dataVersion, Set<String> keyHashes, Duration ttl) {
                store.replace(ruleId, dataVersion, keyHashes, ttl);
            }

            @Override
            public void advance(Duration duration) {
                scripts.advance(duration);
            }
        };
    }

    private static AccessGuardRedisKeyFactory keyFactory() {
        return new AccessGuardRedisKeyFactory("egon:access-guard", "test");
    }

    private static final class FakeListScripts {

        private final Map<String, Map<String, Long>> values = new HashMap<>();
        private long nowMillis;

        private RedissonClient client() {
            RedissonClient client = mock(RedissonClient.class);
            RScript script = mock(RScript.class);
            when(client.getScript(any(Codec.class))).thenReturn(script);
            when(script.eval(any(), any(), any(), any(), any(Object[].class)))
                    .thenAnswer(invocation -> {
                        Object[] raw = invocation.getArguments();
                        return evaluate(
                                invocation.getArgument(1),
                                invocation.getArgument(3),
                                Arrays.copyOfRange(raw, 4, raw.length));
                    });
            return client;
        }

        private Object evaluate(String script, List<Object> keys, Object[] arguments) {
            String key = keys.getFirst().toString();
            Map<String, Long> entries = values.computeIfAbsent(key, ignored -> new HashMap<>());
            if (script.contains("access-guard:list:contains")) {
                String hash = arguments[0].toString();
                Long expiry = entries.get(hash);
                if (expiry == null || expiry > 0 && expiry <= nowMillis) {
                    entries.remove(hash);
                    return 0L;
                }
                return 1L;
            }
            if (script.contains("access-guard:list:add")) {
                entries.put(arguments[0].toString(), expiry(Long.parseLong(arguments[1].toString())));
                return 1L;
            }
            if (script.contains("access-guard:list:remove")) {
                entries.remove(arguments[0].toString());
                return 1L;
            }
            if (script.contains("access-guard:list:replace")) {
                entries.clear();
                long ttlMillis = Long.parseLong(arguments[0].toString());
                for (int index = 1; index < arguments.length; index++) {
                    entries.put(arguments[index].toString(), expiry(ttlMillis));
                }
                return 1L;
            }
            throw new AssertionError("Unexpected script: " + script);
        }

        private long expiry(long ttlMillis) {
            return ttlMillis == 0 ? 0 : nowMillis + ttlMillis;
        }

        private void advance(Duration duration) {
            nowMillis += duration.toMillis();
        }
    }
}
