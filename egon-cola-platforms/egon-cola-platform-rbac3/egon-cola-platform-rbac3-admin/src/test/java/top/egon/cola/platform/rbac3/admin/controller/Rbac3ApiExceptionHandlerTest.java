package top.egon.cola.platform.rbac3.admin.controller;

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
        assertEquals(409, response.getBody().code());
        assertEquals("APP_ROLE_ACTIVATION_MUTEX_VIOLATION",
                response.getBody().status());
        assertEquals("Request rejected by authorization policy",
                response.getBody().message());
    }
}
