package top.egon.cola.component.tianshu.admin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.BusinessException;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.tianshu.admin.controller.metadata.DdcAppController;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAppBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAppResultConverter;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAppResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.repository.DdcAppRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcBizRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcNamespaceEnvAppBindingRepository;
import top.egon.cola.component.tianshu.admin.service.metadata.DdcAppService;
import top.egon.cola.component.tianshu.admin.service.metadata.DdcNamespaceEnvAppBindingService;
import top.egon.cola.component.tianshu.admin.service.metadata.DdcScopeGate;
import top.egon.cola.component.tianshu.error.DdcErrorStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DdcAppController.class)
@AutoConfigureMockMvc(addFilters = false)
class DdcAppControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean(name = "ddcAppService") private DdcAppService appService;
    @MockBean(name = "ddcAppResultConverterImpl") private DdcAppResultConverter resultConverter;

    private DdcAppBO app(String code) {
        return DdcAppBO.builder().publicId(code).appCode(code).bizCode("pay-biz").appName(code).enabled(true).build();
    }

    private DdcAppResult result(String code) {
        return new DdcAppResult(code, code, "pay-biz", code, null, null, true, null, null);
    }

    @Test
    void listWithVisibilityScopeDelegatesToService() throws Exception {
        List<DdcAppBO> rows = List.of(app("orders"));
        when(appService.list("pay-biz", "ops", "prod", "orders")).thenReturn(rows);
        when(resultConverter.toTargetList(rows)).thenReturn(List.of(result("orders")));
        mockMvc.perform(get("/api/v1/tianshu/apps").param("bizCode", "pay-biz").param("namespaceCode", "ops")
                        .param("env", "prod").param("keyword", "orders"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("orders"))
                .andExpect(jsonPath("$.data[0].bizCode").value("pay-biz"));
    }

    @Test
    void listWithoutParamsReturnsAllApps() throws Exception {
        List<DdcAppBO> rows = List.of(app("orders"), app("billing"));
        when(appService.list(null, null, null, null)).thenReturn(rows);
        when(resultConverter.toTargetList(rows)).thenReturn(List.of(result("orders"), result("billing")));
        mockMvc.perform(get("/api/v1/tianshu/apps")).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void pagesAppsWithinMetadataScope() throws Exception {
        List<DdcAppBO> rows = List.of(app("orders"));
        when(appService.page(eq("pay-biz"), eq("ops"), eq("prod"), eq("orders"), any(PageQuery.class)))
                .thenReturn(DdcPageResult.<DdcAppBO>builder().records(rows).total(21).pageNo(2).pageSize(20).build());
        when(resultConverter.toTargetList(rows)).thenReturn(List.of(result("orders")));
        mockMvc.perform(get("/api/v1/tianshu/apps/page").param("bizCode", "pay-biz").param("namespaceCode", "ops")
                        .param("env", "prod").param("keyword", "orders").param("pageNo", "2").param("pageSize", "20"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.records[0].id").value("orders"))
                .andExpect(jsonPath("$.page.total").value(21)).andExpect(jsonPath("$.page.pageNo").value(2))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void deleteInUseReturnsFailureCode() throws Exception {
        doThrow(new CommonException(DdcErrorStatus.APP_IN_USE)).when(appService).delete("app-orders");
        mockMvc.perform(delete("/api/v1/tianshu/apps/app-orders")).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(DdcErrorStatus.APP_IN_USE.getCode()));
    }

    @Test
    void saveRejectsAnApplicationCodeAlreadyUsedInThisTenant() {
        DdcAppRepository appRepository = mock(DdcAppRepository.class);
        DdcBizRepository bizRepository = mock(DdcBizRepository.class);
        DdcNamespaceEnvAppBindingRepository bindingRepository = mock(DdcNamespaceEnvAppBindingRepository.class);
        DdcNamespaceEnvAppBindingService bindingService = mock(DdcNamespaceEnvAppBindingService.class);
        DdcScopeGate scopeGate = mock(DdcScopeGate.class);
        DdcAppService service = new DdcAppService(scopeGate, appRepository, bizRepository, bindingRepository, bindingService);
        when(bizRepository.existsByBizCode("pay-biz")).thenReturn(true);
        when(appRepository.existsByAppCode("shared-app")).thenReturn(true);
        assertThatThrownBy(() -> service.save(app("shared-app")))
                .isInstanceOfSatisfying(BusinessException.class, failure -> {
                    assertThat(failure.getCode()).isEqualTo(ResultCode.CONCURRENCY_ERROR.getCode());
                    assertThat(failure.getStatus()).isEqualTo(ResultCode.CONCURRENCY_ERROR.getStatus());
                    assertThat(failure.getMessage()).isEqualTo("app code already exists");
                });
        verify(appRepository, never()).insert(any(DdcAppBO.class));
    }
}
