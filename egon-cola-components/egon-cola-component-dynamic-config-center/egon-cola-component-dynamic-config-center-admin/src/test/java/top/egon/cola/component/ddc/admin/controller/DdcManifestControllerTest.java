package top.egon.cola.component.ddc.admin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DdcManifestController.class)
class DdcManifestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void manifestReturnsDynamicConfigCenterMetadata() throws Exception {
        mockMvc.perform(get("/api/v1/ddc/manifest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.component").value("dynamic-config-center"))
                .andExpect(jsonPath("$.data.baseApiPath").value("/api/v1/ddc"));
    }
}
