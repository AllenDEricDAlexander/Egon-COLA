package top.egon.cola.component.ddc.common;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.model.dto.DdcPublishMessage;

import static org.assertj.core.api.Assertions.assertThat;

class DdcChecksumTest {

    @Test
    void checksumIsStableForSameMessage() {
        DdcPublishMessage message = message("true");

        assertThat(DdcChecksum.sha256(message)).isEqualTo(DdcChecksum.sha256(message("true")));
    }

    @Test
    void checksumChangesWhenValueChanges() {
        assertThat(DdcChecksum.sha256(message("true"))).isNotEqualTo(DdcChecksum.sha256(message("false")));
    }

    private DdcPublishMessage message(String configValue) {
        DdcPublishMessage message = new DdcPublishMessage();
        message.setChangeId("c1");
        message.setAppCode("demo");
        message.setEnv("dev");
        message.setNamespace("default");
        message.setConfigKey("switch");
        message.setConfigValue(configValue);
        message.setTargetVersion(2L);
        return message;
    }
}
