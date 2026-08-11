package top.egon.cola.platform.idp.starter.admission;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.util.Base64;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OwnerOnlyPrivateKeyLoaderTest {

    @TempDir
    Path directory;

    @Test
    void loadsAnAbsoluteOwnerOnlyPkcs8RsaPrivateKey() throws Exception {
        KeyPair pair = rsaKeyPair();
        Path keyFile = writePrivateKey(pair, "admission-private.pem");
        Files.setPosixFilePermissions(keyFile, Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE
        ));

        RSAPrivateKey loaded = new OwnerOnlyPrivateKeyLoader().load(keyFile);

        assertThat(loaded.getModulus())
                .isEqualTo(((RSAPrivateKey) pair.getPrivate()).getModulus());
    }

    @Test
    void rejectsAGroupReadablePrivateKey() throws Exception {
        Path keyFile = writePrivateKey(
                rsaKeyPair(),
                "group-readable-private.pem");
        Files.setPosixFilePermissions(keyFile, Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.GROUP_READ
        ));

        assertThatThrownBy(() -> new OwnerOnlyPrivateKeyLoader().load(keyFile))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("privateKeyPath must be owner-only");
    }

    @Test
    void rejectsRelativeAndSymbolicLinkPaths() throws Exception {
        Path keyFile = writePrivateKey(rsaKeyPair(), "real-private.pem");
        Files.setPosixFilePermissions(keyFile, Set.of(
                PosixFilePermission.OWNER_READ
        ));
        Path link = directory.resolve("linked-private.pem");
        Files.createSymbolicLink(link, keyFile);

        assertThatThrownBy(() -> new OwnerOnlyPrivateKeyLoader().load(
                Path.of("relative-private.pem")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("privateKeyPath must be absolute");
        assertThatThrownBy(() -> new OwnerOnlyPrivateKeyLoader().load(link))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("privateKeyPath must be a regular non-link file");
    }

    private Path writePrivateKey(KeyPair pair, String filename)
            throws Exception {
        String encoded = Base64.getMimeEncoder(64, new byte[]{'\n'})
                .encodeToString(pair.getPrivate().getEncoded());
        Path path = directory.resolve(filename);
        Files.writeString(
                path,
                "-----BEGIN PRIVATE KEY-----\n"
                        + encoded
                        + "\n-----END PRIVATE KEY-----\n",
                StandardCharsets.US_ASCII
        );
        return path;
    }

    private static KeyPair rsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }
}
