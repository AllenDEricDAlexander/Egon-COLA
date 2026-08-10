package top.egon.cola.platform.idp.admin.support.rbac3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.function.Supplier;

public final class FileServiceAuthorizationSupplier
        implements Supplier<String> {

    private final Path authorizationHeaderFile;

    public FileServiceAuthorizationSupplier(Path authorizationHeaderFile) {
        if (authorizationHeaderFile == null
                || !authorizationHeaderFile.isAbsolute()) {
            throw new IllegalArgumentException(
                    "RBAC3 authorization header file must be absolute"
            );
        }
        this.authorizationHeaderFile = authorizationHeaderFile.normalize();
    }

    @Override
    public String get() {
        verifyRestrictedPermissions();
        try {
            String value = Files.readString(
                    authorizationHeaderFile,
                    StandardCharsets.UTF_8
            ).trim();
            if (value.isBlank()
                    || value.contains("\r")
                    || value.contains("\n")) {
                throw new IllegalStateException(
                        "RBAC3 authorization header file is invalid"
                );
            }
            return value;
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "cannot read RBAC3 authorization header file",
                    exception
            );
        }
    }

    private void verifyRestrictedPermissions() {
        if (!Files.isRegularFile(
                authorizationHeaderFile,
                LinkOption.NOFOLLOW_LINKS
        )) {
            throw new IllegalStateException(
                    "RBAC3 authorization header file must be a regular file"
            );
        }
        try {
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(
                    authorizationHeaderFile
            );
            if (permissions.contains(PosixFilePermission.GROUP_READ)
                    || permissions.contains(PosixFilePermission.GROUP_WRITE)
                    || permissions.contains(PosixFilePermission.GROUP_EXECUTE)
                    || permissions.contains(PosixFilePermission.OTHERS_READ)
                    || permissions.contains(PosixFilePermission.OTHERS_WRITE)
                    || permissions.contains(PosixFilePermission.OTHERS_EXECUTE)) {
                throw new IllegalStateException(
                        "RBAC3 authorization header file must be owner-only"
                );
            }
        } catch (UnsupportedOperationException exception) {
            if (!Files.isReadable(authorizationHeaderFile)) {
                throw new IllegalStateException(
                        "RBAC3 authorization header file is not readable",
                        exception
                );
            }
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "cannot inspect RBAC3 authorization header file",
                    exception
            );
        }
    }
}
