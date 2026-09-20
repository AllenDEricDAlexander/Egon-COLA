package top.egon.cola.component.dtp.contract;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.enums.EgonEnum;
import top.egon.cola.component.dtp.domain.model.valobj.ExecutorKind;
import top.egon.cola.component.dtp.domain.model.valobj.RegistryEnumVO;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.fail;

/**
 * REQ-004/REQ-005/REQ-006 for the DTP starter: the executor and registry value objects carry the
 * public {@link EgonEnum} code contract while their published Redis key and enum-name wire values
 * stay exactly as before.
 */
class RemainingComponentContractTest {

    private static final Pattern ENUM_HEADER =
            Pattern.compile("^\\s*(?:[a-z]+\\s+)*enum\\s+(\\w+)[^{]*\\{");

    private static final Pattern EXCEPTION_HEADER =
            Pattern.compile("^\\s*(?:public|private|protected|static|final)\\s+class\\s+(\\w*Exception)\\b");

    /** ExecutorKind + RegistryEnumVO; keeps the source scan non-vacuous. */
    private static final int EXPECTED_HANDWRITTEN_ENUMS = 2;

    @Test
    void everyHandwrittenEnumImplementsThePublicEgonEnumContract() {
        for (Class<? extends Enum<?>> enumType : governedEnums()) {
            assertThat(EgonEnum.class.isAssignableFrom(enumType))
                    .as("%s 必须实现公共 EgonEnum 合同", enumType.getSimpleName())
                    .isTrue();
        }
    }

    @Test
    void enumCodesAreFixedLiteralsInsteadOfDynamicOrdinals() {
        assertThat(codeTable(ExecutorKind.class)).containsExactly(
                entry("PLATFORM_THREAD_POOL", 0),
                entry("SPRING_THREAD_POOL_TASK_EXECUTOR", 1),
                entry("VIRTUAL_THREAD_PER_TASK", 2),
                entry("UNKNOWN", 3));
        assertThat(codeTable(RegistryEnumVO.class)).containsExactly(
                entry("THREAD_POOL_CONFIG_LIST_KEY", 0),
                entry("THREAD_POOL_CONFIG_PARAMETER_LIST_KEY", 1),
                entry("DYNAMIC_THREAD_POOL_REDIS_TOPIC", 2));
    }

    @Test
    void messagesReuseExistingLabelsAndFallBackToTheConstantName() {
        Map<String, String> registryMessages = new LinkedHashMap<>();
        for (RegistryEnumVO constant : RegistryEnumVO.values()) {
            registryMessages.put(constant.name(), messageOf(constant));
        }
        assertThat(registryMessages).containsExactly(
                entry("THREAD_POOL_CONFIG_LIST_KEY", "池化配置列表"),
                entry("THREAD_POOL_CONFIG_PARAMETER_LIST_KEY", "池化配置参数"),
                entry("DYNAMIC_THREAD_POOL_REDIS_TOPIC", "动态线程池监听主题配置"));

        Map<String, String> executorMessages = new LinkedHashMap<>();
        for (ExecutorKind constant : ExecutorKind.values()) {
            executorMessages.put(constant.name(), messageOf(constant));
        }
        assertThat(executorMessages).containsOnly(
                entry("PLATFORM_THREAD_POOL", "PLATFORM_THREAD_POOL"),
                entry("SPRING_THREAD_POOL_TASK_EXECUTOR", "SPRING_THREAD_POOL_TASK_EXECUTOR"),
                entry("VIRTUAL_THREAD_PER_TASK", "VIRTUAL_THREAD_PER_TASK"),
                entry("UNKNOWN", "UNKNOWN"));
    }

    @Test
    void redisKeysAndEnumWireNamesStayUnchanged() {
        assertThat(Stream.of(ExecutorKind.values()).map(Enum::name).toList())
                .containsExactly("PLATFORM_THREAD_POOL", "SPRING_THREAD_POOL_TASK_EXECUTOR",
                        "VIRTUAL_THREAD_PER_TASK", "UNKNOWN");

        Map<String, String> registryKeys = new LinkedHashMap<>();
        for (RegistryEnumVO constant : RegistryEnumVO.values()) {
            registryKeys.put(constant.name(), constant.getKey());
        }
        assertThat(registryKeys).containsExactly(
                entry("THREAD_POOL_CONFIG_LIST_KEY", "THREAD_POOL_CONFIG_LIST_KEY"),
                entry("THREAD_POOL_CONFIG_PARAMETER_LIST_KEY", "THREAD_POOL_CONFIG_PARAMETER_LIST_KEY"),
                entry("DYNAMIC_THREAD_POOL_REDIS_TOPIC", "DYNAMIC_THREAD_POOL_REDIS_TOPIC"));
        assertThat(RegistryEnumVO.DYNAMIC_THREAD_POOL_REDIS_TOPIC.getDesc())
                .isEqualTo("动态线程池监听主题配置");
    }

    @Test
    void handwrittenEnumsAreAllGovernedAndNoLocalExceptionHierarchyExists() throws IOException {
        List<String> enums = new ArrayList<>();
        List<String> exceptions = new ArrayList<>();
        try (Stream<Path> sources = mainSources()) {
            for (Path source : (Iterable<Path>) sources::iterator) {
                for (String line : Files.readAllLines(source)) {
                    Matcher enumMatcher = ENUM_HEADER.matcher(line);
                    if (enumMatcher.find()) {
                        enums.add(source + ": " + enumMatcher.group(1));
                        continue;
                    }
                    Matcher exceptionMatcher = EXCEPTION_HEADER.matcher(line);
                    if (exceptionMatcher.find()) {
                        exceptions.add(source + ": " + exceptionMatcher.group(1));
                    }
                }
            }
        }

        assertThat(enums).as("本模块手写字面量枚举必须全部纳入公共合同").hasSize(EXPECTED_HANDWRITTEN_ENUMS);
        assertThat(exceptions)
                .as("REQ-002/REQ-003 对本模块不适用：starter 不声明自定义异常")
                .isEmpty();
    }

    private static List<Class<? extends Enum<?>>> governedEnums() {
        List<Class<? extends Enum<?>>> enumTypes = new ArrayList<>();
        enumTypes.add(ExecutorKind.class);
        enumTypes.add(RegistryEnumVO.class);
        return enumTypes;
    }

    private static Map<String, Integer> codeTable(Class<? extends Enum<?>> enumType) {
        Map<String, Integer> codes = new LinkedHashMap<>();
        for (Enum<?> constant : enumType.getEnumConstants()) {
            codes.put(constant.name(), codeOf(constant));
        }
        return codes;
    }

    private static int codeOf(Enum<?> constant) {
        Object code = invokeGetter(constant, "getCode");
        assertThat(code)
                .as("%s.%s 的 getCode() 必须返回固定 int code，而不是旧 String 或 ordinal 派生值",
                        constant.getDeclaringClass().getSimpleName(), constant.name())
                .isInstanceOf(Integer.class);
        return (Integer) code;
    }

    private static String messageOf(Enum<?> constant) {
        return (String) invokeGetter(constant, "getMessage");
    }

    private static Object invokeGetter(Enum<?> constant, String getterName) {
        Class<? extends Enum<?>> enumType = constant.getDeclaringClass();
        Method getter;
        try {
            getter = enumType.getMethod(getterName);
        } catch (NoSuchMethodException exception) {
            return fail("%s 缺少公共 %s() 合同", enumType.getSimpleName(), getterName);
        }
        try {
            return getter.invoke(constant);
        } catch (ReflectiveOperationException exception) {
            return fail("%s.%s 的 %s() 无法调用", enumType.getSimpleName(), constant.name(), getterName);
        }
    }

    private static Stream<Path> mainSources() throws IOException {
        return Files.walk(Path.of("src", "main", "java"))
                .filter(path -> path.toString().endsWith(".java"))
                .sorted();
    }
}
