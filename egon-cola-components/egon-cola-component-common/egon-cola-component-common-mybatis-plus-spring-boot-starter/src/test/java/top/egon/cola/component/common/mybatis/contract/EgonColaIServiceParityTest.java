package top.egon.cola.component.common.mybatis.contract;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EgonColaIServiceParityTest {

    private static final String SERVICE = "top.egon.cola.component.common.mybatis.extension.EgonColaIService";
    private static final String IMPLEMENTATION = "top.egon.cola.component.common.mybatis.extension.EgonColaServiceImpl";

    @Test
    void officialServiceContractIsPublishedWithExactMethodCount() throws Exception {
        Class<?> upstream = Class.forName("com.baomidou.mybatisplus.extension.service.IService");
        Class<?> target = Class.forName(SERVICE);
        Set<MethodKey> expected = Arrays.stream(upstream.getMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers())
                        && !Modifier.isStatic(method.getModifiers()))
                .map(MethodKey::of)
                .collect(Collectors.toSet());

        assertEquals(57, expected.size());
        assertEquals(expected, Arrays.stream(target.getDeclaredMethods())
                .filter(method -> !method.isSynthetic())
                .map(MethodKey::of)
                .collect(Collectors.toSet()));
    }

    @Test
    void implementationDeclaresEveryOfficialMethodAndNoCustomTenantAliases() throws Exception {
        Class<?> upstream = Class.forName("com.baomidou.mybatisplus.extension.service.IService");
        Class<?> implementation = Class.forName(IMPLEMENTATION);
        Set<MethodKey> expected = Arrays.stream(upstream.getMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers())
                        && !Modifier.isStatic(method.getModifiers()))
                .map(MethodKey::of)
                .collect(Collectors.toSet());

        assertEquals(expected, Arrays.stream(implementation.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()) && !method.isSynthetic())
                .map(MethodKey::of)
                .filter(key -> !key.name().equals("main"))
                .collect(Collectors.toSet()));
        assertTrue(Files.exists(Path.of("src/main/java/top/egon/cola/component/common/mybatis/extension")));
        try (var files = Files.walk(Path.of("src/main/java/top/egon/cola/component/common/mybatis"))) {
            assertTrue(files.filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> {
                        try {
                            return Files.readAllLines(path).stream();
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .noneMatch(line -> line.contains("listByCurrentTenantId")
                            || line.contains("countByCurrentTenantId")
                            || line.contains("getByCurrentTenantIdAndId")
                            || line.contains("getOptByCurrentTenantIdAndId")
                            || line.contains("EgonColaSqlInjector")));
        }
    }

    private record MethodKey(String name, String returnType, java.util.List<String> parameters) {

        private static MethodKey of(Method method) {
            return new MethodKey(method.getName(), canonicalType(method.getReturnType()),
                    Arrays.stream(method.getParameterTypes()).map(MethodKey::canonicalType).toList());
        }

        private static String canonicalType(Class<?> type) {
            // IService<T> erases T to Object, while the bounded EgonModel<T>
            // declarations erase it to EgonModel.  Keep the upstream ABI
            // comparison semantic rather than treating the bound as a drift.
            if (type.getName().equals("top.egon.cola.component.common.mybatis.model.EgonModel")) {
                return Object.class.getName();
            }
            if (type.getName().equals("top.egon.cola.component.common.mybatis.extension.EgonColaMapper")) {
                return "com.baomidou.mybatisplus.core.mapper.BaseMapper";
            }
            return type.getName();
        }
    }
}
