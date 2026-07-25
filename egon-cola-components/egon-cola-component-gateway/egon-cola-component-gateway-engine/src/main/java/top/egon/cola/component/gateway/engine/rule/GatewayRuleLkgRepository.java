package top.egon.cola.component.gateway.engine.rule;

import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

public final class GatewayRuleLkgRepository {

    private final Path groupDirectory;

    public GatewayRuleLkgRepository(
            Path engineDataDirectory,
            String gatewayGroupCode) {
        if (gatewayGroupCode == null
                || !gatewayGroupCode.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,63}")) {
            throw new IllegalArgumentException("invalid gatewayGroupCode");
        }
        groupDirectory = engineDataDirectory
                .resolve("rules")
                .resolve(gatewayGroupCode);
    }

    public void persistAndActivate(
            GatewayRuleSnapshot snapshot,
            byte[] snapshotJson) {
        try {
            Path releases = groupDirectory.resolve("releases");
            Files.createDirectories(releases);
            Path release = releases.resolve(snapshot.releaseId() + ".json");
            Path checksum = releases.resolve(
                    snapshot.releaseId() + ".sha256"
            );
            atomicWrite(release, snapshotJson);
            atomicWrite(
                    checksum,
                    snapshot.artifactSha256().getBytes(StandardCharsets.UTF_8)
            );
            atomicWrite(
                    groupDirectory.resolve("active"),
                    snapshot.releaseId().getBytes(StandardCharsets.UTF_8)
            );
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "GATEWAY_RULE_LKG_WRITE_FAILED",
                    failure
            );
        }
    }

    public Optional<StoredGatewayRule> loadActive() {
        try {
            Path active = groupDirectory.resolve("active");
            if (!Files.exists(active)) {
                return Optional.empty();
            }
            String releaseId = Files.readString(active).trim();
            Path releases = groupDirectory.resolve("releases");
            byte[] json = Files.readAllBytes(
                    releases.resolve(releaseId + ".json")
            );
            String sha = Files.readString(
                    releases.resolve(releaseId + ".sha256")
            ).trim();
            return Optional.of(new StoredGatewayRule(releaseId, sha, json));
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "GATEWAY_RULE_LKG_READ_FAILED",
                    failure
            );
        }
    }

    private void atomicWrite(Path target, byte[] value) throws IOException {
        Path temporary = target.resolveSibling(
                target.getFileName() + ".tmp"
        );
        try (FileChannel channel = FileChannel.open(
                temporary,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            channel.write(ByteBuffer.wrap(value));
            channel.force(true);
        }
        try {
            Files.move(
                    temporary,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
            Files.move(
                    temporary,
                    target,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
        try (FileChannel directory = FileChannel.open(
                target.getParent(),
                StandardOpenOption.READ)) {
            directory.force(true);
        }
    }

    public record StoredGatewayRule(
            String releaseId,
            String artifactSha256,
            byte[] snapshotJson
    ) {

        public StoredGatewayRule {
            snapshotJson = snapshotJson.clone();
        }

        @Override
        public byte[] snapshotJson() {
            return snapshotJson.clone();
        }
    }
}
