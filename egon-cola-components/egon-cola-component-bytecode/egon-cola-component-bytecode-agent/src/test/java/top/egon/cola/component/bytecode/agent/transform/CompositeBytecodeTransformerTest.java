package top.egon.cola.component.bytecode.agent.transform;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import top.egon.cola.component.bytecode.agent.AgentConfiguration;
import top.egon.cola.component.bytecode.agent.AgentConfigurationLoader;
import top.egon.cola.component.bytecode.agent.AgentStateStore;
import top.egon.cola.component.bytecode.agent.AgentState;
import top.egon.cola.component.bytecode.agent.ClassNameFilter;
import top.egon.cola.component.bytecode.core.enhance.executor.ExecutorCallSiteEnhancer;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CompositeBytecodeTransformerTest {

    @Test
    void filtersBeforeDelegatingAndReturnsExecutorEnhancement() throws Exception {
        AgentConfiguration configuration = new AgentConfigurationLoaderForTest().load();
        AgentStateStore stateStore = new AgentStateStore("test", 4);
        stateStore.start(configuration);
        stateStore.active();
        ExecutorCallSiteEnhancer enhancer = new ExecutorCallSiteEnhancer();
        AtomicInteger calls = new AtomicInteger();
        CompositeBytecodeTransformer transformer = new CompositeBytecodeTransformer(
                new ClassNameFilter(configuration),
                configuration,
                stateStore,
                (loader, name, bytes) -> {
                    calls.incrementAndGet();
                    return enhancer.enhance(loader, bytes);
                }
        );
        ClassLoader loader = new ClassLoader(getClass().getClassLoader()) { };

        byte[] transformed = transformer.transform(
                null, loader, "application/ExecutorCaller", null, null, fixture());

        assertNotNull(transformed);
        assertEquals(1, calls.get());
        assertEquals(1, bridgeCallCount(transformed));
    }

    @Test
    void marksFatalButLeavesClassLoadingToTheJvm() throws Exception {
        AgentConfiguration configuration = new AgentConfigurationLoader().load(
                "enabled=true,features=access-guard,include=application.*," +
                        "failure-policy=mark-fatal");
        AgentStateStore stateStore = new AgentStateStore("test", 4);
        stateStore.start(configuration);
        stateStore.active();
        CompositeBytecodeTransformer transformer = new CompositeBytecodeTransformer(
                new ClassNameFilter(configuration),
                configuration,
                stateStore,
                (loader, name, bytes) -> {
                    throw new IllegalArgumentException("unsupported-target");
                }
        );

        assertNull(transformer.transform(
                null, getClass().getClassLoader(), "application/FatalTarget",
                null, null, fixture()));
        assertEquals(AgentState.FAILED, stateStore.state());
    }

    private byte[] fixture() {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "application/ExecutorCaller",
                null, "java/lang/Object", null);
        MethodVisitor method = writer.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "execute",
                "(Ljava/util/concurrent/Executor;Ljava/lang/Runnable;)V",
                null,
                null
        );
        method.visitCode();
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitVarInsn(Opcodes.ALOAD, 1);
        method.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "java/util/concurrent/Executor",
                "execute",
                "(Ljava/lang/Runnable;)V",
                true
        );
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private int bridgeCallCount(byte[] bytes) {
        AtomicInteger count = new AtomicInteger();
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName,
                                                String methodDescriptor, boolean isInterface) {
                        if (owner.endsWith("/EgonExecutorBridge")) {
                            count.incrementAndGet();
                        }
                    }
                };
            }
        }, 0);
        return count.get();
    }

    private static final class AgentConfigurationLoaderForTest {
        AgentConfiguration load() {
            return new AgentConfigurationLoader().load(
                    "enabled=true,features=executor,include=application.*");
        }
    }
}
