package top.egon.cola.platform.tianquan.shoubing.starter.observability;

import jakarta.validation.Validator;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.function.BooleanSupplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthAuditProperties;

/** Owns the existing exclusive spool lease and filesystem readiness checks. */
@Slf4j
@RequiredArgsConstructor
public class IdpSecurityObservationReadinessComponent {
    @Qualifier("oauthAuditProperties") private final OAuthAuditProperties audit;
    @Qualifier("defaultValidator") private final Validator validator;

    public FileChannel acquireSpoolLease() throws IOException {
        new ValidationUtils(validator).validate(audit);
        Path directory = audit.spoolDirectory();
        if (Files.isSymbolicLink(directory)) { throw new IllegalStateException("OAUTH_AUDIT_SPOOL_SYMLINK"); }
        if (!ownerKeyValid()) { throw new IllegalStateException("OAUTH_AUDIT_KEY_UNAVAILABLE"); }
        Files.createDirectories(directory);
        Path lockPath = directory.resolve(".receiver.lock");
        var channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
        try {
            Files.setPosixFilePermissions(lockPath, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
            if (channel.tryLock() == null) { throw new IllegalStateException("OAUTH_AUDIT_SPOOL_IN_USE"); }
            Path probe = Files.createTempFile(directory, ".readiness-", ".tmp");
            Path published = probe.resolveSibling(probe.getFileName() + ".published");
            try {
                try (var file = FileChannel.open(probe, StandardOpenOption.WRITE)) { file.force(true); }
                Files.move(probe, published, StandardCopyOption.ATOMIC_MOVE);
                try (var parent = FileChannel.open(directory, StandardOpenOption.READ)) { parent.force(true); }
            } finally { Files.deleteIfExists(probe); Files.deleteIfExists(published); }
            return channel;
        } catch (IOException | RuntimeException failure) {
            try { channel.close(); } catch (IOException closeFailure) { failure.addSuppressed(closeFailure); }
            throw failure;
        }
    }

    public BooleanSupplier readiness(FileChannel lease) {
        return () -> {
            try {
                if (!lease.isOpen() || Files.isSymbolicLink(audit.spoolDirectory())
                        || !Files.isWritable(audit.spoolDirectory()) || !ownerKeyValid()) { return false; }
                long bytes = 0;
                try (var files = Files.newDirectoryStream(audit.spoolDirectory())) {
                    for (var path : files) {
                        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) { continue; }
                        String name = path.getFileName().toString();
                        if (name.endsWith(".dead") || name.endsWith(".evt")
                                && !Files.exists(path.resolveSibling(name + ".sent"))) { return false; }
                        bytes = Math.addExact(bytes, Files.size(path));
                        if (bytes >= audit.spoolMaxBytes()) { return false; }
                    }
                }
                return true;
            } catch (IOException | ArithmeticException unavailable) { return false; }
        };
    }

    private boolean ownerKeyValid() throws IOException {
        if (!Files.isRegularFile(audit.hmacKeyFile(), LinkOption.NOFOLLOW_LINKS)) { return false; }
        var permissions = Files.getPosixFilePermissions(audit.hmacKeyFile());
        long length = Files.size(audit.hmacKeyFile());
        return permissions.stream().allMatch(permission -> permission.name().startsWith("OWNER_"))
                && length >= 32 && length <= 4096;
    }
}
