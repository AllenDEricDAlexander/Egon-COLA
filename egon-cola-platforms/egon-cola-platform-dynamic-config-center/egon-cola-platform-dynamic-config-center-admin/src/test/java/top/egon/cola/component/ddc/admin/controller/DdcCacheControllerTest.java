package top.egon.cola.component.ddc.admin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.ddc.admin.controller.config.DdcCacheController;
import top.egon.cola.component.ddc.admin.model.vo.DdcCacheCheckRow;
import top.egon.cola.component.ddc.admin.service.cache.DdcCacheService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DdcCacheController.class)
@AutoConfigureMockMvc(addFilters = false)
class DdcCacheControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DdcCacheService cacheService;

    @Test
    void pagesCacheChecks() throws Exception {
        DdcCacheCheckRow row = new DdcCacheCheckRow(
                "application.yml", "db", "redis", 2L, 2L, true
        );
        when(cacheService.page(
                eq("infra"), eq("prod"), eq("gateway"),
                any(PageQuery.class)
        )).thenReturn(new PageImpl<>(
                List.of(row), PageRequest.of(0, 1), 3
        ));

        mockMvc.perform(get("/api/v1/ddc/cache/check/page")
                        .param("bizCode", "infra")
                        .param("env", "prod")
                        .param("appCode", "gateway")
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].resourceName")
                        .value("application.yml"))
                .andExpect(jsonPath("$.page.total").value(3))
                .andExpect(jsonPath("$.page.pageNo").value(1))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
