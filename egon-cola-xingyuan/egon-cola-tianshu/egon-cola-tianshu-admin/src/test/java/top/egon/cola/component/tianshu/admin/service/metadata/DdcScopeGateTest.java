package top.egon.cola.component.tianshu.admin.service.metadata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAppBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcBizBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcEnvBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcNamespaceBO;
import top.egon.cola.component.tianshu.admin.repository.DdcAppRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcBizRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcEnvRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcNamespaceRepository;
import top.egon.cola.component.tianshu.error.DdcErrorStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcScopeGateTest {

    private DdcBizRepository bizRepository;

    private DdcAppRepository appRepository;

    private DdcEnvRepository envRepository;

    private DdcNamespaceRepository namespaceRepository;

    private DdcScopeGate gate;

    @BeforeEach
    void setUp() {
        MDC.put("tenantId", "101");
        bizRepository = mock(DdcBizRepository.class);
        appRepository = mock(DdcAppRepository.class);
        envRepository = mock(DdcEnvRepository.class);
        namespaceRepository = mock(DdcNamespaceRepository.class);
        gate = new DdcScopeGate(bizRepository, appRepository, envRepository, namespaceRepository);
    }

    @AfterEach
    void clearTenantContext() {
        MDC.clear();
    }

    private DdcBizBO biz(boolean enabled) {
        return DdcBizBO.builder().enabled(enabled).build();
    }

    private DdcAppBO app(boolean enabled) {
        return DdcAppBO.builder().enabled(enabled).build();
    }

    private DdcEnvBO env(boolean enabled) {
        return DdcEnvBO.builder().enabled(enabled).build();
    }

    private DdcNamespaceBO ns(boolean enabled) {
        return DdcNamespaceBO.builder().enabled(enabled).build();
    }

    @Test
    void passesWhenAllScopeEntitiesAreEnabled() {
        when(bizRepository.findByBizCode("pay-biz")).thenReturn(Optional.of(biz(true)));
        when(appRepository.findByBizCodeAndAppCode("pay-biz", "orders-app"))
                .thenReturn(Optional.of(app(true)));
        when(envRepository.findByEnvCode("dev")).thenReturn(Optional.of(env(true)));
        when(namespaceRepository.findByBizCodeAndNamespaceCode("pay-biz", "default"))
                .thenReturn(Optional.of(ns(true)));

        assertThatCode(() -> gate.assertEnabled("pay-biz", "orders-app", "dev", "default"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsWhenAppIsDisabled() {
        when(bizRepository.findByBizCode("pay-biz")).thenReturn(Optional.of(biz(true)));
        when(appRepository.findByBizCodeAndAppCode("pay-biz", "orders-app"))
                .thenReturn(Optional.of(app(false)));
        when(envRepository.findByEnvCode("dev")).thenReturn(Optional.of(env(true)));
        when(namespaceRepository.findByBizCodeAndNamespaceCode("pay-biz", "default"))
                .thenReturn(Optional.of(ns(true)));

        assertThatThrownBy(() -> gate.assertEnabled("pay-biz", "orders-app", "dev", "default"))
                .isInstanceOfSatisfying(CommonException.class,
                        e -> assertThatThrownByStatus(e));
    }

    @Test
    void rejectsWhenEntityIsMissing() {
        when(bizRepository.findByBizCode("pay-biz")).thenReturn(Optional.of(biz(true)));
        when(appRepository.findByBizCodeAndAppCode("pay-biz", "missing-app"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> gate.assertEnabled("pay-biz", "missing-app", "dev", "default"))
                .isInstanceOfSatisfying(CommonException.class,
                        e -> assertThatThrownByStatus(e));
    }

    @Test
    void cachesForFiveSecondsWithoutRequerying() {
        when(bizRepository.findByBizCode("pay-biz")).thenReturn(Optional.of(biz(true)));
        when(appRepository.findByBizCodeAndAppCode("pay-biz", "orders-app"))
                .thenReturn(Optional.of(app(true)));
        when(envRepository.findByEnvCode("dev")).thenReturn(Optional.of(env(true)));
        when(namespaceRepository.findByBizCodeAndNamespaceCode("pay-biz", "default"))
                .thenReturn(Optional.of(ns(true)));

        gate.assertEnabled("pay-biz", "orders-app", "dev", "default");
        gate.assertEnabled("pay-biz", "orders-app", "dev", "default");

        verify(bizRepository, times(1)).findByBizCode("pay-biz");
        verify(appRepository, times(1))
                .findByBizCodeAndAppCode("pay-biz", "orders-app");
    }

    @Test
    void sameScopeCacheEntryIsIndependentAcrossTenants() {
        when(bizRepository.findByBizCode("pay-biz"))
                .thenReturn(Optional.of(biz(true)), Optional.of(biz(false)));
        when(appRepository.findByBizCodeAndAppCode("pay-biz", "orders-app"))
                .thenReturn(Optional.of(app(true)));
        when(envRepository.findByEnvCode("dev")).thenReturn(Optional.of(env(true)));
        when(namespaceRepository.findByBizCodeAndNamespaceCode("pay-biz", "default"))
                .thenReturn(Optional.of(ns(true)));

        gate.assertEnabled("pay-biz", "orders-app", "dev", "default");
        MDC.put("tenantId", "202");

        assertThatThrownBy(() -> gate.assertEnabled(
                "pay-biz", "orders-app", "dev", "default"))
                .isInstanceOfSatisfying(CommonException.class,
                        exception -> assertThatThrownByStatus(exception));
        verify(bizRepository, times(2)).findByBizCode("pay-biz");
    }

    @Test
    void invalidateForcesRequery() {
        when(bizRepository.findByBizCode("pay-biz")).thenReturn(Optional.of(biz(true)));
        when(appRepository.findByBizCodeAndAppCode("pay-biz", "orders-app"))
                .thenReturn(Optional.of(app(true)));
        when(envRepository.findByEnvCode("dev")).thenReturn(Optional.of(env(true)));
        when(namespaceRepository.findByBizCodeAndNamespaceCode("pay-biz", "default"))
                .thenReturn(Optional.of(ns(true)));

        gate.assertEnabled("pay-biz", "orders-app", "dev", "default");
        gate.invalidate("app:pay-biz:orders-app");
        gate.assertEnabled("pay-biz", "orders-app", "dev", "default");

        verify(appRepository, times(2))
                .findByBizCodeAndAppCode("pay-biz", "orders-app");
    }

    private void assertThatThrownByStatus(CommonException e) {
        org.assertj.core.api.Assertions.assertThat(e.getCode())
                .isEqualTo(DdcErrorStatus.SCOPE_DISABLED.getCode());
    }
}
