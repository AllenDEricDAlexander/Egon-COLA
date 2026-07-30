package top.egon.cola.component.gateway.contract.rule;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Guards against a static-initialisation cycle between {@link ServiceCallPolicy} and its nested
 * policy records.
 *
 * <p>The records hold {@code DEFAULTS} constants, and the outer record holds a {@code DEFAULTS}
 * built from them. If a nested record's initialiser reaches back into the outer class — as it did
 * when they shared a private static helper there — then initialising the nested record first makes
 * the JVM start the outer initialiser, which re-enters the nested one. Re-entrant initialisation
 * does not block: it returns immediately with the nested {@code DEFAULTS} still null, so the outer
 * {@code DEFAULTS} is permanently cached with null components.
 *
 * <p>The failure is invisible in ordinary test runs because it depends on which class the JVM
 * touches first, so this test forces the fatal order in a private class loader.
 */
class ServiceCallPolicyInitialisationOrderTest {

    private static final String OUTER = ServiceCallPolicy.class.getName();
    private static final String NESTED = OUTER + "$RetryPolicy";

    @Test
    @DisplayName("outer defaults are fully populated even when a nested record initialises first")
    void nestedRecordInitialisingFirstDoesNotNullOuterDefaults() throws Exception {
        try (IsolatedLoader loader = new IsolatedLoader()) {
            // Force the order that used to break: the nested record before the outer record.
            Class.forName(NESTED, true, loader);

            Class<?> outer = Class.forName(OUTER, true, loader);
            Object defaults = outer.getMethod("defaults").invoke(null);

            for (String component : new String[]{"retry", "loadBalance", "circuitBreaker", "cache"}) {
                Method accessor = outer.getMethod(component);
                assertNotNull(accessor.invoke(defaults),
                        "ServiceCallPolicy.defaults()." + component + "() was null, which means the "
                                + "nested record's DEFAULTS was read during its own initialisation");
            }
        }
    }

    /**
     * Loads the policy classes itself rather than delegating, so each test run gets fresh
     * initialisation state. Everything else delegates to the parent as usual.
     */
    private static final class IsolatedLoader extends ClassLoader implements AutoCloseable {

        private IsolatedLoader() {
            super(ServiceCallPolicyInitialisationOrderTest.class.getClassLoader());
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.startsWith(OUTER) && !name.equals(LoadBalanceStrategy.class.getName())
                    && !name.equals(PolicyDurations.class.getName())) {
                return super.loadClass(name, resolve);
            }
            Class<?> loaded = findLoadedClass(name);
            if (loaded == null) {
                loaded = defineFromParentResource(name);
            }
            if (resolve) {
                resolveClass(loaded);
            }
            return loaded;
        }

        private Class<?> defineFromParentResource(String name) throws ClassNotFoundException {
            String resource = name.replace('.', '/') + ".class";
            try (InputStream stream = getParent().getResourceAsStream(resource)) {
                if (stream == null) {
                    throw new ClassNotFoundException(name);
                }
                byte[] bytes = stream.readAllBytes();
                return defineClass(name, bytes, 0, bytes.length);
            } catch (IOException ex) {
                throw new ClassNotFoundException(name, ex);
            }
        }

        @Override
        public void close() {
            // Nothing to release; present so the test reads as a scoped resource.
        }
    }
}
