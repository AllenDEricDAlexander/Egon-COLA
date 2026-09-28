package top.egon.cola.component.tianshu.admin.service.metadata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAppBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcNamespaceBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcNamespaceEnvAppBindingBO;
import top.egon.cola.component.tianshu.admin.model.dto.DdcNamespaceEnvAppBindingRequest;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcNamespaceEnvAppBindingResultConverterImpl;
import top.egon.cola.component.tianshu.admin.model.vo.DdcNamespaceEnvAppBindingVO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.repository.DdcAppRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcEnvRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcNamespaceEnvAppBindingRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcNamespaceRepository;
import top.egon.cola.component.tianshu.error.DdcErrorStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
        service = new DdcNamespaceEnvAppBindingService(bindingRepository, namespaceRepository, appRepository,
                envRepository, new DdcNamespaceEnvAppBindingResultConverterImpl());
    }

    @Test
    void bindsOnePhysicalAppToAnotherNamespaceWithoutCopyingTheApp() {
        DdcNamespaceBO namespace = namespace();
        DdcAppBO app = app();
        when(namespaceRepository.findByBizCodeAndNamespaceCode("infra", "ops")).thenReturn(Optional.of(namespace));
        when(appRepository.findByBizCodeAndAppCode("infra", "ge")).thenReturn(Optional.of(app));
        when(envRepository.existsByEnvCode("prod")).thenReturn(true);
        when(bindingRepository.insert(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DdcNamespaceEnvAppBindingVO value = service.create(new DdcNamespaceEnvAppBindingRequest(
                "infra", "ops", "prod", "ge", true));

        assertThat(value.namespaceId()).isEqualTo("ns-ops");
        assertThat(value.appId()).isEqualTo("app-ge");
        assertThat(value.appCode()).isEqualTo("ge");
    }

    @Test
    void rejectsOnlyTheSameNamespaceEnvironmentAppTriple() {
        when(namespaceRepository.findByBizCodeAndNamespaceCode("infra", "ops")).thenReturn(Optional.of(namespace()));
        when(appRepository.findByBizCodeAndAppCode("infra", "ge")).thenReturn(Optional.of(app()));
        when(envRepository.existsByEnvCode("prod")).thenReturn(true);
        when(bindingRepository.existsByNamespaceIdAndEnvCodeAndAppId(11L, "prod", 22L)).thenReturn(true);

        assertThatThrownBy(() -> service.create(new DdcNamespaceEnvAppBindingRequest(
                "infra", "ops", "prod", "ge", true)))
                .isInstanceOfSatisfying(CommonException.class,
                        error -> assertThat(error.getCode()).isEqualTo(DdcErrorStatus.NAMESPACE_BINDING_EXISTS.getCode()));
    }

    @Test
    void pagesWithOneJoinProjectionWithoutPerRowLookups() {
        DdcNamespaceEnvAppBindingBO row = DdcNamespaceEnvAppBindingBO.builder().publicId("binding-1")
                .bizCode("infra").namespacePublicId("ns-ops").namespaceCode("ops").envCode("prod")
                .appPublicId("app-ge").appCode("ge").appName("Gateway Engine").enabled(true).build();
        when(bindingRepository.search(eq("infra"), eq("ops"), eq("prod"), eq("ge"), any()))
                .thenReturn(DdcPageResult.<DdcNamespaceEnvAppBindingBO>builder().records(List.of(row))
                        .total(1).pageNo(1).pageSize(10).build());

        DdcPageResult<DdcNamespaceEnvAppBindingVO> page = service.page(
                " infra ", " ops ", " prod ", " ge ", new PageQuery(1, 10));

        assertThat(page.getRecords()).containsExactly(new DdcNamespaceEnvAppBindingVO(
                "binding-1", "infra", "ns-ops", "ops", "prod", "app-ge", "ge", "Gateway Engine", true));
        assertThat(page.getTotal()).isEqualTo(1);
        verify(bindingRepository).search(eq("infra"), eq("ops"), eq("prod"), eq("ge"), any());
        verifyNoInteractions(namespaceRepository, appRepository);
    }

    private DdcNamespaceBO namespace() {
        return DdcNamespaceBO.builder().id(11L).publicId("ns-ops").bizCode("infra").namespaceCode("ops")
                .namespace("ops").enabled(true).build();
    }
    private DdcAppBO app() {
        return DdcAppBO.builder().id(22L).publicId("app-ge").bizCode("infra").appCode("ge")
                .appName("Gateway Engine").enabled(true).build();
    }
}
