package top.egon.cola.component.common.cache.codec;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.redisson.codec.JsonJacksonCodec;
import top.egon.cola.component.common.cache.model.EgonColaCacheNullValueBO;

/**
 * 受限多态编解码单点（主 Spec §13.3 冻结白名单）：L2 值 codec 与事件 JSON mapper
 * 均由此构造，白名单外 {@code @class} 在解析阶段即被拒绝、不实例化。
 */
public final class EgonColaCacheCodecs {

    private EgonColaCacheCodecs() {
    }

    /** RMapCache 值编解码器，承载宿主 PO 快照与空值哨兵。 */
    public static JsonJacksonCodec mapValueCodec() {
        return new RestrictedTypeCodec(valueMapper());
    }

    /** L2 值用 mapper：NON_FINAL 默认类型 + 受限 PTV + java.time ISO 文本。 */
    public static ObjectMapper valueMapper() {
        ObjectMapper mapper = new ObjectMapper();
        configureTimeAndUnknown(mapper);
        // 无字段 record 属 final 类型，NON_FINAL 默认类型不会为其写入类型标识，显式补齐哨兵的还原通道。
        mapper.addMixIn(EgonColaCacheNullValueBO.class, NullValueTypeMixin.class);
        mapper.activateDefaultTyping(restrictedValidator(), ObjectMapper.DefaultTyping.NON_FINAL);
        return mapper;
    }

    /** 事件 topic 载荷用 mapper：目标类型恒为信封本身，无需多态类型信息。 */
    public static ObjectMapper eventMapper() {
        ObjectMapper mapper = new ObjectMapper();
        configureTimeAndUnknown(mapper);
        return mapper;
    }

    private static void configureTimeAndUnknown(ObjectMapper mapper) {
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    private static PolymorphicTypeValidator restrictedValidator() {
        return BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("top.egon.cola.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.time.")
                .allowIfSubType("java.lang.")
                .build();
    }

    // final record 在 NON_FINAL 默认类型下不写类型标识；mixin 显式补齐，
    // include 必须与默认类型一致的 WRAPPER_ARRAY，否则编解码两向格式漂移。
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY)
    abstract static class NullValueTypeMixin {
    }

    /**
     * Redisson {@link JsonJacksonCodec} 的默认类型初始化会覆盖外部 mapper 的受限
     * PTV（LaissezFaire），覆写为空操作以保留 {@link #valueMapper()} 的白名单裁决。
     */
    private static final class RestrictedTypeCodec extends JsonJacksonCodec {

        RestrictedTypeCodec(ObjectMapper mapper) {
            super(mapper);
        }

        @Override
        protected void initTypeInclusion(ObjectMapper mapper) {
            // keep the restricted default typing configured by valueMapper()
        }
    }
}
