package top.egon.cola.component.common.trace.autoconfigure;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

class MyLogInterceptorTest {

    private final Logger logger =
            (Logger) getLogger(MyLogInterceptor.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private Level originalLevel;

    @BeforeEach
    void setUpAppender() {
        originalLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDownAppender() {
        logger.detachAppender(appender);
        appender.stop();
        logger.setLevel(originalLevel);
    }

    @Test
    void logsEveryHandlerWithoutAnAnnotation() throws Exception {
        MyLogInterceptor interceptor = new MyLogInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/orders/1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Method method = TestController.class.getMethod("orders");
        HandlerMethod handler = new HandlerMethod(new TestController(), method);

        assertThat(interceptor.preHandle(request, response, handler))
                .isTrue();
        interceptor.postHandle(request, response, handler, null);
        interceptor.afterCompletion(request, response, handler, null);

        assertThat(appender.list).singleElement()
                .extracting(ILoggingEvent::getLevel)
                .isEqualTo(Level.INFO);
        assertThat(appender.list).singleElement()
                .extracting(ILoggingEvent::getFormattedMessage)
                .asString()
                .contains(
                        "http_access",
                        "method=GET",
                        "uri=/orders/1",
                        "handler=" + TestController.class.getName() + ".orders",
                        "status=200",
                        "cost_ms=");
    }

    @Test
    void logsWhenHandlerCompletesWithAnException() {
        MyLogInterceptor interceptor = new MyLogInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();

        interceptor.preHandle(request, response, new Object());
        interceptor.afterCompletion(
                request,
                response,
                new Object(),
                new IllegalStateException("failed")
        );

        assertThat(appender.list).singleElement()
                .extracting(ILoggingEvent::getFormattedMessage)
                .asString()
                .contains(
                        "method=POST",
                        "uri=/orders",
                        "handler=java.lang.Object"
                );
    }

    static final class TestController {

        public void orders() {
        }
    }
}
