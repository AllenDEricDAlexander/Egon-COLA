package top.egon.cola.component.ddc.admin.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.ddc.admin.model.entity.DdcAppEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcBizEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcEnvEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcNamespaceEntity;
import top.egon.cola.component.ddc.admin.repository.DdcAppRepository;
import top.egon.cola.component.ddc.admin.repository.DdcBizRepository;
import top.egon.cola.component.ddc.admin.repository.DdcEnvRepository;
import top.egon.cola.component.ddc.admin.repository.DdcNamespaceRepository;
import top.egon.cola.component.ddc.common.DdcErrorStatus;

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
        bizRepository = mock(DdcBizRepository.class);
        appRepository = mock(DdcAppRepository.class);
        envRepository = mock(DdcEnvRepository.class);
        namespaceRepository = mock(DdcNamespaceRepository.class);
        gate = new DdcScopeGate(bizRepository, appRepository, envRepository, namespaceRepository);
    }

    private DdcBizEntity biz(boolean enabled) {
        DdcBizEntity entity = new DdcBizEntity();
        entity.setEnabled(enabled);
        return entity;
    }

    private DdcAppEntity app(boolean enabled) {
        DdcAppEntity entity = new DdcAppEntity();
        entity.setEnabled(enabled);
        return entity;
    }

    private DdcEnvEntity env(boolean enabled) {
        DdcEnvEntity entity = new DdcEnvEntity();
        entity.setEnabled(enabled);
        return entity;
    }

    private DdcNamespaceEntity ns(boolean enabled) {
        DdcNamespaceEntity entity = new DdcNamespaceEntity();
        entity.setEnabled(enabled);
        return entity;
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
