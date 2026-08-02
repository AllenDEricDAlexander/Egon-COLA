package top.egon.cola.platform.idp.admin.token.infrastructure;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Objects;
import java.util.Set;

public final class RsaPemKeyLoader {

    private static final long MAXIMUM_KEY_FILE_BYTES = 65_536L;
    private static final Set<PosixFilePermission> DISCLOSING_PERMISSIONS =
            Set.of(
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_WRITE,
                    PosixFilePermission.GROUP_EXECUTE,
                    PosixFilePermission.OTHERS_READ,
                    PosixFilePermission.OTHERS_WRITE,
                    PosixFilePermission.OTHERS_EXECUTE
            );

    public KeyMaterial load(Path publicKeyFile, Path privateKeyFile) {
        Path publicPath = readableRegularFile(publicKeyFile, "publicKeyFile");
        Path privatePath = readableRegularFile(
                privateKeyFile,
                "privateKeyFile"
        );
        requireOwnerOnly(privatePath);
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            RSAPublicKey publicKey = (RSAPublicKey) factory.generatePublic(
                    new X509EncodedKeySpec(readPem(
                            publicPath,
                            "PUBLIC KEY"
                    ))
            );
            RSAPrivateKey privateKey = (RSAPrivateKey) factory.generatePrivate(
                    new PKCS8EncodedKeySpec(readPem(
                            privatePath,
                            "PRIVATE KEY"
                    ))
            );
            if (publicKey.getModulus().bitLength() < 2_048
                    || !publicKey.getModulus().equals(
                            privateKey.getModulus()
                    )) {
                throw new IllegalStateException("invalid RSA key pair");
            }
            return new KeyMaterial(publicKey, privateKey);
        } catch (GeneralSecurityException | IOException exception) {
            throw new IllegalStateException(
                    "cannot load RSA signing key",
                    exception
            );
        }
    }

    private Path readableRegularFile(Path value, String field) {
        Path path = Objects.requireNonNull(value, field).toAbsolutePath()
                .normalize();
        try {
            if (Files.isSymbolicLink(path)
                    || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                    || !Files.isReadable(path)
                    || Files.size(path) <= 0L
                    || Files.size(path) > MAXIMUM_KEY_FILE_BYTES) {
                throw new IllegalStateException(field + " is not a safe file");
            }
            return path;
        } catch (IOException exception) {
            throw new IllegalStateException(
                    field + " cannot be inspected",
                    exception
            );
        }
    }

    private void requireOwnerOnly(Path privateKeyFile) {
        PosixFileAttributeView view = Files.getFileAttributeView(
                privateKeyFile,
                PosixFileAttributeView.class,
                LinkOption.NOFOLLOW_LINKS
        );
        if (view == null) {
            return;
        }
        try {
            Set<PosixFilePermission> permissions = view.readAttributes()
                    .permissions();
            if (permissions.stream().anyMatch(
                    DISCLOSING_PERMISSIONS::contains
            )) {
                throw new IllegalStateException(
                        "privateKeyFile must be owner-only"
                );
            }
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "privateKeyFile permissions cannot be inspected",
                    exception
            );
        }
    }

    private byte[] readPem(Path path, String type) throws IOException {
        String content = Files.readString(path);
        String begin = "-----BEGIN " + type + "-----";
        String end = "-----END " + type + "-----";
        int beginIndex = content.indexOf(begin);
        int endIndex = content.indexOf(end);
        if (beginIndex != 0
                || endIndex <= begin.length()
                || !content.substring(endIndex + end.length()).isBlank()) {
            throw new IllegalStateException("invalid PEM key file");
        }
        String encoded = content.substring(begin.length(), endIndex)
                .replaceAll("\\s", "");
        try {
            return Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("invalid PEM key file", exception);
        }
    }

    public record KeyMaterial(
            RSAPublicKey publicKey,
            RSAPrivateKey privateKey
    ) {

        @Override
        public String toString() {
            return "KeyMaterial[publicKey=<present>, privateKey=<redacted>]";
        }
    }
}
