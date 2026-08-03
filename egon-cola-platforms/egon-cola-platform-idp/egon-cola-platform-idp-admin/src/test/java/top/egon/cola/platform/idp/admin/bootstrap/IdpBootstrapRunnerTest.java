package top.egon.cola.platform.idp.admin.bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdpBootstrapRunnerTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void readsPasswordFromEnvironmentAndClearsForwardedCharacters() {
        RecordingBootstrapPort port = new RecordingBootstrapPort();
        IdpBootstrapRunner runner = new IdpBootstrapRunner(port);

        int result = runner.run(
                new String[]{"--idp-bootstrap-admin=alice"},
                Map.of("IDP_BOOTSTRAP_PASSWORD", "strong-password-1")
        );

        assertEquals(0, result);
        assertEquals("alice", port.username);
        assertEquals("strong-password-1", port.copiedPassword);
        assertArrayEquals(
                new char["strong-password-1".length()],
                port.forwardedPassword
        );
    }

    @Test
    void readsPasswordFromRestrictedFileBeforeEnvironment() throws IOException {
        Path passwordFile = temporaryDirectory.resolve("bootstrap.password");
        Files.writeString(passwordFile, "file-password-1\n");
        RecordingBootstrapPort port = new RecordingBootstrapPort();
        IdpBootstrapRunner runner = new IdpBootstrapRunner(port);

        int result = runner.run(
                new String[]{"--idp-bootstrap-admin=alice"},
                Map.of(
                        "IDP_BOOTSTRAP_PASSWORD_FILE",
                        passwordFile.toString(),
                        "IDP_BOOTSTRAP_PASSWORD",
                        "environment-password-1"
                )
        );

        assertEquals(0, result);
        assertEquals("file-password-1", port.copiedPassword);
        assertArrayEquals(
                new char["file-password-1".length()],
                port.forwardedPassword
        );
    }

    @Test
    void rejectsMissingPasswordFile() {
        IdpBootstrapRunner runner = new IdpBootstrapRunner(
                new RecordingBootstrapPort()
        );

        assertThrows(IllegalStateException.class, () -> runner.run(
                new String[]{"--idp-bootstrap-admin=alice"},
                Map.of(
                        "IDP_BOOTSTRAP_PASSWORD_FILE",
                        temporaryDirectory.resolve("missing.password").toString()
                )
        ));
    }

    @Test
    void rejectsPasswordArgumentsAndMissingEnvironmentSecret() {
        IdpBootstrapRunner runner = new IdpBootstrapRunner(
                new RecordingBootstrapPort()
        );

        assertThrows(IllegalArgumentException.class, () -> runner.run(
                new String[]{
                        "--idp-bootstrap-admin=alice",
                        "--password=leaked"
                },
                Map.of("IDP_BOOTSTRAP_PASSWORD", "strong-password-1")
        ));
        assertThrows(IllegalStateException.class, () -> runner.run(
                new String[]{"--idp-bootstrap-admin=alice"},
                Map.of()
        ));
    }

    @Test
    void doesNothingWithoutExplicitBootstrapArgument() {
        RecordingBootstrapPort port = new RecordingBootstrapPort();
        IdpBootstrapRunner runner = new IdpBootstrapRunner(port);

        assertEquals(0, runner.run(new String[0], Map.of()));
        assertEquals(0, port.calls);
    }

    private static final class RecordingBootstrapPort
            implements IdpBootstrapRunner.BootstrapPort {
        private int calls;
        private String username;
        private String copiedPassword;
        private char[] forwardedPassword;

        @Override
        public void bootstrap(String username, char[] password) {
            calls++;
            this.username = username;
            copiedPassword = new String(password);
            forwardedPassword = password;
        }
    }
}
