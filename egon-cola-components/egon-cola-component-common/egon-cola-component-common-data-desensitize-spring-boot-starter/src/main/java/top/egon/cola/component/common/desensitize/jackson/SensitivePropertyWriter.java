package top.egon.cola.component.common.desensitize.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import top.egon.cola.component.common.desensitize.annotation.SensitiveType;
import top.egon.cola.component.common.desensitize.strategy.SensitiveStrategyRegistry;

import java.io.IOException;

final class SensitivePropertyWriter extends BeanPropertyWriter {

    private final SensitiveType type;

    private final SensitiveStrategyRegistry strategyRegistry;

    SensitivePropertyWriter(BeanPropertyWriter source,
                            SensitiveType type,
                            SensitiveStrategyRegistry strategyRegistry) {
        super(source);
        this.type = type;
        this.strategyRegistry = strategyRegistry;
    }

    @Override
    public void serializeAsField(Object bean,
                                 JsonGenerator generator,
                                 SerializerProvider provider)
            throws Exception {
        super.serializeAsField(
                bean,
                new MaskingJsonGenerator(generator),
                provider
        );
    }

    private class MaskingJsonGenerator extends JsonGeneratorDelegate {

        MaskingJsonGenerator(JsonGenerator delegate) {
            super(delegate);
        }

        @Override
        public void writeString(String value) throws IOException {
            super.writeString(strategyRegistry.mask(type, value));
        }

        @Override
        public void writeString(char[] text, int offset, int length) throws IOException {
            writeString(new String(text, offset, length));
        }

        @Override
        public void writeString(SerializableString value) throws IOException {
            writeString(value.getValue());
        }
    }
}
