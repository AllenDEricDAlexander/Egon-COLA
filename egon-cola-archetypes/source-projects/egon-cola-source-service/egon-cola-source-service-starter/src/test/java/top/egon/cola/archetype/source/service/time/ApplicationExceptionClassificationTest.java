package top.egon.cola.archetype.source.service.time;

import org.junit.jupiter.api.Test;
import top.egon.cola.archetype.source.service.common.exception.ApplicationException;
import top.egon.cola.archetype.source.service.common.enums.ApplicationErrorCode;
import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.CommonException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Classification of the existing public application vocabulary, without starting a runtime. */
class ApplicationExceptionClassificationTest {
    @Test
    void expectedRejectionsAndOperationalFailuresKeepSeparateAlertSemantics() {
        assertFalse(new ApplicationException(ApplicationErrorCode.VALIDATION_FAILED, "invalid").isAlertRequired());
        assertTrue(new ApplicationException(ApplicationErrorCode.INFRASTRUCTURE_FAILURE, "unavailable").isAlertRequired());
    }

    @Test
    void asyncConfigurationDelegatesOnceWhenComponentHandlerIsAvailable() throws Exception {
        var factory = new org.springframework.beans.factory.support.StaticListableBeanFactory();
        var seen = new java.util.concurrent.atomic.AtomicReference<Throwable>();
        factory.addBean("dtpAsyncUncaughtExceptionHandler",
                (org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler) (error, method, args) -> seen.set(error));
        var provider = factory.getBeanProvider(org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler.class);
        var configuration = new top.egon.cola.archetype.source.service.starter.config.async.AsyncConfiguration(provider);
        var failure = new IllegalStateException("expected");
        configuration.getAsyncUncaughtExceptionHandler().handleUncaughtException(failure,
                getClass().getDeclaredMethod("asyncConfigurationDelegatesOnceWhenComponentHandlerIsAvailable"), "private-argument");
        org.junit.jupiter.api.Assertions.assertSame(failure, seen.get());
        org.junit.jupiter.api.Assertions.assertNotNull(new top.egon.cola.archetype.source.service.starter.config.async.AsyncConfiguration().getAsyncUncaughtExceptionHandler());
    }

    @Test
    void asyncInjectionConstructorRetainsExplicitQualifiersAndCompatibility() {
        var constructors = top.egon.cola.archetype.source.service.starter.config.async.AsyncConfiguration.class.getConstructors();
        var injected = java.util.Arrays.stream(constructors)
                .filter(item -> item.isAnnotationPresent(org.springframework.beans.factory.annotation.Autowired.class))
                .toList();
        org.junit.jupiter.api.Assertions.assertEquals(1, injected.size());
        org.junit.jupiter.api.Assertions.assertEquals(1, injected.getFirst().getParameterCount());
        for (var parameter : injected.getFirst().getParameters()) {
            org.junit.jupiter.api.Assertions.assertNotNull(
                    parameter.getAnnotation(org.springframework.beans.factory.annotation.Qualifier.class));
        }
    }
}
