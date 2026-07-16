package top.egon.cola.component.bytecode.test.agent;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

final class ForkedAgentProcess {

    private static final int MAXIMUM_OUTPUT_CHARACTERS = 64 * 1024;

    Result run(String mainClass) throws Exception {
        Path javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java");
        return run(javaExecutable, mainClass,
                "enabled=true,features=executor,include=sample.bytecode.agent.*");
    }

    Result run(String mainClass, String agentArguments) throws Exception {
        Path javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java");
        return run(javaExecutable, mainClass, agentArguments);
    }

    Result run(Path javaExecutable, String mainClass) throws Exception {
        return run(javaExecutable, mainClass,
                "enabled=true,features=executor,include=sample.bytecode.agent.*");
    }

    Result run(
            Path javaExecutable,
            String mainClass,
            String agentArguments
    ) throws Exception {
        Path agent = Path.of(System.getProperty("egon.bytecode.agent.jar"));
        if (!Files.isRegularFile(agent)) {
            throw new IllegalStateException("Agent JAR is missing: " + agent);
        }
        String classPath = System.getProperty(
                "surefire.test.class.path", System.getProperty("java.class.path"));
        List<String> command = new ArrayList<>();
        command.add(javaExecutable.toString());
        command.add("-Xverify:all");
        command.add("-javaagent:" + agent + "=" + agentArguments);
        command.add("-cp");
        command.add(classPath);
        command.add(mainClass);

        Process process = new ProcessBuilder(command).start();
        try (var readers = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<String> standardOutput = readers.submit(() -> bounded(process.getInputStream()));
            Future<String> errorOutput = readers.submit(() -> bounded(process.getErrorStream()));
            boolean completed = process.waitFor(Duration.ofSeconds(60).toMillis(), TimeUnit.MILLISECONDS);
            if (!completed) {
                process.destroyForcibly();
                process.waitFor(10, TimeUnit.SECONDS);
                throw new IllegalStateException("Forked Agent process timed out after 60 seconds");
            }
            return new Result(
                    process.exitValue(),
                    standardOutput.get(10, TimeUnit.SECONDS),
                    errorOutput.get(10, TimeUnit.SECONDS)
            );
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private String bounded(InputStream stream) throws Exception {
        StringBuilder output = new StringBuilder();
        byte[] buffer = new byte[4096];
        int count;
        while ((count = stream.read(buffer)) >= 0) {
            if (output.length() < MAXIMUM_OUTPUT_CHARACTERS) {
                int accepted = Math.min(count, MAXIMUM_OUTPUT_CHARACTERS - output.length());
                output.append(new String(buffer, 0, accepted, java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        return output.toString();
    }

    record Result(int exitCode, String standardOutput, String errorOutput) {
        String combinedOutput() {
            return standardOutput + errorOutput;
        }
    }
}
