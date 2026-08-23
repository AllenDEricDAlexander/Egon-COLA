package ${package}.adapter.handler;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseWrapperHandlerTest {
    @Test
    void does_not_double_wrap_api_response() {
        ResponseWrapperHandler handler = new ResponseWrapperHandler();
        ApiResponse<String> response = ApiResponse.success("ok");
        assertThat(handler.beforeBodyWrite(response, null, null, null, null, null)).isSameAs(response);
    }
}
