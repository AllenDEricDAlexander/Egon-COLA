package top.egon.cola.component.bytecode.agent;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.bytecode.agent.transform.CompositeBytecodeTransformer;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassNameFilterTest {

    private final ClassLoader applicationLoader = new ClassLoader() { };

    @Test
    void matchesExplicitIncludesAndAppliesUserExcludes() {
        ClassNameFilter filter = filter("application.*,company.service.*", "application.generated.*");

        assertTrue(filter.matches(applicationLoader, "application/OrderService", classBytes(65)));
        assertTrue(filter.matches(applicationLoader, "company/service/InvoiceService", classBytes(69)));
        assertFalse(filter.matches(applicationLoader, "application/generated/OrderMapper", classBytes(65)));
        assertFalse(filter.matches(applicationLoader, "other/OrderService", classBytes(65)));
    }

    @Test
    void hardExclusionsCannotBeOverriddenByIncludes() {
        ClassNameFilter filter = filter("*", "");

        assertFalse(filter.matches(applicationLoader, "java/lang/String", classBytes(65)));
        assertFalse(filter.matches(applicationLoader, "javax/sql/DataSource", classBytes(65)));
        assertFalse(filter.matches(applicationLoader, "jakarta/annotation/Resource", classBytes(65)));
        assertFalse(filter.matches(applicationLoader, "jdk/internal/Misc", classBytes(65)));
        assertFalse(filter.matches(applicationLoader, "sun/misc/Unsafe", classBytes(65)));
        assertFalse(filter.matches(applicationLoader, "com/sun/proxy/Proxy", classBytes(65)));
        assertFalse(filter.matches(applicationLoader, "org/objectweb/asm/ClassReader", classBytes(65)));
        assertFalse(filter.matches(applicationLoader, "org/springframework/context/ApplicationContext", classBytes(65)));
        assertFalse(filter.matches(applicationLoader, "org/slf4j/Logger", classBytes(65)));
        assertFalse(filter.matches(applicationLoader, "ch/qos/logback/classic/Logger", classBytes(65)));
        assertFalse(filter.matches(applicationLoader, "io/micrometer/core/instrument/MeterRegistry", classBytes(65)));
        assertFalse(filter.matches(applicationLoader,
                "top/egon/cola/component/bytecode/agent/BytecodeAgent", classBytes(65)));
    }

    @Test
    void rejectsBootstrapAndUnsupportedOrMalformedClasses() {
        ClassNameFilter filter = filter("application.*", "");

        assertFalse(filter.matches(null, "application/OrderService", classBytes(65)));
        assertFalse(filter.matches(applicationLoader, "application/OrderService", classBytes(64)));
        assertFalse(filter.matches(applicationLoader, "application/OrderService", classBytes(70)));
        assertFalse(filter.matches(applicationLoader, "application/OrderService", new byte[4]));
    }

    @Test
    void rejectsUnmatchedClassBeforeTransformationOperationConstructsAnAsmReader() throws Exception {
        AgentConfiguration configuration = new AgentConfigurationLoader(new Properties(), Map.of())
                .load("enabled=true,include=application.*");
        AgentStateStore stateStore = new AgentStateStore("test", 2);
        stateStore.start(configuration);
        stateStore.active();
        AtomicInteger operations = new AtomicInteger();
        CompositeBytecodeTransformer transformer = new CompositeBytecodeTransformer(
                new ClassNameFilter(configuration),
                configuration,
                stateStore,
                (loader, className, bytes) -> {
                    operations.incrementAndGet();
                    throw new AssertionError("ClassReader must not be created");
                }
        );

        transformer.transform(null, applicationLoader, "other/OrderService",
                null, null, classBytes(65));

        assertEquals(0, operations.get());
    }

    private ClassNameFilter filter(String includes, String excludes) {
        String arguments = "enabled=true,include=" + includes;
        if (!excludes.isBlank()) {
            arguments += ",exclude=" + excludes;
        }
        return new ClassNameFilter(new AgentConfigurationLoader(new Properties(), Map.of()).load(arguments));
    }

    private byte[] classBytes(int majorVersion) {
        return new byte[]{
                (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE,
                0, 0, (byte) (majorVersion >>> 8), (byte) majorVersion
        };
    }
}
