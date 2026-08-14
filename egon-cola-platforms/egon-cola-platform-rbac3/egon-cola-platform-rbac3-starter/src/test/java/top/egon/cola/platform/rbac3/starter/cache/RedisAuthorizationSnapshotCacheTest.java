package top.egon.cola.platform.rbac3.starter.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RKeys;
import org.redisson.api.RScript;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class RedisAuthorizationSnapshotCacheTest {

    @Test
    void putAtomicallyWritesExactDataAndInvalidationIndexes() {
        RedissonClient redisson = mock(RedissonClient.class);
        RScript script = mock(RScript.class);
        when(redisson.getScript(any(Codec.class))).thenReturn(script);
        when(script.eval(
                eq(RScript.Mode.READ_WRITE), anyString(),
                eq(RScript.ReturnType.INTEGER), anyList(), any(Object[].class)))
                .thenReturn(1L);
        RedisAuthorizationSnapshotCache store = store(redisson);
        var key = new AuthorizationSnapshotCache.Key(
                "finance", "tenant-a", "alice-sub");

        store.put(key, AuthorizationSnapshotCacheTest.snapshot("alice-sub"),
                Duration.ofMinutes(5));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Object>> keys = ArgumentCaptor.forClass(List.class);
        verify(script).eval(
                eq(RScript.Mode.READ_WRITE), anyString(),
                eq(RScript.ReturnType.INTEGER), keys.capture(), any(Object[].class));
        assertThat(keys.getValue()).containsExactly(
                "rbac3:authorization:finance:tenant-a:alice-sub",
                "rbac3:authorization:finance:tenant-a:user:alice-sub",
                "rbac3:authorization:finance:tenant-a:tenant");
    }

    @Test
    void userInvalidationDeletesOnlyKeysFromTheExactUserIndex() {
        RedissonClient redisson = mock(RedissonClient.class);
        RSet<String> userIndex = set();
        RSet<String> tenantIndex = set();
        RKeys redisKeys = mock(RKeys.class);
        String dataKey = "rbac3:authorization:finance:tenant-a:alice-sub";
        String userIndexKey =
                "rbac3:authorization:finance:tenant-a:user:alice-sub";
        String tenantIndexKey =
                "rbac3:authorization:finance:tenant-a:tenant";
        when(redisson.<String>getSet(eq(userIndexKey), any(Codec.class)))
                .thenReturn(userIndex);
        when(redisson.<String>getSet(eq(tenantIndexKey), any(Codec.class)))
                .thenReturn(tenantIndex);
        when(userIndex.readAll()).thenReturn(Set.of(dataKey));
        when(redisson.getKeys()).thenReturn(redisKeys);

        store(redisson).invalidateUser("finance", "tenant-a", "alice-sub");

        verify(redisKeys).delete(aryEq(new String[]{dataKey}));
        verify(tenantIndex).removeAll(List.of(dataKey));
        verify(userIndex).delete();
        verifyNoMoreInteractions(redisKeys);
    }

    private RedisAuthorizationSnapshotCache store(RedissonClient redisson) {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());
        return new RedisAuthorizationSnapshotCache(
                redisson, objectMapper, Duration.ZERO);
    }

    @SuppressWarnings("unchecked")
    private RSet<String> set() {
        return mock(RSet.class);
    }
}
