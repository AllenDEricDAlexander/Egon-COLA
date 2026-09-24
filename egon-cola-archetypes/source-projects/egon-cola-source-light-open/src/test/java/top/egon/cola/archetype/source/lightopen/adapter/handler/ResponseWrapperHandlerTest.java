package top.egon.cola.archetype.source.lightopen.adapter.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletResponse;
import top.egon.cola.component.common.core.pojo.PageResultRecord;
import top.egon.cola.component.common.core.pojo.ResultRecord;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseWrapperHandlerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ResponseWrapperHandler handler = new ResponseWrapperHandler(objectMapper);

    @Test
    void does_not_double_wrap_common_results() {
        ResultRecord<String> response = ResultRecord.success("ok");
        assertThat(handler.beforeBodyWrite(response, null, null, null, null, null)).isSameAs(response);
        PageResultRecord<String> page = PageResultRecord.success(List.of("ok"), 1, 1, 10);
        assertThat(handler.beforeBodyWrite(page, null, null, null, null, null)).isSameAs(page);
    }

    @Test
    void wraps_payload_and_null_with_common_metadata() {
        MDC.put("traceId", "trace-1");
        try {
            ResultRecord<?> result = (ResultRecord<?>) handler.beforeBodyWrite(
                    List.of("ok"), null, null, null, null, null);
            assertThat(result.success()).isTrue();
            assertThat(result.code()).isEqualTo(10000);
            assertThat(result.status()).isEqualTo("SUCCESS");
            assertThat(result.data()).isEqualTo(List.of("ok"));
            assertThat(result.traceId()).isEqualTo("trace-1");
            assertThat(result.timestamp()).isPositive();
            ResultRecord<?> empty = (ResultRecord<?>) handler.beforeBodyWrite(null, null, null, null, null, null);
            assertThat(empty.success()).isTrue();
            assertThat(empty.data()).isNull();
        } finally {
            MDC.remove("traceId");
        }
    }

    @Test
    void string_converter_receives_serialized_json() throws Exception {
        ServletServerHttpResponse response = new ServletServerHttpResponse(new MockHttpServletResponse());
        Object body = handler.beforeBodyWrite("ok", null, MediaType.TEXT_PLAIN,
                StringHttpMessageConverter.class, null, response);
        assertThat(body).isInstanceOf(String.class);
        assertThat(objectMapper.readTree((String) body).get("data").textValue()).isEqualTo("ok");
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    }
}
