package top.egon.cola.archetype.source.lightopen.adapter.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.archetype.source.lightopen.adapter.filter.RequestContextFilter;
import top.egon.cola.archetype.source.lightopen.adapter.filter.RequestContextHolder;
import top.egon.cola.archetype.source.lightopen.adapter.filter.TraceIdFilter;
import top.egon.cola.archetype.source.lightopen.common.exception.UserUseCaseException;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HttpAdapterContractTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ProbeController())
            .setControllerAdvice(new GlobalExceptionHandler(), new ResponseWrapperHandler(objectMapper))
            .addFilters(new TraceIdFilter(), new RequestContextFilter(objectMapper)).build();

    @Test
    void binds_tenant_and_returns_common_result_with_the_same_trace() throws Exception {
        mvc.perform(get("/probe").header("X-Tenant-Id", "42").header("X-Operator-Id", "operator-1")
                        .header("X-Request-Id", "request-1").header("X-Trace-Id", "trace-1"))
                .andExpect(status().isOk()).andExpect(header().string("X-Trace-Id", "trace-1"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(10000))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.traceId").value("trace-1"))
                .andExpect(jsonPath("$.timestamp").isNumber())
                .andExpect(jsonPath("$.data.tenantId").value(42))
                .andExpect(jsonPath("$.data.userId").value("operator-1"))
                .andExpect(jsonPath("$.data.requestId").value("request-1"));
        assertThat(RequestContextHolder.get()).isEmpty();
        assertThat(MDC.get("traceId")).isNull();
        assertThat(MDC.get("tenantId")).isNull();
    }

    @Test
    void business_failure_retains_status_and_is_not_wrapped_as_success() throws Exception {
        mvc.perform(get("/probe/failure").header("X-Trace-Id", "trace-failure"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(600000))
                .andExpect(jsonPath("$.status").value("USER_EXISTS"))
                .andExpect(jsonPath("$.message").value("User exists"))
                .andExpect(jsonPath("$.traceId").value("trace-failure"));
        assertThat(MDC.get("traceId")).isNull();
        assertThat(MDC.get("userId")).isNull();
    }

    @Test
    void invalid_tenant_failure_has_trace_and_stops_before_controller() throws Exception {
        mvc.perform(get("/probe").header("X-Tenant-Id", "invalid").header("X-Trace-Id", "trace-invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.traceId").value("trace-invalid"));
        assertThat(MDC.get("traceId")).isNull();
        assertThat(MDC.get("tenantId")).isNull();
    }

    @RestController
    static class ProbeController {
        @GetMapping("/probe")
        Map<String, Object> context() {
            return Map.of("tenantId", EgonColaTenantIdProvider.currentTenantId(),
                    "userId", MDC.get("userId"), "requestId", MDC.get("requestId"));
        }

        @GetMapping("/probe/failure")
        Object failure() {
            throw new UserUseCaseException("USER_EXISTS", "User exists");
        }
    }
}
