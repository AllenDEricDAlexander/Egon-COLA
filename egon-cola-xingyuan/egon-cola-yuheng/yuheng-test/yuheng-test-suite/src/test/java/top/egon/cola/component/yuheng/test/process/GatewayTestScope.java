package top.egon.cola.component.yuheng.test.process;

import java.nio.file.Path;
import java.util.UUID;

public record GatewayTestScope(
        String suffix,
        String env,
        String namespace,
        String topic,
        Path dataDirectory
) {

    public static GatewayTestScope create(Path baseDirectory) {
        String suffix = UUID.randomUUID()
                .toString()
                .replace("-", "");
        return new GatewayTestScope(
                suffix,
                "yuheng-" + suffix,
                "test-" + suffix,
                "yuheng-call-" + suffix,
                baseDirectory.resolve("engine-" + suffix)
        );
    }

    public Path dataDirectory(String processName) {
        if (processName == null
                || !processName.matches("[A-Za-z0-9][A-Za-z0-9._-]*")) {
            throw new IllegalArgumentException(
                    "processName must be a safe path segment"
            );
        }
        return dataDirectory.resolveSibling(
                processName + "-" + suffix
        );
    }

    public Path processOutputDirectory() {
        return dataDirectory.resolveSibling("processes-" + suffix);
    }
}
