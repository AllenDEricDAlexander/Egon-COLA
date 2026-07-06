package top.egon.cola.component.dtp.admin.manifest;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class DtpManifestControllerTest {

    @Test
    void manifestDescribesDynamicThreadPoolComponent() {
        DtpManifestController controller = new DtpManifestController();

        DtpComponentManifest manifest = controller.manifest().getData();

        assertEquals("dynamic-thread-pool", manifest.getComponent());
        assertEquals("Dynamic Thread Pool", manifest.getName());
        assertTrue(manifest.isEnabled());
        assertEquals("/api/v1/dtp", manifest.getBaseApi());
        assertEquals("dynamic-thread-pool", manifest.getFrontend().getModule());
        assertEquals("/components/dynamic-thread-pool", manifest.getFrontend().getRouteBase());
        assertFalse(manifest.getFrontend().getMenus().isEmpty());
        assertFalse(manifest.getPermissions().isEmpty());
    }

    @Test
    void manifestEndpointExposesComponentManifest() throws Exception {
        MockMvc mockMvc = standaloneSetup(new DtpManifestController()).build();

        mockMvc.perform(get("/api/v1/dtp/manifest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data.component").value("dynamic-thread-pool"))
                .andExpect(jsonPath("$.data.baseApi").value("/api/v1/dtp"))
                .andExpect(jsonPath("$.data.frontend.routeBase").value("/components/dynamic-thread-pool"))
                .andExpect(jsonPath("$.data.frontend.menus", not(empty())));
    }
}
