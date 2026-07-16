package top.egon.cola.component.bytecode.core.enhance.executor;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.CheckClassAdapter;
import top.egon.cola.component.bytecode.bridge.BridgeCapability;
import top.egon.cola.component.bytecode.bridge.BridgeProtocol;
import top.egon.cola.component.bytecode.bridge.BytecodeRuntimeDispatcher;
import top.egon.cola.component.bytecode.bridge.DispatcherRegistry;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutorCallSiteEnhancerTest {

    @Test
    void rewritesOnlyFourApprovedInterfaceCallSitesAndVerifiesFrames() {
        byte[] original = fixtureBytes();
        ClassLoader loader = new ClassLoader(getClass().getClassLoader()) { };

        byte[] transformed = new ExecutorCallSiteEnhancer().enhance(loader, original);

        assertNotNull(transformed);
        InvocationCounts counts = invocationCounts(transformed);
        assertEquals(7, counts.bridgeCalls());
        assertEquals(2, counts.unrelatedExecutorCalls());
        StringWriter verification = new StringWriter();
        CheckClassAdapter.verify(new ClassReader(transformed), loader, false,
                new PrintWriter(verification));
        assertEquals("", verification.toString());
    }

    @Test
    void preservesSingleUnderlyingInvocationAndOriginalFutureIdentity() throws Exception {
        byte[] transformed = new ExecutorCallSiteEnhancer()
                .enhance(new DefiningClassLoader(getClass().getClassLoader()), fixtureBytes());
        DefiningClassLoader loader = new DefiningClassLoader(getClass().getClassLoader());
        transformed = new ExecutorCallSiteEnhancer().enhance(loader, fixtureBytes());
        Class<?> fixtureClass = loader.define(transformed);
        RecordingExecutor executor = new RecordingExecutor();
        AtomicInteger decoratedRuns = new AtomicInteger();
        var registration = DispatcherRegistry.register(loader, "test", new Dispatcher() {
            @Override
            public Runnable decorateRunnable(
                    Class<?> callerClass, Executor target, Runnable task, long callSiteId) {
                return () -> {
                    decoratedRuns.incrementAndGet();
                    task.run();
                };
            }
        });
        try {
            Future<?> result = (Future<?>) fixtureClass
                    .getMethod("submitRunnable", ExecutorService.class, Runnable.class)
                    .invoke(null, executor, (Runnable) () -> { });

            assertSame(executor.future, result);
            assertEquals(1, executor.submissions.get());
            executor.submitted.run();
            assertEquals(1, decoratedRuns.get());
        } finally {
            registration.close();
        }
    }

    @Test
    void isStableAcrossRunsPreventsDuplicateEnhancementAndDetectsIdCollisions() {
        byte[] original = fixtureBytes();
        DefiningClassLoader firstLoader = new DefiningClassLoader(getClass().getClassLoader());
        DefiningClassLoader secondLoader = new DefiningClassLoader(getClass().getClassLoader());
        ExecutorCallSiteEnhancer enhancer = new ExecutorCallSiteEnhancer();

        byte[] first = enhancer.enhance(firstLoader, original);
        byte[] second = enhancer.enhance(secondLoader, original);

        assertEquals(ids(first), ids(second));
        assertNull(enhancer.enhance(firstLoader, first));

        ExecutorCallSiteEnhancer colliding = new ExecutorCallSiteEnhancer(
                input -> (long) java.util.Objects.hash(
                        input.enclosingMethodName(),
                        input.enclosingMethodDescriptor(),
                        input.instructionOrdinal()));
        ClassLoader collisionLoader = new ClassLoader(getClass().getClassLoader()) { };
        colliding.enhance(collisionLoader, original);
        assertThrows(IllegalStateException.class,
                () -> colliding.enhance(collisionLoader, renamedFixtureBytes("CollisionFixture")));
    }

    private byte[] fixtureBytes() {
        String resource = "/" + Fixture.class.getName().replace('.', '/') + ".class";
        try (var stream = getClass().getResourceAsStream(resource)) {
            assertNotNull(stream);
            return stream.readAllBytes();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private byte[] renamedFixtureBytes(String simpleName) {
        String replacement = getClass().getPackageName().replace('.', '/') + "/" + simpleName;
        ClassReader reader = new ClassReader(fixtureBytes());
        org.objectweb.asm.ClassWriter writer = new org.objectweb.asm.ClassWriter(0);
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public void visit(int version, int access, String name, String signature,
                              String superName, String[] interfaces) {
                super.visit(version, access, replacement, signature, superName, interfaces);
            }
        }, 0);
        return writer.toByteArray();
    }

    private InvocationCounts invocationCounts(byte[] bytes) {
        AtomicInteger bridges = new AtomicInteger();
        AtomicInteger unrelated = new AtomicInteger();
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDescriptor, boolean isInterface) {
                        if (owner.equals("top/egon/cola/component/bytecode/bridge/EgonExecutorBridge")) {
                            bridges.incrementAndGet();
                        } else if (owner.startsWith("java/util/concurrent/")
                                && (methodName.equals("execute") || methodName.equals("submit")
                                || methodName.equals("schedule"))) {
                            unrelated.incrementAndGet();
                        }
                    }
                };
            }
        }, 0);
        return new InvocationCounts(bridges.get(), unrelated.get());
    }

    private java.util.List<Long> ids(byte[] bytes) {
        java.util.List<Long> ids = new java.util.ArrayList<>();
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    Object previous;

                    @Override
                    public void visitLdcInsn(Object value) {
                        previous = value;
                        if (value instanceof Long id) {
                            ids.add(id);
                        }
                    }
                };
            }
        }, 0);
        return ids;
    }

    record InvocationCounts(int bridgeCalls, int unrelatedExecutorCalls) {
    }

    public static final class Fixture {

        public static void execute(Executor executor, Runnable task) {
            executor.execute(task);
        }

        public static Future<?> submitRunnable(ExecutorService executor, Runnable task) {
            return executor.submit(task);
        }

        public static <T> Future<T> submitResult(
                ExecutorService executor, Runnable task, T result) {
            return executor.submit(task, result);
        }

        public static <T> Future<T> submitCallable(
                ExecutorService executor, Callable<T> task) {
            return executor.submit(task);
        }

        public static Future<String> submitLambda(ExecutorService executor) {
            return executor.submit(() -> "lambda");
        }

        public static Future<?> submitInsideTry(ExecutorService executor, Runnable task) {
            try {
                return executor.submit(task);
            } catch (RuntimeException exception) {
                throw exception;
            }
        }

        public static Future<Integer> submitVirtual() {
            try (ExecutorService executor = java.util.concurrent.Executors
                    .newVirtualThreadPerTaskExecutor()) {
                return executor.submit(() -> 1);
            }
        }

        public static Future<?> concrete(ThreadPoolExecutor executor, Runnable task) {
            return executor.submit(task);
        }

        public static Future<String> unrelated(
                ScheduledExecutorService executor,
                Callable<String> task
        ) {
            return executor.schedule(task, 1, TimeUnit.MILLISECONDS);
        }
    }

    static class DefiningClassLoader extends ClassLoader {

        DefiningClassLoader(ClassLoader parent) {
            super(parent);
        }

        Class<?> define(byte[] bytes) {
            return defineClass(null, bytes, 0, bytes.length);
        }
    }

    static final class RecordingExecutor extends AbstractExecutorService {

        private final AtomicInteger submissions = new AtomicInteger();
        private final FutureTask<Void> future = new FutureTask<>(() -> null);
        private Runnable submitted;

        @Override
        public Future<?> submit(Runnable task) {
            submissions.incrementAndGet();
            submitted = task;
            return future;
        }

        @Override
        public void shutdown() {
        }

        @Override
        public java.util.List<Runnable> shutdownNow() {
            return java.util.List.of();
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return false;
        }

        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }

    static class Dispatcher implements BytecodeRuntimeDispatcher {

        @Override
        public int protocolMajor() {
            return BridgeProtocol.MAJOR;
        }

        @Override
        public int protocolMinor() {
            return BridgeProtocol.MINOR;
        }

        @Override
        public Set<BridgeCapability> capabilities() {
            return Set.of(BridgeCapability.EXECUTOR);
        }

        @Override
        public Runnable decorateRunnable(
                Class<?> callerClass, Executor executor, Runnable task, long callSiteId) {
            return task;
        }

        @Override
        public <V> Callable<V> decorateCallable(
                Class<?> callerClass, Executor executor, Callable<V> task, long callSiteId) {
            return task;
        }
    }
}
