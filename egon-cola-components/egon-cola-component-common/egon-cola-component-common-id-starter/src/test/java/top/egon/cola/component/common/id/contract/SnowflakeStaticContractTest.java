package top.egon.cola.component.common.id.contract;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.id.generator.IdGenerator;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.component.common.id.time.TimeSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Freezes the Step 2 identity contract: one statically bound Snowflake engine, a
 * non-functional {@link LongIdGenerator} strategy, and no construction or reset seam
 * that a caller could use to fork the ID state.
 *
 * <p>The binding lifecycle is process-global, so each scenario runs in its own JVM. The
 * not-yet-existing symbols are reached reflectively on purpose: the first run must fail
 * because the contract is missing, not because this fixture stopped compiling.</p>
 */
@Timeout(value = 120, unit = TimeUnit.SECONDS)
class SnowflakeStaticContractTest {

    private static final String SNOWFLAKE_PACKAGE = "top.egon.cola.component.common.id.snowflake";

    private static final Duration TOLERANCE = Duration.ofMillis(5);

    private static final int MACHINE_ID_SHIFT = 12;

    private static final int TIMESTAMP_SHIFT = 22;

    @Test
    void snowflakeEntryPointsAreStaticOnlyAndInconstructible() {
        Class<?> facade = SnowflakeIdGenerator.class;

        assertTrue(Modifier.isFinal(facade.getModifiers()), "the static facade must not be extended");
        assertEquals(1, facade.getDeclaredConstructors().length, "exactly one declared constructor");
        for (Constructor<?> constructor : facade.getDeclaredConstructors()) {
            assertEquals(0, constructor.getParameterCount(), "no callable generator construction seam");
            assertTrue(Modifier.isPrivate(constructor.getModifiers()),
                    "the utility constructor must be inaccessible: " + constructor);
        }

        Set<String> instanceMethods = Arrays.stream(facade.getDeclaredMethods())
                .filter(method -> !Modifier.isStatic(method.getModifiers()))
                .map(Method::getName)
                .collect(Collectors.toSet());
        assertEquals(Set.of(), instanceMethods, "generation must not need an injected instance");
        assertEquals(Set.of("initialize", "nextLongId", "nextId"),
                Arrays.stream(facade.getDeclaredMethods())
                        .filter(method -> !Modifier.isPrivate(method.getModifiers()))
                        .map(Method::getName)
                        .collect(Collectors.toSet()),
                "the facade exposes binding plus the two ID entries only");
        assertTrue(Arrays.stream(facade.getDeclaredMethods())
                        .noneMatch(method -> method.getName().toLowerCase().contains("reset")),
                "a live engine must never be reset or reseeded");
    }

    @Test
    void longIdGeneratorDeclaresTwoIndependentAbstractOperations() {
        assertFalse(Arrays.stream(LongIdGenerator.class.getAnnotations())
                        .anyMatch(annotation -> annotation.annotationType().getSimpleName()
                                .equals("FunctionalInterface")),
                "REQ-014: the strategy is no longer a functional interface");
        List<Method> declared = Arrays.stream(LongIdGenerator.class.getDeclaredMethods()).toList();
        assertEquals(Set.of("nextLongId", "nextId"),
                declared.stream().map(Method::getName).collect(Collectors.toSet()));
        assertTrue(declared.stream().allMatch(method -> Modifier.isAbstract(method.getModifiers())),
                "both operations must be implemented by every named generator");
        assertEquals(long.class, find(declared, "nextLongId").getReturnType());
        assertEquals(String.class, find(declared, "nextId").getReturnType());
        assertTrue(IdGenerator.class.isAssignableFrom(LongIdGenerator.class),
                "the string contract still owns nextId");
    }

    @Test
    void engineKeepsTheOriginalCasAlgorithmBehindTheStaticFacade() throws Throwable {
        Class<?> engine = Class.forName(SNOWFLAKE_PACKAGE + ".SnowflakeLongIdGenerator");

        assertTrue(Modifier.isFinal(engine.getModifiers()), "the engine is a fixed implementation");
        assertTrue(LongIdGenerator.class.isAssignableFrom(engine), "the engine is the named LongIdGenerator");
        assertTrue(Modifier.isPublic(engine.getModifiers()),
                "clock-injected fixtures and JMH build isolated engines per fixture");
        assertEquals(List.of(1, 2, 3),
                Arrays.stream(engine.getConstructors()).map(Constructor::getParameterCount).sorted().toList(),
                "the three original constructors move intact");

        TimeSource fixedClock = () -> 1_767_225_601_234L;
        Object isolated = newEngine(engine, 42L, TOLERANCE, fixedClock);
        long first = invokeLong(isolated, "nextLongId");
        long second = invokeLong(isolated, "nextLongId");

        assertTrue(second > first, "one engine stays strictly increasing");
        assertEquals(42, machineIdOf(first), "the 10-bit machine field is unchanged");
        assertEquals(0, sequenceOf(first), "the 12-bit sequence field is unchanged");
        assertEquals(1, sequenceOf(second), "the 12-bit sequence advances by one");

        // Each operation consumes the state machine, so the decimal relationship is taken from a
        // fresh engine with the identical fixed clock, which reproduces the first ID exactly.
        Object mirror = newEngine(engine, 42L, TOLERANCE, fixedClock);
        assertEquals(Long.toString(first), invokeString(mirror, "nextId"),
                "nextId is the decimal representation of the same algorithm result");
    }

    @Test
    void uninitializedStaticEntryRejectsWithACommonConfigurationFailure() {
        ScenarioResult result = runScenario("uninitialized");

        assertEquals(0, result.exitCode(), result.output());
        assertTrue(result.output().contains("UNINITIALIZED-REJECTED"), result.output());
    }

    @Test
    void sameConfigurationRebindsTheAlreadyBoundEngine() {
        ScenarioResult result = runScenario("reuse");

        assertEquals(0, result.exitCode(), result.output());
        assertTrue(result.output().contains("REUSE-OK"), result.output());
    }

    @Test
    void differentConfigurationIsRejectedAndKeepsTheLiveEngine() {
        ScenarioResult result = runScenario("conflict");

        assertEquals(0, result.exitCode(), result.output());
        assertTrue(result.output().contains("CONFLICT-OK"), result.output());
    }

    @Test
    void concurrentInitializationBindsExactlyOneEngineWithoutDuplicateIds() {
        ScenarioResult result = runScenario("concurrent");

        assertEquals(0, result.exitCode(), result.output());
        assertTrue(result.output().contains("CONCURRENT-OK"), result.output());
    }

    @ParameterizedTest
    @ValueSource(strings = {"boundary-zero", "boundary-max"})
    void assignedMachineIdBoundariesBind(String scenario) {
        ScenarioResult result = runScenario(scenario);

        assertEquals(0, result.exitCode(), result.output());
        assertTrue(result.output().contains("BOUNDARY-OK"), result.output());
    }

    private static ScenarioResult runScenario(String name) {
        String javaBinary = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        ProcessBuilder builder = new ProcessBuilder(javaBinary, "-cp",
                System.getProperty("java.class.path"), Scenario.class.getName(), name)
                .redirectErrorStream(true);
        Process process = null;
        try {
            process = builder.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            }
            assertTrue(process.waitFor(110, TimeUnit.SECONDS), "scenario " + name + " did not finish");
            return new ScenarioResult(process.exitValue(), output.toString());
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            throw new AssertionError("cannot run isolated scenario " + name, exception);
        } finally {
            if (process != null) {
                process.destroyForcibly();
            }
        }
    }

    private static Method find(List<Method> methods, String name) {
        return methods.stream().filter(method -> method.getName().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError(name + " is not declared"));
    }

    private static Object newEngine(Class<?> engine, long machineId, Duration tolerance, TimeSource timeSource)
            throws Throwable {
        Constructor<?> constructor = engine.getConstructor(long.class, Duration.class, TimeSource.class);
        return constructor.newInstance(machineId, tolerance, timeSource);
    }

    private static long invokeLong(Object target, String name) throws Throwable {
        return (long) invoke(target, name);
    }

    private static String invokeString(Object target, String name) throws Throwable {
        return (String) invoke(target, name);
    }

    private static Object invoke(Object target, String name) throws Throwable {
        try {
            return target.getClass().getMethod(name).invoke(target);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }

    private static int machineIdOf(long id) {
        return (int) ((id >>> MACHINE_ID_SHIFT) & 1_023L);
    }

    private static int sequenceOf(long id) {
        return (int) (id & 4_095L);
    }

    private static int timestampOf(long id) {
        return (int) (id >>> TIMESTAMP_SHIFT);
    }

    private record ScenarioResult(int exitCode, String output) {
    }

    /**
     * Entry point for the binding-lifecycle scenarios, each in a fresh JVM so the process-global
     * engine state starts unbound. Failures print the observed state and exit non-zero.
     */
    public static final class Scenario {

        private static final int WARM_UP_IDS = 4_000;

        private Scenario() {
        }

        public static void main(String[] arguments) {
            String name = arguments[0];
            try {
                switch (name) {
                    case "uninitialized" -> uninitialized();
                    case "reuse" -> sameConfigurationReusesEngine();
                    case "conflict" -> differentConfigurationKeepsEngine();
                    case "concurrent" -> concurrentBinding();
                    case "boundary-zero" -> bindsAssignedMachineId(0L);
                    case "boundary-max" -> bindsAssignedMachineId(1_023L);
                    default -> {
                        System.out.println("UNKNOWN-SCENARIO " + name);
                        System.exit(2);
                    }
                }
                System.exit(0);
            } catch (Throwable throwable) {
                System.out.println("SCENARIO-FAILED " + name);
                throwable.printStackTrace(System.out);
                System.exit(1);
            }
        }

        private static void uninitialized() throws Throwable {
            Long id = tryNextLongId();
            if (id != null) {
                throw new AssertionError("an unbound facade must not generate " + id);
            }
            System.out.println("UNINITIALIZED-REJECTED");
        }

        private static void sameConfigurationReusesEngine() throws Throwable {
            initialize(7L, TOLERANCE);
            long last = 0L;
            for (int index = 0; index < WARM_UP_IDS; index++) {
                last = nextLongId();
            }
            if (last <= 0L || machineIdOf(last) != 7) {
                throw new AssertionError("unexpected id " + last);
            }
            initialize(7L, TOLERANCE);
            long after = nextLongId();
            if (after <= last) {
                throw new AssertionError("rebinding the same configuration replaced the live engine: last="
                        + last + ", after=" + after + ", lastTimestamp=" + timestampOf(last)
                        + ", afterTimestamp=" + timestampOf(after));
            }
            if (timestampOf(after) == timestampOf(last) && sequenceOf(after) != sequenceOf(last) + 1) {
                throw new AssertionError("the reused engine did not continue its sequence: last=" + last
                        + ", after=" + after);
            }
            System.out.println("REUSE-OK");
        }

        private static void differentConfigurationKeepsEngine() throws Throwable {
            initialize(7L, TOLERANCE);
            long before = nextLongId();
            Throwable rejection = initializeFailure(8L, TOLERANCE);
            if (!(rejection instanceof CommonException commonException)) {
                throw new AssertionError("a conflicting binding must fail as a common configuration error, got "
                        + rejection);
            }
            long after = nextLongId();
            if (machineIdOf(after) != 7 || after <= before) {
                throw new AssertionError("the rejected rebind disturbed the live engine: before=" + before
                        + ", after=" + after);
            }
            System.out.println("CONFLICT-OK " + commonStatusOf(rejection));
        }

        private static void concurrentBinding() throws Throwable {
            List<Thread> threads = new java.util.ArrayList<>();
            Set<Long> ids = java.util.concurrent.ConcurrentHashMap.newKeySet();
            List<Throwable> failures = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
            var barrier = new java.util.concurrent.CyclicBarrier(16);
            for (int index = 0; index < 16; index++) {
                Thread thread = Thread.ofPlatform().name("id-binding-" + index).start(() -> {
                    try {
                        barrier.await(30, TimeUnit.SECONDS);
                        initialize(9L, TOLERANCE);
                        for (int round = 0; round < 200; round++) {
                            long id = nextLongId();
                            if (machineIdOf(id) != 9 || !ids.add(id)) {
                                throw new AssertionError("duplicate or mis-bound id " + id);
                            }
                        }
                    } catch (Throwable throwable) {
                        failures.add(throwable);
                    }
                });
                threads.add(thread);
            }
            for (Thread thread : threads) {
                thread.join(TimeUnit.SECONDS.toMillis(60));
            }
            if (!failures.isEmpty()) {
                throw new AssertionError("concurrent binding failed: " + failures.getFirst());
            }
            if (ids.size() != 16L * 200L) {
                throw new AssertionError("expected 3200 unique ids but produced " + ids.size());
            }
            System.out.println("CONCURRENT-OK");
        }

        /**
         * A Spring context can only bind one configuration per JVM, so both accepted boundaries are
         * exercised as separate scenarios instead of as repeated contexts.
         */
        private static void bindsAssignedMachineId(long machineId) throws Throwable {
            initialize(machineId, TOLERANCE);
            long id = nextLongId();
            if (machineIdOf(id) != machineId) {
                throw new AssertionError("machineId " + machineId + " was bound as " + id);
            }
            System.out.println("BOUNDARY-OK " + machineId);
        }

        private static Long tryNextLongId() throws Throwable {
            try {
                return nextLongId();
            } catch (CommonException commonException) {
                if (!Modifier.isStatic(entry("nextLongId").getModifiers())) {
                    throw new AssertionError("nextLongId must be a static entry", commonException);
                }
                return null;
            }
        }

        private static String commonStatusOf(Throwable throwable) {
            return ((CommonException) throwable).getStatus();
        }

        private static void initialize(long machineId, Duration tolerance) throws Throwable {
            try {
                Method method = entry("initialize");
                method.invoke(null, method.getParameterTypes()[0] == long.class
                        ? (Object) machineId : Long.valueOf(machineId), tolerance);
            } catch (InvocationTargetException exception) {
                throw exception.getCause();
            }
        }

        private static Throwable initializeFailure(long machineId, Duration tolerance) {
            try {
                initialize(machineId, tolerance);
                return new AssertionError("initialize accepted a conflicting configuration");
            } catch (Throwable throwable) {
                return throwable;
            }
        }

        private static long nextLongId() throws Throwable {
            try {
                return (long) entry("nextLongId").invoke(null);
            } catch (InvocationTargetException exception) {
                throw unwrap(exception);
            }
        }

        private static Throwable unwrap(InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            assertNotNull(cause, "static entry failed without a cause");
            return cause;
        }

        private static Method entry(String name) {
            return Arrays.stream(SnowflakeIdGenerator.class.getDeclaredMethods())
                    .filter(method -> method.getName().equals(name) && Modifier.isStatic(method.getModifiers()))
                    .filter(method -> !name.equals("initialize") || method.getParameterCount() == 2)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("missing static entry " + name
                            + " on SnowflakeIdGenerator: " + Arrays.toString(
                            SnowflakeIdGenerator.class.getDeclaredMethods())));
        }
    }
}
