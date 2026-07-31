package top.egon.cola.component.ddc.admin.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DdcAdminWebResourceTest {

    @Test
    void noLongerShipsTheBundledAdminWeb() {
        assertThat(DdcAdminWebResourceTest.class.getClassLoader()
                .getResource("static/ddc-admin/index.html"))
                .as("the admin jar must not bundle the extracted ddc-admin web")
                .isNull();
    }
}
