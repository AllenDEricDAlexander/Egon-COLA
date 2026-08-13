package top.egon.cola.platform.rbac3.admin.interfaces.http;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import top.egon.cola.platform.rbac3.contract.error.Rbac3ErrorCode;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import top.egon.cola.platform.rbac3.admin.shared.controller.Rbac3ApiExceptionHandler;

class Rbac3ApiExceptionHandlerTest {

    @Test
    void returnsStableSafeErrorEnvelopeWithRequestAndTraceIds() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "request-1");
        request.addHeader("X-Trace-Id", "trace-1");

        var response = new Rbac3ApiExceptionHandler().handleRuleViolation(
                new Rbac3RuleViolation("APP_ROLE_ACTIVATION_MUTEX_VIOLATION"),
                request);

        assertEquals(409, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(Rbac3ErrorCode.APP_ROLE_ACTIVATION_MUTEX_VIOLATION,
                response.getBody().error().code());
        assertEquals("request-1", response.getBody().meta().requestId());
        assertEquals("trace-1", response.getBody().meta().traceId());
    }
}
