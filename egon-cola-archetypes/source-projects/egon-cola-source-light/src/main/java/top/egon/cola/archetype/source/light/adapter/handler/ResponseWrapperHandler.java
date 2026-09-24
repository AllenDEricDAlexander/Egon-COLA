package top.egon.cola.archetype.source.light.adapter.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import top.egon.cola.component.common.core.pojo.ResultRecord;

@ControllerAdvice(basePackages = "top.egon.cola.archetype.source.light.adapter")
@RequiredArgsConstructor
public class ResponseWrapperHandler implements ResponseBodyAdvice<Object> {
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(
            MethodParameter returnType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        String packageName = returnType.getContainingClass().getPackageName();
        return packageName.startsWith("top.egon.cola.archetype.source.light.adapter.")
                && !packageName.contains(".graphql")
                && !ResultRecord.class.isAssignableFrom(returnType.getParameterType())
                && !PageResultRecord.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {
        if (body instanceof ResultRecord<?> || body instanceof PageResultRecord<?>) {
            return body;
        }
        ResultRecord<Object> result = ResultRecord.success(body);
        if (selectedConverterType != null && StringHttpMessageConverter.class.isAssignableFrom(selectedConverterType)) {
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            try {
                return objectMapper.writeValueAsString(result);
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Failed to serialize response", exception);
            }
        }
        return result;
    }
}
