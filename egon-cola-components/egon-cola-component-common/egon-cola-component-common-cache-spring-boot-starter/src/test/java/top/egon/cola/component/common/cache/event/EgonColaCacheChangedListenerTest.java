package top.egon.cola.component.common.cache.event;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.listener.MessageListener;
import org.redisson.api.RTopic;
import org.slf4j.LoggerFactory;
import top.egon.cola.component.common.cache.autoconfigure.EgonColaCacheProperties;
import top.egon.cola.component.common.cache.codec.EgonColaCacheCodecs;
import top.egon.cola.component.common.cache.core.EgonColaTwoLevelCacheManager;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static top.egon.cola.component.common.cache.event.EgonColaCacheChangedOperation.EVICT;
import static top.egon.cola.component.common.cache.event.EgonColaCacheChangedOperation.PREFIX_EVICT;
import static top.egon.cola.component.common.cache.event.EgonColaCacheChangedOperation.PUT;

class EgonColaCacheChangedListenerTest {

    private static final String LOCAL_NODE = "node-a";
    private static final ObjectMapper EVENT_MAPPER = EgonColaCacheCodecs.eventMapper();

    private EgonColaTwoLevelCacheManager manager;
    private RTopic topic;
    private EgonColaCacheChangedListener listener;
    private ListAppender<ILoggingEvent> appender;
    private final AtomicReference<MessageListener<String>> captured = new AtomicReference<>();

    private static String eventJson(String originNodeId, String cacheName,
                                    EgonColaCacheChangedOperation operation, List<String> keys)
            throws JsonProcessingException {
        return EVENT_MAPPER.writeValueAsString(new EgonColaCacheChangedEvent(
                EgonColaCacheChangedEvent.SCHEMA_VERSION, "evt-1", originNodeId,
                Instant.parse("2026-09-18T00:00:00Z"), cacheName, operation, keys));
    }

    private void deliver(String body) {
        captured.get().onMessage("egon:cola:cache:event", body);
    }

    @BeforeEach
    void startListener() {
        manager = mock(EgonColaTwoLevelCacheManager.class);
        topic = mock(RTopic.class);
        when(manager.topic()).thenReturn(topic);
        when(manager.originNodeId()).thenReturn(LOCAL_NODE);
        when(topic.addListener(eq(String.class), any())).thenAnswer(invocation -> {
            captured.set(invocation.getArgument(1));
            return 7;
        });
        listener = new EgonColaCacheChangedListener(manager, new EgonColaCacheProperties());
        listener.start();

        appender = new ListAppender<>();
        appender.start();
        ((Logger) LoggerFactory.getLogger(EgonColaCacheChangedListener.class)).addAppender(appender);
    }

    @AfterEach
    void stopListener() {
        ((Logger) LoggerFactory.getLogger(EgonColaCacheChangedListener.class)).detachAppender(appender);
        listener.stop();
    }

    @Test
    void evictOnlyDropsRemoteL1() throws Exception {
        deliver(eventJson("node-b", "UserBO", EVICT, List.of("41:7")));

        verify(manager, times(1)).applyRemotePut("UserBO", List.of("41:7"));
        verify(manager, never()).applyLocalEviction(any(), any());
        verify(manager, never()).applyRemotePrefixEviction(any(), any());
    }

    @Test
    void prefixEvictDispatchesPerGlobKey() throws Exception {
        deliver(eventJson("node-b", "UserBO", PREFIX_EVICT, List.of("41:*")));

        verify(manager, times(1)).applyRemotePrefixEviction("UserBO", "41:*");
        verify(manager, never()).applyRemotePut(any(), any());
    }

    @Test
    void putOnlyDropsRemoteL1() throws Exception {
        deliver(eventJson("node-b", "UserBO", PUT, List.of("41:7")));

        verify(manager, times(1)).applyRemotePut("UserBO", List.of("41:7"));
        verify(manager, never()).applyLocalEviction(any(), any());
        verify(manager, never()).applyRemotePrefixEviction(any(), any());
    }

    @Test
    void selfEchoIsSkipped() throws Exception {
        deliver(eventJson(LOCAL_NODE, "UserBO", EVICT, List.of("41:7")));

        verify(manager, never()).applyLocalEviction(any(), any());
        verify(manager, never()).applyRemotePut(any(), any());
        verify(manager, never()).applyRemotePrefixEviction(any(), any());
    }

    @Test
    void multiRegionMessagesRouteByCacheName() throws Exception {
        deliver(eventJson("node-b", "UserBO", EVICT, List.of("41:7")));
        deliver(eventJson("node-b", "OrderBO", PUT, List.of("41:8")));

        verify(manager, times(1)).applyRemotePut("UserBO", List.of("41:7"));
        verify(manager, times(1)).applyRemotePut("OrderBO", List.of("41:8"));
    }

    @Test
    void badPayloadsAreLoggedAndDropped() {
        List<String> bodies = List.of(
                "not-json",
                "{\"schemaVersion\":1,\"eventId\":\"e\",\"originNodeId\":\"node-b\","
                        + "\"occurredAt\":\"2026-09-18T00:00:00Z\",\"cacheName\":\"UserBO\","
                        + "\"operation\":\"DROP\",\"keys\":[\"41:7\"]}",
                "{\"schemaVersion\":2,\"eventId\":\"e\",\"originNodeId\":\"node-b\","
                        + "\"occurredAt\":\"2026-09-18T00:00:00Z\",\"cacheName\":\"UserBO\","
                        + "\"operation\":\"EVICT\",\"keys\":[\"41:7\"]}");

        for (String body : bodies) {
            assertThatCode(() -> deliver(body)).doesNotThrowAnyException();
        }

        verify(manager, never()).applyLocalEviction(any(), any());
        verify(manager, never()).applyRemotePut(any(), any());
        verify(manager, never()).applyRemotePrefixEviction(any(), any());
        assertThat(appender.list)
                .filteredOn(event -> event.getLevel() == ch.qos.logback.classic.Level.ERROR)
                .hasSize(bodies.size())
                .allSatisfy(event -> assertThat(event.getFormattedMessage())
                        .contains("CACHE_EVENT_DESERIALIZE_FAILED"));
    }

    @Test
    void dispatchFailuresNeverEscapeRedissonThread() throws Exception {
        doThrow(new RuntimeException("boom")).when(manager).applyRemotePut(eq("UserBO"), any());

        assertThatCode(() -> deliver(eventJson("node-b", "UserBO", EVICT, List.of("41:7"))))
                .doesNotThrowAnyException();
        assertThat(appender.list).anySatisfy(event ->
                assertThat(event.getFormattedMessage()).contains("CACHE_EVENT_DESERIALIZE_FAILED"));
    }

    @Test
    void startIsIdempotentAndStopRemovesExactlyOneSubscription() {
        listener.start();

        verify(topic, times(1)).addListener(eq(String.class), any());
        listener.stop();
        listener.stop();
        verify(topic, times(1)).removeListener(7);
    }
}
