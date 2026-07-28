package top.egon.cola.component.ddc.admin.web;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DdcAdminWebResourceTest {

    @Test
    void shipsServiceRegistryAndConfigManagementPage() throws IOException {
        String html = resource("static/ddc-admin/index.html");
        String script = resource("static/ddc-admin/app.js");
        String styles = resource("static/ddc-admin/styles.css");
        String uuid = resource("static/ddc-admin/uuid.mjs");
        String configFormat = resource("static/ddc-admin/config-format.mjs");

        assertThat(html)
                .contains("DDC Admin")
                .contains("服务注册")
                .contains("配置管理")
                .contains("app.js")
                .contains("styles.css");
        assertThat(script)
                .contains("/api/v1/ddc/registry/services")
                .contains("/api/v1/ddc/registry/instances")
                .contains("/api/v1/ddc/configs")
                .contains("sessionStorage");
        assertThat(styles).contains("--color-primary");
        assertThat(uuid).contains("export const uuidV7");
        assertThat(configFormat).isNotBlank();
    }

    private String resource(String path) throws IOException {
        try (InputStream input = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(path)) {
            assertThat(input)
                    .as("classpath resource %s", path)
                    .isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
