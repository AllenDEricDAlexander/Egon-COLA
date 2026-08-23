package ${package}.starter.config.encryption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AesGcmConfigDecryptorTest {

    private final AesGcmConfigDecryptor decryptor = new AesGcmConfigDecryptor();

    @Test
    void decryptsValueCreatedByCli() throws Exception {
        char[] key = "12345678901234567890123456789012".toCharArray();
        String encrypted = ConfigCipherCli.encrypt(key, "secret-value");
        assertThat(encrypted).startsWith("ENC(v1:");
        assertThat(decryptor.decrypt(encrypted, key)).isEqualTo("secret-value");
    }

    @Test
    void encryptsSamePlainTextWithDifferentIv() throws Exception {
        char[] key = "12345678901234567890123456789012".toCharArray();
        String first = ConfigCipherCli.encrypt(key, "same-value");
        String second = ConfigCipherCli.encrypt(key, "same-value");
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void rejectsInvalidKeyLength() throws Exception {
        char[] key = "12345678901234567890123456789012".toCharArray();
        String encrypted = ConfigCipherCli.encrypt(key, "secret-value");
        assertThatThrownBy(() -> decryptor.decrypt(encrypted, "short".toCharArray()))
                .isInstanceOf(ConfigDecryptException.class);
    }

    @Test
    void rejectsCommandLineSecretArguments() {
        String key = "12345678901234567890123456789012";
        String plainText = "provided-plain";

        assertThatThrownBy(() -> ConfigCipherCli.main(new String[]{key, plainText}))
                .isInstanceOf(IllegalArgumentException.class)
                .satisfies(ex -> assertThat(ex.getMessage())
                        .contains("Usage: ConfigCipherCli")
                        .doesNotContain(key)
                        .doesNotContain(plainText));
    }

    @Test
    void removesOnlyOneTerminalLineEndingFromCliStdinText() {
        assertThat(ConfigCipherCli.stripOneTerminalLineEnding("secret  \t\n")).isEqualTo("secret  \t");
        assertThat(ConfigCipherCli.stripOneTerminalLineEnding("secret  \t\r\n")).isEqualTo("secret  \t");
        assertThat(ConfigCipherCli.stripOneTerminalLineEnding("secret  \t\r")).isEqualTo("secret  \t");
        assertThat(ConfigCipherCli.stripOneTerminalLineEnding("secret\n\n")).isEqualTo("secret\n");
        assertThat(ConfigCipherCli.stripOneTerminalLineEnding("secret  \t")).isEqualTo("secret  \t");
    }
}
