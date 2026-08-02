package top.egon.cola.platform.idp.admin.token;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.egon.cola.platform.idp.admin.token.infrastructure.RsaPemKeyLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RsaPemKeyLoaderTest {

    @TempDir
    Path directory;

    @Test
    void loadsExternalRs256KeyPairAndRequiresPrivateOwnerOnlyFile()
            throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        Path publicFile = directory.resolve("idp-public.pem");
        Path privateFile = directory.resolve("idp-private.pem");
        Files.writeString(publicFile, pem(
                "PUBLIC KEY",
                pair.getPublic().getEncoded()
        ));
        Files.writeString(privateFile, pem(
                "PRIVATE KEY",
                pair.getPrivate().getEncoded()
        ));
        Files.setPosixFilePermissions(privateFile, EnumSet.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE
        ));

        RsaPemKeyLoader.KeyMaterial material = new RsaPemKeyLoader().load(
                publicFile,
                privateFile
        );

        assertEquals(pair.getPublic(), material.publicKey());
        assertEquals(pair.getPrivate(), material.privateKey());
        assertFalse(material.toString().contains(
                pair.getPrivate().toString()
        ));

        Files.setPosixFilePermissions(privateFile, Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.GROUP_READ
        ));
        assertThrows(IllegalStateException.class, () ->
                new RsaPemKeyLoader().load(publicFile, privateFile));
    }

    private String pem(String type, byte[] encoded) {
        return "-----BEGIN " + type + "-----\n"
                + Base64.getMimeEncoder(64, new byte[]{'\n'})
                .encodeToString(encoded)
                + "\n-----END " + type + "-----\n";
    }
}
