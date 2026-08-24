package top.egon.cola.component.common.trace.autoconfigure;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.lang.reflect.Method;

/**
 * Logs every Spring MVC request after its handler has completed.
 */
public class MyLogInterceptor implements HandlerInterceptor {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MyLogInterceptor.class);

    private static final String START_TIME_ATTRIBUTE =
            MyLogInterceptor.class.getName() + ".startTime";

    private static final String LOGGED_ATTRIBUTE =
            MyLogInterceptor.class.getName() + ".logged";

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        request.setAttribute(START_TIME_ATTRIBUTE, System.nanoTime());
        request.removeAttribute(LOGGED_ATTRIBUTE);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           ModelAndView modelAndView) {
        logRequest(request, response, handler);
        request.setAttribute(LOGGED_ATTRIBUTE, Boolean.TRUE);
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception exception) {
        if (!Boolean.TRUE.equals(request.getAttribute(LOGGED_ATTRIBUTE))) {
            logRequest(request, response, handler);
        }
        request.removeAttribute(START_TIME_ATTRIBUTE);
        request.removeAttribute(LOGGED_ATTRIBUTE);
    }

    private void logRequest(HttpServletRequest request,
                            HttpServletResponse response,
                            Object handler) {
        Object startedAt = request.getAttribute(START_TIME_ATTRIBUTE);
        if (!(startedAt instanceof Long startTime)) {
            return;
        }
        long costMs = (System.nanoTime() - startTime) / 1_000_000L;
        LOGGER.info(
                "http_access method={} uri={} handler={} status={} cost_ms={}",
                request.getMethod(),
                request.getRequestURI(),
                handlerName(handler),
                response.getStatus(),
                costMs
        );
    }

    private String handlerName(Object handler) {
        if (handler instanceof HandlerMethod handlerMethod) {
            Method method = handlerMethod.getMethod();
            return method.getDeclaringClass().getName() + "." + method.getName();
        }
        return handler == null ? "unknown" : handler.getClass().getName();
    }
}
