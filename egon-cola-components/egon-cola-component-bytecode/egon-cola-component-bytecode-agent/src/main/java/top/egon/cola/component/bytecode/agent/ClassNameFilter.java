package top.egon.cola.component.bytecode.agent;

import java.util.List;
import java.util.regex.Pattern;

public final class ClassNameFilter {

    private static final int MINIMUM_CLASS_VERSION = 65;
    private static final int MAXIMUM_CLASS_VERSION = 69;
    private static final List<String> HARD_EXCLUSIONS = List.of(
            "java.",
            "javax.",
            "jakarta.",
            "jdk.",
            "sun.",
            "com.sun.",
            "org.objectweb.asm.",
            "org.springframework.",
            "org.slf4j.",
            "ch.qos.logback.",
            "io.micrometer.",
            "top.egon.cola.component.bytecode."
    );
    private static final List<String> GENERATED_PROXY_MARKERS = List.of(
            "$$SpringCGLIB$$",
            "$$FastClassBySpringCGLIB$$",
            "$$EnhancerBySpringCGLIB$$"
    );

    private final List<Pattern> includes;
    private final List<Pattern> excludes;

    public ClassNameFilter(AgentConfiguration configuration) {
        this.includes = compile(configuration.includes());
        this.excludes = compile(configuration.excludes());
    }

    public boolean matches(ClassLoader loader, String internalClassName, byte[] classfileBuffer) {
        if (loader == null || internalClassName == null || !supported(classfileBuffer)) {
            return false;
        }
        String className = internalClassName.replace('/', '.');
        if (HARD_EXCLUSIONS.stream().anyMatch(className::startsWith)) {
            return false;
        }
        if (GENERATED_PROXY_MARKERS.stream().anyMatch(className::contains)) {
            return false;
        }
        String simpleName = className.substring(className.lastIndexOf('.') + 1);
        if (isJdkProxy(simpleName)) {
            return false;
        }
        return includes.stream().anyMatch(pattern -> pattern.matcher(className).matches())
                && excludes.stream().noneMatch(pattern -> pattern.matcher(className).matches());
    }

    private boolean isJdkProxy(String simpleName) {
        if (!simpleName.startsWith("$Proxy") || simpleName.length() == "$Proxy".length()) {
            return false;
        }
        return simpleName.substring("$Proxy".length()).chars().allMatch(Character::isDigit);
    }

    private boolean supported(byte[] classfileBuffer) {
        if (classfileBuffer == null || classfileBuffer.length < 8
                || classfileBuffer[0] != (byte) 0xCA
                || classfileBuffer[1] != (byte) 0xFE
                || classfileBuffer[2] != (byte) 0xBA
                || classfileBuffer[3] != (byte) 0xBE) {
            return false;
        }
        int majorVersion = (Byte.toUnsignedInt(classfileBuffer[6]) << 8)
                | Byte.toUnsignedInt(classfileBuffer[7]);
        return majorVersion >= MINIMUM_CLASS_VERSION && majorVersion <= MAXIMUM_CLASS_VERSION;
    }

    private List<Pattern> compile(List<String> globs) {
        return globs.stream().map(this::compile).toList();
    }

    private Pattern compile(String glob) {
        StringBuilder expression = new StringBuilder("^");
        for (int index = 0; index < glob.length(); index++) {
            char character = glob.charAt(index);
            if (character == '*') {
                expression.append(".*");
            } else if (character == '?') {
                expression.append('.');
            } else {
                if ("\\.[]{}()+-^$|".indexOf(character) >= 0) {
                    expression.append('\\');
                }
                expression.append(character);
            }
        }
        return Pattern.compile(expression.append('$').toString());
    }
}
