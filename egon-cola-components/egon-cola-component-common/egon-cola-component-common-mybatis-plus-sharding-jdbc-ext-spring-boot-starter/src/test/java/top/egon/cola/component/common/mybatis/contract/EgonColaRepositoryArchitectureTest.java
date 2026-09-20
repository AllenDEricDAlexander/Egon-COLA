package top.egon.cola.component.common.mybatis.contract;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EgonColaRepositoryArchitectureTest {

    private static final String SERVICE = "top.egon.cola.component.common.mybatis.extension.EgonColaIRepository";
    private static final String IMPLEMENTATION = "top.egon.cola.component.common.mybatis.extension.EgonColaRepository";

    @Test
    void officialServiceContractIsPublishedWithExactMethodCount() throws Exception {
        Class<?> upstream = Class.forName("com.baomidou.mybatisplus.extension.service.IService");
        Class<?> target = Class.forName(SERVICE);
        Set<MethodKeyBO> expected = Arrays.stream(upstream.getMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers())
                        && !Modifier.isStatic(method.getModifiers()))
                .map(MethodKeyBO::of)
                .collect(Collectors.toSet());

        assertEquals(57, expected.size());
        assertEquals(expected, Arrays.stream(target.getDeclaredMethods())
                .filter(method -> !method.isSynthetic())
                .map(MethodKeyBO::of)
                .collect(Collectors.toSet()));
    }

    @Test
    void implementationDeclaresEveryOfficialMethodAndNoCustomTenantAliases() throws Exception {
        Class<?> upstream = Class.forName("com.baomidou.mybatisplus.extension.service.IService");
        Class<?> implementation = Class.forName(IMPLEMENTATION);
        Set<MethodKeyBO> expected = Arrays.stream(upstream.getMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers())
                        && !Modifier.isStatic(method.getModifiers()))
                .map(MethodKeyBO::of)
                .collect(Collectors.toSet());
        assertEquals(expected, Arrays.stream(implementation.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()) && !method.isSynthetic())
                .map(MethodKeyBO::of)
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

    @Test
    void cacheImplementationTypesStayInvisibleToPersistenceAndCore() throws Exception {
        List<Path> roots = new ArrayList<>(List.of(
                Path.of("src/main/java"), Path.of("src/test/java")));
        Path coreSources = Path.of("../egon-cola-component-common-core/src/main/java");
        if (Files.isDirectory(coreSources)) {
            roots.add(coreSources);
        }
        List<String> violations = new ArrayList<>();
        for (Path root : roots) {
            try (var files = Files.walk(root)) {
                files.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                    // REQ-019 让必需缓存的装配在 autoconfigure 边界可见；持久化与核心仍然 blindness。
                    if (root.toString().startsWith("src/main") && path.toString().contains("/autoconfigure/")) {
                        return;
                    }
                    try {
                        for (String line : Files.readAllLines(path)) {
                            if (line.startsWith("import top.egon.cola.component.common.cache.")) {
                                violations.add(path + " -> " + line.trim());
                            }
                        }
                    } catch (Exception exception) {
                        throw new IllegalStateException(exception);
                    }
                });
            }
        }
        Path port = coreSources.resolve("top/egon/cola/component/common/core/cache/EgonColaCachePort.java");
        if (Files.exists(port)) {
            for (String line : Files.readAllLines(port)) {
                if (line.startsWith("import ") && !line.startsWith("import java.")) {
                    violations.add(port + " -> non-JDK import: " + line.trim());
                }
            }
        }
        assertTrue(violations.isEmpty(), () -> "CACHE_LAYERING_VIOLATION: " + violations);
    }

    @Test
    void persistenceModelHasNoActiveRecordAndRepositoryHasNoCrudFieldInjection() throws Exception {
        assertEquals(Object.class, top.egon.cola.component.common.mybatis.model.EgonModel.class.getSuperclass());
        Class<?> implementation = Class.forName(IMPLEMENTATION);
        assertEquals(com.baomidou.mybatisplus.extension.repository.CrudRepository.class, implementation.getSuperclass());
        Class<?> target = Class.forName(SERVICE);
        assertTrue(Arrays.asList(target.getInterfaces()).contains(com.baomidou.mybatisplus.extension.repository.IRepository.class));
        assertTrue(Arrays.stream(implementation.getDeclaredFields())
                .noneMatch(field -> field.isAnnotationPresent(org.springframework.beans.factory.annotation.Autowired.class)));
        Class<?> model = top.egon.cola.component.common.mybatis.model.EgonModel.class;
        assertEquals(java.time.LocalDateTime.class, model.getDeclaredField("deletedAt").getType());
        assertEquals(Long.class, model.getDeclaredField("version").getType());
        assertEquals(com.baomidou.mybatisplus.annotation.IdType.ASSIGN_ID,
                model.getDeclaredField("id").getAnnotation(com.baomidou.mybatisplus.annotation.TableId.class).type());
        assertEquals("null", model.getDeclaredField("deletedAt").getAnnotation(com.baomidou.mybatisplus.annotation.TableLogic.class).value());
        assertTrue(model.getDeclaredField("version").isAnnotationPresent(com.baomidou.mybatisplus.annotation.Version.class));
    }

    private record MethodKeyBO(String name, String returnType, java.util.List<String> parameters) {

        private static MethodKeyBO of(Method method) {
            return new MethodKeyBO(method.getName(), canonicalType(method.getReturnType()),
                    Arrays.stream(method.getParameterTypes()).map(MethodKeyBO::canonicalType).toList());
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
