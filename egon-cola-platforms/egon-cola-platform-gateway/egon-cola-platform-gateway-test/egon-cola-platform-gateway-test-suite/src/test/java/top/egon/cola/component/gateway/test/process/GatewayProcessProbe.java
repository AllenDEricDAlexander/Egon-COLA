package top.egon.cola.component.gateway.test.process;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public final class GatewayProcessProbe {

    private GatewayProcessProbe() {
    }

    public static void main(String[] arguments) throws Exception {
        Map<String, String> options = Arrays.stream(arguments)
                .map(argument -> argument.split("=", 2))
                .filter(parts -> parts.length == 2)
                .collect(Collectors.toMap(
                        parts -> parts[0].replaceFirst("^--", ""),
                        parts -> parts[1]
                ));
        String name = options.getOrDefault("name", "probe");
        Path shutdownLog = options.containsKey("shutdown-log")
                ? Path.of(options.get("shutdown-log"))
                : null;
        boolean blockShutdown = Boolean.parseBoolean(
                options.getOrDefault("block-shutdown", "false")
        );
        Runtime.getRuntime().addShutdownHook(new Thread(
                () -> onShutdown(name, shutdownLog, blockShutdown),
                name + "-shutdown"
        ));
        System.out.println("READY " + name);
        System.out.flush();
        new CountDownLatch(1).await();
    }

    private static void onShutdown(
            String name,
            Path shutdownLog,
            boolean blockShutdown) {
        if (shutdownLog != null) {
            try {
                Files.writeString(
                        shutdownLog,
                        name + System.lineSeparator(),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                );
            } catch (IOException failure) {
                throw new IllegalStateException(
                        "cannot record probe shutdown",
                        failure
                );
            }
        }
        if (blockShutdown) {
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
