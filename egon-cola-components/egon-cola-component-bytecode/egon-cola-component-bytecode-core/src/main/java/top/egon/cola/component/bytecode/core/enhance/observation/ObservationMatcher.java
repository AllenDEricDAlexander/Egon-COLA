package top.egon.cola.component.bytecode.core.enhance.observation;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import top.egon.cola.component.bytecode.api.observation.EgonObserved;
import top.egon.cola.component.bytecode.core.enhance.MethodId;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public final class ObservationMatcher {

    private static final String ANNOTATION_DESCRIPTOR = Type.getDescriptor(EgonObserved.class);
    private static final int MAXIMUM_TAGS = 8;
    private static final int MAXIMUM_TAG_KEY_LENGTH = 64;
    private static final int MAXIMUM_TAG_VALUE_LENGTH = 128;
    private static final Pattern TAG_KEY = Pattern.compile("[a-zA-Z][a-zA-Z0-9_.-]*");
    private static final List<String> HARD_EXCLUSIONS = List.of(
            "java.", "javax.", "jakarta.", "jdk.", "sun.", "com.sun.",
            "org.objectweb.asm.", "org.springframework.", "org.slf4j.",
            "ch.qos.logback.", "io.micrometer.",
            "top.egon.cola.component.bytecode."
    );

    private final List<Pattern> classIncludes;
    private final List<Pattern> methodIncludes;
    private final List<Pattern> exclusions;
    private final boolean observeConstructors;
    private final long defaultSlowThresholdNanos;

    public ObservationMatcher(
            List<String> classIncludes,
            List<String> methodIncludes,
            List<String> exclusions,
            boolean observeConstructors,
            long defaultSlowThresholdNanos
    ) {
        this.classIncludes = compile(classIncludes);
        this.methodIncludes = compile(methodIncludes.isEmpty() ? List.of("*") : methodIncludes);
        this.exclusions = compile(exclusions);
        this.observeConstructors = observeConstructors;
        this.defaultSlowThresholdNanos = defaultSlowThresholdNanos;
    }

    public Optional<ObservationPolicy> match(String owner, MethodNode method) {
        String className = owner.replace('/', '.');
        if (unsupported(className, method)
                || excluded(className, method.name)) {
            return Optional.empty();
        }
        AnnotationNode annotation = annotation(method);
        boolean constructor = "<init>".equals(method.name);
        boolean configured = matches(classIncludes, className)
                && (constructor
                ? observeConstructors
                : matches(methodIncludes, method.name));
        if (annotation == null && !configured) {
            return Optional.empty();
        }
        AnnotationValues annotationValues = values(annotation);
        long thresholdNanos = annotationValues.slowThresholdMillis < 0L
                ? defaultSlowThresholdNanos
                : Math.multiplyExact(annotationValues.slowThresholdMillis, 1_000_000L);
        return Optional.of(new ObservationPolicy(
                MethodId.compute(owner, method.name, method.desc),
                owner,
                method.name,
                method.desc,
                method.access,
                constructor,
                layer(className),
                annotationValues.tags,
                thresholdNanos
        ));
    }

    private boolean unsupported(String className, MethodNode method) {
        if (HARD_EXCLUSIONS.stream().anyMatch(className::startsWith)) {
            return true;
        }
        int unsupportedAccess = Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE
                | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE;
        return (method.access & unsupportedAccess) != 0
                || "<clinit>".equals(method.name)
                || method.name.startsWith("lambda$");
    }

    private boolean excluded(String className, String methodName) {
        return matches(exclusions, className)
                || matches(exclusions, className + "#" + methodName);
    }

    private AnnotationNode annotation(MethodNode method) {
        List<AnnotationNode> annotations = new ArrayList<>();
        if (method.visibleAnnotations != null) {
            annotations.addAll(method.visibleAnnotations);
        }
        if (method.invisibleAnnotations != null) {
            annotations.addAll(method.invisibleAnnotations);
        }
        return annotations.stream()
                .filter(candidate -> ANNOTATION_DESCRIPTOR.equals(candidate.desc))
                .findFirst()
                .orElse(null);
    }

    private AnnotationValues values(AnnotationNode annotation) {
        if (annotation == null || annotation.values == null) {
            return new AnnotationValues(Map.of(), -1L);
        }
        Map<String, String> tags = Map.of();
        long slowThresholdMillis = -1L;
        for (int index = 0; index < annotation.values.size(); index += 2) {
            String name = String.valueOf(annotation.values.get(index));
            Object value = annotation.values.get(index + 1);
            if ("tags".equals(name) && value instanceof List<?> configuredTags) {
                tags = validateTags(configuredTags);
            } else if ("slowThresholdMillis".equals(name) && value instanceof Number number) {
                slowThresholdMillis = number.longValue();
            }
        }
        if (slowThresholdMillis < -1L) {
            throw new IllegalArgumentException("slowThresholdMillis must be -1 or non-negative");
        }
        return new AnnotationValues(tags, slowThresholdMillis);
    }

    private Map<String, String> validateTags(List<?> configuredTags) {
        if (configuredTags.size() > MAXIMUM_TAGS) {
            throw new IllegalArgumentException("Observation supports at most 8 static tags");
        }
        Map<String, String> tags = new LinkedHashMap<>();
        for (Object configuredTag : configuredTags) {
            String tag = String.valueOf(configuredTag);
            int separator = tag.indexOf('=');
            if (separator < 1 || separator == tag.length() - 1) {
                throw new IllegalArgumentException(
                        "Observation tags must use non-empty key=value syntax");
            }
            String key = tag.substring(0, separator).trim();
            String value = tag.substring(separator + 1).trim();
            if (key.length() > MAXIMUM_TAG_KEY_LENGTH || !TAG_KEY.matcher(key).matches()) {
                throw new IllegalArgumentException("Invalid observation tag key: " + key);
            }
            if (value.length() > MAXIMUM_TAG_VALUE_LENGTH
                    || value.contains("${") || value.contains("#{")) {
                throw new IllegalArgumentException("Invalid observation tag value for " + key);
            }
            if (tags.putIfAbsent(key, value) != null) {
                throw new IllegalArgumentException("Duplicate observation tag key: " + key);
            }
        }
        return Map.copyOf(tags);
    }

    private String layer(String className) {
        String normalized = "." + className.toLowerCase(Locale.ROOT) + ".";
        for (String layer : List.of(
                "domain", "application", "infrastructure", "adapter",
                "facade", "starter", "common")) {
            if (normalized.contains("." + layer + ".")) {
                return layer.toUpperCase(Locale.ROOT);
            }
        }
        return "UNKNOWN";
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

    private boolean matches(List<Pattern> patterns, String value) {
        return patterns.stream().anyMatch(pattern -> pattern.matcher(value).matches());
    }

    private record AnnotationValues(Map<String, String> tags, long slowThresholdMillis) {
    }
}
