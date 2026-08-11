package top.egon.cola.platform.idp.starter.admission;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * 从绝对、非符号链接且 owner-only 的外部文件装载 PKCS#8 RSA 私钥。
 *
 * <p>Loads a PKCS#8 RSA private key from an absolute, non-symbolic-link, owner-only external
 * file.</p>
 */
public final class OwnerOnlyPrivateKeyLoader {

    /** 所有组和其他用户权限；all group and other-user permissions. */
    private static final Set<PosixFilePermission> NON_OWNER_PERMISSIONS =
            EnumSet.of(
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_WRITE,
                    PosixFilePermission.GROUP_EXECUTE,
                    PosixFilePermission.OTHERS_READ,
                    PosixFilePermission.OTHERS_WRITE,
                    PosixFilePermission.OTHERS_EXECUTE
            );

    /**
     * 创建 owner-only 私钥装载器。
     *
     * <p>Creates the owner-only private-key loader.</p>
     */
    public OwnerOnlyPrivateKeyLoader() {
    }

    /**
     * 校验文件边界并装载 PKCS#8 RSA 私钥。
     *
     * <p>Validates the file boundary and loads a PKCS#8 RSA private key.</p>
     *
     * @param privateKeyPath 私钥绝对路径；absolute private-key path
     * @return RSA 私钥；RSA private key
     * @throws IllegalStateException 路径、权限或密钥内容不安全时抛出；when the path,
     * permissions, or key material is unsafe
     */
    public RSAPrivateKey load(Path privateKeyPath) {
        Objects.requireNonNull(privateKeyPath, "privateKeyPath");
        if (!privateKeyPath.isAbsolute()) {
            throw new IllegalStateException(
                    "privateKeyPath must be absolute"
            );
        }
        if (Files.isSymbolicLink(privateKeyPath)
                || !Files.isRegularFile(
                        privateKeyPath,
                        LinkOption.NOFOLLOW_LINKS
                )) {
            throw new IllegalStateException(
                    "privateKeyPath must be a regular non-link file"
            );
        }
        try {
            Set<PosixFilePermission> permissions =
                    Files.getPosixFilePermissions(
                            privateKeyPath,
                            LinkOption.NOFOLLOW_LINKS
                    );
            if (permissions.stream().anyMatch(
                    NON_OWNER_PERMISSIONS::contains)) {
                throw new IllegalStateException(
                        "privateKeyPath must be owner-only"
                );
            }
            String pem = Files.readString(
                    privateKeyPath,
                    StandardCharsets.US_ASCII
            );
            String encoded = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            if (encoded.isBlank()) {
                throw new IllegalStateException(
                        "privateKeyPath must contain a PKCS#8 RSA key"
                );
            }
            byte[] bytes = Base64.getDecoder().decode(encoded);
            return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(bytes));
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "privateKeyPath must contain a PKCS#8 RSA key",
                    exception
            );
        }
    }
}
