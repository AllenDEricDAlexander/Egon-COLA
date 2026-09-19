package top.egon.cola.component.common.cache.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.redisson.codec.JsonJacksonCodec;
import top.egon.cola.component.common.cache.event.EgonColaCacheChangedEvent;
import top.egon.cola.component.common.cache.event.EgonColaCacheChangedOperation;
import top.egon.cola.component.common.cache.model.EgonColaCacheNullValueBO;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EgonColaCacheCodecsTest {

    private static final Instant NOW = Instant.parse("2026-09-18T00:00:00Z");

    private final ObjectMapper valueMapper = EgonColaCacheCodecs.valueMapper();
    private final ObjectMapper eventMapper = EgonColaCacheCodecs.eventMapper();

    private EgonColaCacheChangedEvent envelope() {
        return new EgonColaCacheChangedEvent(EgonColaCacheChangedEvent.SCHEMA_VERSION, "evt-1", "node-1",
                NOW, "UserBO", EgonColaCacheChangedOperation.EVICT, List.of("41:7", "41:8"));
    }

    @Test
    void valueCodecRoundTripsEnvelopeWithInstant() throws Exception {
        EgonColaCacheChangedEvent event = envelope();

        String json = valueMapper.writeValueAsString(event);
        EgonColaCacheChangedEvent decoded = valueMapper.readValue(json, EgonColaCacheChangedEvent.class);

        assertThat(decoded).isEqualTo(event);
        assertThat(decoded.occurredAt()).isEqualTo(NOW);
        assertThat(json).contains("2026-09-18T00:00:00Z");
    }

    @Test
    void eventMapperRoundTripsEnvelopeWithoutTypeWrappers() throws Exception {
        String json = eventMapper.writeValueAsString(envelope());

        assertThat(json).doesNotContain("@class");
        assertThat(eventMapper.readValue(json, EgonColaCacheChangedEvent.class)).isEqualTo(envelope());
    }

    @Test
    void unknownEventFieldsAreIgnoredForForwardCompatibility() throws Exception {
        String futureFieldJson = "{\"schemaVersion\":1,\"eventId\":\"evt-1\",\"originNodeId\":\"node-1\","
                + "\"occurredAt\":\"2026-09-18T00:00:00Z\",\"cacheName\":\"UserBO\",\"operation\":\"EVICT\","
                + "\"keys\":[\"41:7\"],\"futureField\":\"whatever\"}";

        EgonColaCacheChangedEvent decoded =
                eventMapper.readValue(futureFieldJson, EgonColaCacheChangedEvent.class);

        assertThat(decoded.keys()).containsExactly("41:7");
    }

    @Test
    void nullSentinelRoundTripsThroughRestrictedTyping() throws Exception {
        String json = valueMapper.writeValueAsString(EgonColaCacheNullValueBO.INSTANCE);

        Object decoded = valueMapper.readValue(json, Object.class);

        assertThat(decoded).isInstanceOf(EgonColaCacheNullValueBO.class);
        assertThat(decoded).isEqualTo(EgonColaCacheNullValueBO.INSTANCE);
    }

    @Test
    void linkedHashMapRoundTripsThroughRestrictedTyping() throws Exception {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", 7L);
        value.put("name", "tom");
        value.put("nested", new LinkedHashMap<>(Map.of("k", "v")));

        String json = valueMapper.writeValueAsString(value);
        Object decoded = valueMapper.readValue(json, Object.class);

        assertThat(decoded).isEqualTo(value);
        assertThat(decoded).isInstanceOf(LinkedHashMap.class);
    }

    @Test
    void valueCodecRejectsEvilTypeId() {
        // 与 mapValueCodec 的 WRAPPER_ARRAY 默认类型同形，确保走到 PTV 白名单裁决而非 token 提前失败
        String evil = "[\"javax.script.ScriptEngineManager\",{}]";

        assertThatThrownBy(() -> valueMapper.readValue(evil, Object.class))
                .isInstanceOf(IOException.class);
    }

    @Test
    void valueCodecRejectsRuntimeSubTypeTypeId() {
        String evil = "[\"java.lang.Runtime\",{}]";

        assertThatThrownBy(() -> valueMapper.readValue(evil, Object.class))
                .isInstanceOf(IOException.class);
    }

    @Test
    void mapValueCodecIsBuiltOnRestrictedMapper() throws Exception {
        JsonJacksonCodec codec = EgonColaCacheCodecs.mapValueCodec();

        assertThat(codec.getObjectMapper().getPolymorphicTypeValidator()).isNotNull();
        assertThatThrownBy(() -> codec.getObjectMapper()
                .readValue("[\"javax.script.ScriptEngineManager\",{}]", Object.class))
                .isInstanceOf(IOException.class);
    }
}
