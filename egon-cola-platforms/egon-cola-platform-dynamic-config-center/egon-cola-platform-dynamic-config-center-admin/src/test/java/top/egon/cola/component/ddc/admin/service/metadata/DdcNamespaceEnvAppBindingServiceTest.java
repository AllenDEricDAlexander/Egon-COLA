package top.egon.cola.component.ddc.admin.service.metadata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.ddc.admin.model.dto.DdcNamespaceEnvAppBindingRequest;
import top.egon.cola.component.ddc.admin.model.entity.DdcAppEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcNamespaceEntity;
import top.egon.cola.component.ddc.admin.model.vo.DdcNamespaceEnvAppBindingVO;
import top.egon.cola.component.ddc.admin.repository.DdcAppRepository;
import top.egon.cola.component.ddc.admin.repository.DdcEnvRepository;
import top.egon.cola.component.ddc.admin.repository.DdcNamespaceEnvAppBindingRepository;
import top.egon.cola.component.ddc.admin.repository.DdcNamespaceRepository;
import top.egon.cola.component.ddc.error.DdcErrorStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcNamespaceEnvAppBindingServiceTest {

    private DdcNamespaceEnvAppBindingRepository bindingRepository;

    private DdcNamespaceRepository namespaceRepository;

    private DdcAppRepository appRepository;

    private DdcEnvRepository envRepository;

    private DdcNamespaceEnvAppBindingService service;

    @BeforeEach
    void setUp() {
        bindingRepository = mock(DdcNamespaceEnvAppBindingRepository.class);
        namespaceRepository = mock(DdcNamespaceRepository.class);
        appRepository = mock(DdcAppRepository.class);
        envRepository = mock(DdcEnvRepository.class);
        service = new DdcNamespaceEnvAppBindingService(
                bindingRepository,
                namespaceRepository,
                appRepository,
                envRepository
        );
    }

    @Test
    void bindsOnePhysicalAppToAnotherNamespaceWithoutCopyingTheApp() {
        DdcNamespaceEntity namespace = namespace("ns-ops", "ops");
        DdcAppEntity app = app();
        when(namespaceRepository.findByBizCodeAndNamespaceCode("infra", "ops"))
                .thenReturn(Optional.of(namespace));
        when(appRepository.findByBizCodeAndAppCode("infra", "ge"))
                .thenReturn(Optional.of(app));
        when(envRepository.existsByEnvCode("prod")).thenReturn(true);
        when(bindingRepository.save(any())).thenAnswer(invocation ->
                invocation.getArgument(0));

        var value = service.create(new DdcNamespaceEnvAppBindingRequest(
                "infra", "ops", "prod", "ge", true));

        assertThat(value.namespaceId()).isEqualTo("ns-ops");
        assertThat(value.appId()).isEqualTo("app-ge");
        assertThat(value.appCode()).isEqualTo("ge");
    }

    @Test
    void rejectsOnlyTheSameNamespaceEnvironmentAppTriple() {
        DdcNamespaceEntity namespace = namespace("ns-ops", "ops");
        DdcAppEntity app = app();
        when(namespaceRepository.findByBizCodeAndNamespaceCode("infra", "ops"))
                .thenReturn(Optional.of(namespace));
        when(appRepository.findByBizCodeAndAppCode("infra", "ge"))
                .thenReturn(Optional.of(app));
        when(envRepository.existsByEnvCode("prod")).thenReturn(true);
        when(bindingRepository.existsByNamespaceIdAndEnvCodeAndAppId(
                "ns-ops", "prod", "app-ge"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(
                new DdcNamespaceEnvAppBindingRequest(
                        "infra", "ops", "prod", "ge", true)))
                .isInstanceOfSatisfying(CommonException.class,
                        error -> assertThat(error.getCode()).isEqualTo(
                                DdcErrorStatus.NAMESPACE_BINDING_EXISTS.getCode()));
    }

    @Test
    void pagesWithOneJoinProjectionWithoutPerRowLookups() {
        DdcNamespaceEnvAppBindingVO row = new DdcNamespaceEnvAppBindingVO(
                "binding-1", "infra", "ns-ops", "ops", "prod",
                "app-ge", "ge", "Gateway Engine", true);
        when(bindingRepository.search(
                eq("infra"), eq("ops"), eq("prod"), eq("ge"), any()))
                .thenReturn(new PageImpl<>(
                        List.of(row), PageRequest.of(0, 10), 1));

        var page = service.page(
                " infra ", " ops ", " prod ", " ge ",
                new PageQuery(1, 10));

        assertThat(page.getContent()).containsExactly(row);
        verify(bindingRepository).search(
                eq("infra"), eq("ops"), eq("prod"), eq("ge"), any());
        verify(namespaceRepository, never()).findById(anyString());
        verify(appRepository, never()).findById(anyString());
    }

    private DdcNamespaceEntity namespace(String id, String code) {
        DdcNamespaceEntity namespace = new DdcNamespaceEntity();
        namespace.setId(id);
        namespace.setBizCode("infra");
        namespace.setNamespaceCode(code);
        namespace.setNamespace(code);
        namespace.setEnabled(true);
        return namespace;
    }

    private DdcAppEntity app() {
        DdcAppEntity app = new DdcAppEntity();
        app.setId("app-ge");
        app.setBizCode("infra");
        app.setAppCode("ge");
        app.setAppName("Gateway Engine");
        app.setEnabled(true);
        return app;
    }
}
