package top.egon.cola.component.dtp.registry.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @ClassName: DtpConfigChangeMessageTest
 * @description: 动态线程池配置变更消息链路载体测试
 * @author: 有罗敷的马同学
 * @datetime: 2026Year-08Month-07Day
 * @Version: 1.0
 */
class DtpConfigChangeMessageTest {

    @Test
    void shouldCarryW3cTraceFields() {
        DtpConfigChangeMessage message = new DtpConfigChangeMessage();

        message.setTraceparent("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");
        message.setTracestate("egon=sampled");
        message.setRequestId("request-001");

        assertEquals(
                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01",
                message.getTraceparent()
        );
        assertEquals("egon=sampled", message.getTracestate());
        assertEquals("request-001", message.getRequestId());
    }
}
