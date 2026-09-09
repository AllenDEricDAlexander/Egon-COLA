package top.egon.cola.component.tianshu.admin.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DdcAdminWebResourceTest {

    @Test
    void noLongerShipsTheBundledAdminWeb() {
        assertThat(DdcAdminWebResourceTest.class.getClassLoader()
                .getResource("static/tianshu-admin/index.html"))
                .as("the admin jar must not bundle the extracted tianshu-admin web")
                .isNull();
    }
}
