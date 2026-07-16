package top.egon.cola.component.bytecode.test.agent;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.bytecode.bridge.CallSiteMetadata;
import top.egon.cola.component.bytecode.bridge.DispatcherRegistry;

import java.lang.ref.WeakReference;

import static org.junit.jupiter.api.Assertions.assertNull;

class AgentClassLoaderLeakTest {

    @Test
    void metadataRegistryDoesNotRetainApplicationClassLoader() throws Exception {
        WeakReference<ClassLoader> reference = registerTemporaryLoader();

        for (int attempt = 0; attempt < 40 && reference.get() != null; attempt++) {
            System.gc();
            Thread.sleep(10);
        }

        assertNull(reference.get());
    }

    private WeakReference<ClassLoader> registerTemporaryLoader() {
        ClassLoader loader = new ClassLoader(getClass().getClassLoader()) { };
        DispatcherRegistry.registerCallSite(loader, new CallSiteMetadata(
                1,
                "sample/Temporary",
                "run",
                "()V",
                "java/util/concurrent/Executor",
                "execute",
                "(Ljava/lang/Runnable;)V",
                1
        ));
        return new WeakReference<>(loader);
    }
}
