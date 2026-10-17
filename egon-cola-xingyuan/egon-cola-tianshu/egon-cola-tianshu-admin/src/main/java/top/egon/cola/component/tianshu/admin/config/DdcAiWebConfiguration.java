package top.egon.cola.component.tianshu.admin.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiCreateCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiDraftCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiOptimizationPolicyCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiOptimizationTriggerCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiPromotionCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiPublishCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiRollbackCommand;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

@Configuration(proxyBeanMethods = false)
public class DdcAiWebConfiguration {

    public static final int MAX_REQUEST_BODY_BYTES = 8 * 1024 * 1024;

    @Bean(name = "ddcAiWebConfigurer")
    public WebMvcConfigurer ddcAiWebConfigurer(Jackson2ObjectMapperBuilder objectMapperBuilder) {
        ObjectMapper strictObjectMapper = objectMapperBuilder.build();
        strictObjectMapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        strictObjectMapper.enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES);
        strictObjectMapper.disable(MapperFeature.ALLOW_COERCION_OF_SCALARS);
        strictObjectMapper.getFactory().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        strictObjectMapper.coercionConfigFor(LogicalType.Textual)
                .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);
        MappingJackson2HttpMessageConverter converter = new DdcAiStrictJsonMessageConverter(strictObjectMapper);
        return new WebMvcConfigurer() {
            @Override
            public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
                converters.add(0, converter);
            }
        };
    }

    public static final class RequestBodyTooLargeException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public RequestBodyTooLargeException() {
            super("AI request body exceeds the configured limit");
        }
    }

    private static final class DdcAiStrictJsonMessageConverter extends MappingJackson2HttpMessageConverter {

        private static final Set<Class<?>> COMMAND_TYPES = Set.of(
                DdcAiCreateCommand.class,
                DdcAiDraftCommand.class,
                DdcAiPublishCommand.class,
                DdcAiRollbackCommand.class,
                DdcAiPromotionCommand.class,
                DdcAiOptimizationPolicyCommand.class,
                DdcAiOptimizationTriggerCommand.class
        );

        private final ObjectMapper strictObjectMapper;

        private DdcAiStrictJsonMessageConverter(ObjectMapper strictObjectMapper) {
            super(strictObjectMapper);
            this.strictObjectMapper = strictObjectMapper;
        }

        @Override
        protected boolean supports(Class<?> clazz) {
            return COMMAND_TYPES.contains(clazz);
        }

        @Override
        public boolean canRead(Class<?> clazz, MediaType mediaType) {
            return COMMAND_TYPES.contains(clazz) && super.canRead(clazz, mediaType);
        }

        @Override
        public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {
            return type instanceof Class<?> clazz && COMMAND_TYPES.contains(clazz)
                    && super.canRead(type, contextClass, mediaType);
        }

        @Override
        public boolean canWrite(Class<?> clazz, MediaType mediaType) {
            return false;
        }

        @Override
        public boolean canWrite(Type type, Class<?> contextClass, MediaType mediaType) {
            return false;
        }

        @Override
        protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage) throws IOException {
            return readStrict(clazz, inputMessage);
        }

        @Override
        public Object read(Type type, Class<?> contextClass, HttpInputMessage inputMessage) throws IOException {
            return readStrict(type, inputMessage);
        }

        private Object readStrict(Type type, HttpInputMessage inputMessage) throws IOException {
            long contentLength = inputMessage.getHeaders().getContentLength();
            if (contentLength > MAX_REQUEST_BODY_BYTES) throw new RequestBodyTooLargeException();
            byte[] body = inputMessage.getBody().readNBytes(MAX_REQUEST_BODY_BYTES + 1);
            if (body.length > MAX_REQUEST_BODY_BYTES) throw new RequestBodyTooLargeException();
            try {
                return strictObjectMapper.readValue(body, strictObjectMapper.getTypeFactory().constructType(type));
            } catch (JsonProcessingException failure) {
                throw new HttpMessageNotReadableException("Invalid AI request JSON", failure, inputMessage);
            }
        }
    }
}
