package top.egon.cola.component.yuheng.edge.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Component;
import top.egon.cola.component.yuheng.edge.service.impl.EdgeTokenCipher;

@Component("edgeSessionRedisSerializer")
@RequiredArgsConstructor
public class EdgeSessionRedisSerializer implements RedisSerializer<Object> {
    private static final String PURPOSE = "spring-session";
    private static final String CONTEXT = "egon:bff:session:v2";

    @Qualifier("edgeTokenCipher")
    private final EdgeTokenCipher cipher;
    private final JdkSerializationRedisSerializer delegate = new JdkSerializationRedisSerializer();

    @Override
    public byte[] serialize(Object value) throws SerializationException {
        if (value == null) {
            return null;
        }
        try {
            byte[] serialized = delegate.serialize(value);
            return cipher.encrypt(serialized, PURPOSE, CONTEXT);
        } catch (RuntimeException exception) {
            throw new SerializationException("Unable to encrypt Spring Session value", exception);
        }
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return delegate.deserialize(cipher.decrypt(bytes, PURPOSE, CONTEXT));
        } catch (RuntimeException exception) {
            throw new SerializationException("Unable to authenticate Spring Session value", exception);
        }
    }
}
