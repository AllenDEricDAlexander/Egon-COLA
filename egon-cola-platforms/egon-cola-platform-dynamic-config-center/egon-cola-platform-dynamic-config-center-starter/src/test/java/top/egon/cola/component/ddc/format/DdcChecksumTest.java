package top.egon.cola.component.ddc.format;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.ddc.model.config.DdcPublishMessage;

import static org.assertj.core.api.Assertions.assertThat;

class DdcChecksumTest {

    @Test
    void checksumIsStableForSameMessage() {
        DdcPublishMessage message = message("true");

        assertThat(DdcChecksum.sha256(message)).isEqualTo(DdcChecksum.sha256(message("true")));
    }

    @Test
    void checksumChangesWhenContentChanges() {
        assertThat(DdcChecksum.sha256(message("true"))).isNotEqualTo(DdcChecksum.sha256(message("false")));
    }

    private DdcPublishMessage message(String content) {
        DdcPublishMessage message = new DdcPublishMessage();
        message.setChangeId("c1");
        message.setBizCode("retail");
        message.setAppCode("demo");
        message.setEnv("dev");
        message.setResourceName("application.yml");
        message.setContent(content);
        message.setFormat("YAML");
        message.setTargetVersion(2L);
        message.setResourceChecksum(DdcChecksum.resource(
                message.getResourceName(),
                message.getFormat(),
                content
        ));
        return message;
    }
}
