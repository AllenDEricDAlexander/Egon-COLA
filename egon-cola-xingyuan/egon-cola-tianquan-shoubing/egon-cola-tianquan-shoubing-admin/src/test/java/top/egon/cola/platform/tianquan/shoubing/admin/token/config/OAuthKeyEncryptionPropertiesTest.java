package top.egon.cola.platform.tianquan.shoubing.admin.token.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthPayloadCipherComponent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthKeyEncryptionPropertiesTest {

    @TempDir Path temporary;

    @Test
    void scannedPropertiesSatisfyThePayloadCipherQualifierWithoutAHandwrittenBeanAlias() throws Exception {
        Path key = Files.write(temporary.resolve("test-kek"), new byte[32]);
        Files.setPosixFilePermissions(key, PosixFilePermissions.fromString("rw-------"));
        new ApplicationContextRunner().withUserConfiguration(PropertiesConfiguration.class)
                .withBean(OAuthPayloadCipherComponent.class)
                .withPropertyValues("egon.tianquan-shoubing.oauth.key-encryption.key-id=test-kek",
                        "egon.tianquan-shoubing.oauth.key-encryption.key-file=" + key)
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(OAuthPayloadCipherComponent.class);
                    assertThat(context.getBean(OAuthKeyEncryptionProperties.class).getKeyFile()).isEqualTo(key);
                });
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties(OAuthKeyEncryptionProperties.class)
    static class PropertiesConfiguration { }
}
