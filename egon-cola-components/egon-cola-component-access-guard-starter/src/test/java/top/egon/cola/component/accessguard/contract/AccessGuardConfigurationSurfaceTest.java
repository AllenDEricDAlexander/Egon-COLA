package top.egon.cola.component.accessguard.contract;

import org.junit.jupiter.api.Test;
import org.springframework.util.ClassUtils;
import top.egon.cola.component.accessguard.autoconfigure.AccessGuardProperties;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

class AccessGuardConfigurationSurfaceTest {

    private static final Set<String> EXPECTED_PROPERTIES = Set.of(
            "enabled",
            "engine",
            "storage",
            "defaults.rejection",
            "key.contributors",
            "key.trustedProxies",
            "key.headers",
            "key.hmacSecret",
            "key.maxPartLength",
            "redisson.clientBeanName",
            "redisson.keyPrefix",
            "redisson.application",
            "local.maxEntries",
            "local.cleanupInterval",
            "local.idleTtl",
            "threadPool.name",
            "threadPool.corePoolSize",
            "threadPool.maxPoolSize",
            "threadPool.queueCapacity",
            "threadPool.keepAlive",
            "rules",
            "rules.*.enabled",
            "rules.*.key.contributors",
            "rules.*.denyList.enabled",
            "rules.*.denyList.dataVersion",
            "rules.*.allowList.enabled",
            "rules.*.allowList.mode",
            "rules.*.allowList.dataVersion",
            "rules.*.penaltyBox.enabled",
            "rules.*.penaltyBox.threshold",
            "rules.*.penaltyBox.violationTtl",
            "rules.*.penaltyBox.penaltyTtl",
            "rules.*.rateLimit.enabled",
            "rules.*.rateLimit.algorithm",
            "rules.*.rateLimit.capacity",
            "rules.*.rateLimit.refillTokens",
            "rules.*.rateLimit.refillPeriod",
            "rules.*.rateLimit.requestedTokens",
            "rules.*.timeLimit.enabled",
            "rules.*.timeLimit.mode",
            "rules.*.timeLimit.executor",
            "rules.*.timeLimit.timeout",
            "rules.*.timeLimit.cancelRunningTask",
            "rules.*.rejection.mode",
            "rules.*.rejection.fallbackMethod",
            "rules.*.rejection.returnJson",
            "rules.*.failurePolicies.keyResolution",
            "rules.*.failurePolicies.denyListStore",
            "rules.*.failurePolicies.allowListStore",
            "rules.*.failurePolicies.penaltyStore",
            "rules.*.failurePolicies.rateLimitBackend",
            "rules.*.failurePolicies.execution",
            "rules.*.failurePolicies.observability",
            "rules.*.observability.finalEvents",
            "rules.*.observability.stageEvents",
            "rules.*.observability.metrics",
            "rules.*.observability.logging",
            "rules.*.observability.endpoint");

    private static final List<String> REMOVED_TYPES = List.of(
            removed("annotation", "Do", "WhiteList"),
            removed("annotation", "Do", "RateLimiter"),
            removed("annotation", "Do", "Hystrix"),
            removed("annotation", "Timeout", "CircuitBreaker"),
            removed("agent", "Agent", "ProceedingJoinPoint"),
            removed("execution", "AccessGuard", "ExecutionService"));

    @Test
    void exposesOnlyTheV2PropertyAndRuntimeSurface() throws Exception {
        Map<String, String> getters = new LinkedHashMap<>();
        collectProperties(AccessGuardProperties.class, "", getters);
        collectProperties(AccessGuardProperties.Rule.class, "rules.*.", getters);

        assertThat(getters.keySet()).containsExactlyInAnyOrderElementsOf(EXPECTED_PROPERTIES);
        assertThat(unreadGetters(getters)).isEmpty();
        assertThat(REMOVED_TYPES).allSatisfy(type ->
                assertThat(ClassUtils.isPresent(type, getClass().getClassLoader()))
                        .as("legacy type %s", type)
                        .isFalse());
    }

    private static void collectProperties(
            Class<?> type,
            String prefix,
            Map<String, String> getters
    ) {
        for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                continue;
            }
            String path = prefix + field.getName();
            Class<?> fieldType = field.getType();
            if (!fieldType.isEnum()
                    && fieldType.getEnclosingClass() == AccessGuardProperties.class
                    && fieldType != AccessGuardProperties.Rule.class) {
                collectProperties(fieldType, path + ".", getters);
            } else {
                getters.put(path, getter(field));
            }
        }
    }

    private static String getter(Field field) {
        String prefix = field.getType() == boolean.class ? "is" : "get";
        return prefix + Character.toUpperCase(field.getName().charAt(0))
                + field.getName().substring(1);
    }

    private static String removed(String packageName, String first, String second) {
        return "top.egon.cola.component.accessguard." + packageName + "." + first + second;
    }

    private static List<String> unreadGetters(Map<String, String> getters)
            throws IOException, URISyntaxException {
        Path classes = Path.of(AccessGuardProperties.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
        List<byte[]> productionClasses = new ArrayList<>();
        try (var paths = Files.walk(classes.resolve("top/egon/cola/component/accessguard"))) {
            paths.filter(path -> path.toString().endsWith(".class"))
                    .filter(path -> !path.getFileName().toString().startsWith("AccessGuardProperties"))
                    .forEach(path -> productionClasses.add(read(path)));
        }
        Set<String> unread = new TreeSet<>();
        getters.forEach((property, getter) -> {
            byte[] symbol = getter.getBytes(StandardCharsets.UTF_8);
            if (productionClasses.stream().noneMatch(bytes -> contains(bytes, symbol))) {
                unread.add(property + " -> " + getter);
            }
        });
        return List.copyOf(unread);
    }

    private static byte[] read(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot inspect " + path, exception);
        }
    }

    private static boolean contains(byte[] bytes, byte[] symbol) {
        outer:
        for (int offset = 0; offset <= bytes.length - symbol.length; offset++) {
            for (int index = 0; index < symbol.length; index++) {
                if (bytes[offset + index] != symbol[index]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }
}
