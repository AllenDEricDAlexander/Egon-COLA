package top.egon.cola.component.common.mybatis.contract;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.enums.EgonEnum;
import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlResult;
import top.egon.cola.component.common.mybatis.ddl.EgonColaDdlTargetBO;
import top.egon.cola.component.common.mybatis.exception.EgonColaMybatisPlusConfigurationException;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.component.common.mybatis.model.EgonColaModelValidationGroups;
import top.egon.cola.component.common.mybatis.routing.EgonColaRouteQuery;
import top.egon.cola.component.common.mybatis.routing.EgonColaRoutingProfileBO;
import top.egon.cola.component.common.mybatis.sharding.EgonColaShardingProperties;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * 修复合同：本模块的九个手写 enum 必须落在 {@link EgonEnum} 上，配置异常必须以
 * {@link CommonException} 为公共根（REQ-003、REQ-004）。
 *
 * <p>线值、{@code @ConfigurationProperties} 绑定名与既有失败码文本必须由原回归继续证明保持。</p>
 */
class EgonColaContractGovernanceTest {

    private static final Path MAIN_SOURCES = Path.of("src/main/java");

    private static final String MYBATIS_PACKAGE = "top.egon.cola.component.common.mybatis";

    private static final String EXCEPTION_PACKAGE = MYBATIS_PACKAGE + ".exception";

    private static final List<Class<?>> GOVERNED_ENUMS = List.of(
            EgonColaDdlResult.StatusEnum.class,
            EgonColaDdlTargetBO.RoleEnum.class,
            EgonColaRouteQuery.OperationEnum.class,
            EgonColaRoutingProfileBO.TableKindEnum.class,
            EgonColaShardingProperties.ModeEnum.class,
            EgonColaShardingProperties.ConfigStyleEnum.class,
            EgonColaShardingProperties.DataSourceRoleEnum.class,
            EgonColaModelValidationGroups.Operation.class,
            BatchOperation.TYPE);

    private static final Map<Class<?>, List<String>> DECLARATION_ORDER = ordered(
            entry(EgonColaDdlResult.StatusEnum.class, "APPLIED", "SKIPPED"),
            entry(EgonColaDdlTargetBO.RoleEnum.class, "MASTER_DATA", "SHARD"),
            entry(EgonColaRouteQuery.OperationEnum.class, "QUERY", "COMMAND", "ROUTE_CANDIDATES"),
            entry(EgonColaRoutingProfileBO.TableKindEnum.class,
                    "SINGLE", "BROADCAST_READ_ONLY", "TENANT_LEGACY", "TENANT_ID_TWO_LEVEL"),
            entry(EgonColaShardingProperties.ModeEnum.class, "SHARDING", "SHARDING_READWRITE"),
            entry(EgonColaShardingProperties.ConfigStyleEnum.class, "STRATEGY", "NATIVE"),
            entry(EgonColaShardingProperties.DataSourceRoleEnum.class, "PRIMARY", "REPLICA"),
            entry(EgonColaModelValidationGroups.Operation.class,
                    "INSERT", "UPDATE", "DELETE", "QUERY", "LOADED"),
            entry(BatchOperation.TYPE, "INSERT", "UPDATE", "UPSERT"));

    private static final Pattern PACKAGE = Pattern.compile("^package\\s+([\\w.]+)\\s*;");

    private static final Pattern ENUM_HEADER =
            Pattern.compile("^\\s*(?:[a-z]+\\s+)*enum\\s+(\\w+)([^{]*)\\{");

    private static final Pattern ENUM_CONSTANT =
            Pattern.compile("^\\s*([A-Z][A-Z0-9_]*)\\s*(?:\\(|,|;)");

    private static final Pattern EXPLICIT_CODE = Pattern.compile("\\(\\s*\\d+\\s*,");

    private static final Pattern EXCEPTION_DECLARATION =
            Pattern.compile("\\bclass\\s+(\\w*Exception)\\s+extends\\s+(\\w+)");

    // REQ-004：每个手写 enum 实现 EgonEnum，code 是显式字面量且不来自声明次序推算。
    @Test
    void everyHandwrittenEnumJoinsTheCommonEnumContract() throws Exception {
        List<String> headers = enumHeaders();

        assertThat(headers).as("本模块手写 enum 只有九个").hasSize(9);
        assertThat(headers).as("REQ-004 手写 enum 必须实现 EgonEnum")
                .allSatisfy(header -> assertThat(header).contains(EgonEnum.class.getSimpleName()));
        assertThat(enumConstantsWithoutExplicitCode())
                .as("code 必须是显式字面量，不能来自 ordinal()").isEmpty();
        assertThat(text(MAIN_SOURCES)).as("禁止用 ordinal() 动态算码").doesNotContain("ordinal()");
    }

    // REQ-004：没有整数 code 的枚举按当前声明次序固定 0..N-1，常量名与线值表示不得改变。
    @Test
    void theGovernedEnumsExposeStableCodesAndKeepTheirNamesAsTheOnlyWireRepresentation() throws Exception {
        for (Class<?> type : GOVERNED_ENUMS) {
            assertThat(EgonEnum.class.isAssignableFrom(type))
                    .as("REQ-004 %s 必须实现 EgonEnum", type.getName()).isTrue();
            List<Object> constants = constants(type);
            assertThat(constants).as("%s 常量清单", type.getSimpleName())
                    .hasSize(DECLARATION_ORDER.get(type).size());
            for (int index = 0; index < constants.size(); index++) {
                Object constant = constants.get(index);
                assertThat(((Enum<?>) constant).name())
                        .as("%s 声明次序与常量名必须保持", type.getSimpleName())
                        .isEqualTo(DECLARATION_ORDER.get(type).get(index));
                assertThat(code(constant))
                        .as("%s.%s 的 code", type.getSimpleName(), constant)
                        .isEqualTo(index);
                assertThat(message(constant))
                        .as("%s.%s 的 message 使用原常量名", type.getSimpleName(), constant)
                        .isEqualTo(((Enum<?>) constant).name());
            }
            assertThat(Arrays.stream(type.getDeclaredFields())
                            .filter(field -> field.isAnnotationPresent(EnumValue.class)
                                    || field.isAnnotationPresent(JsonValue.class)))
                    .as("REQ-004 不得为枚举新增 @EnumValue/@JsonValue 改协议")
                    .isEmpty();
        }
    }

    // 配置绑定与校验分组语义必须原样保留：枚举仍然按名字解析，分组载荷仍然可用。
    @Test
    void configurationBindingAndValidationGroupPayloadsSurviveTheContract() {
        assertThat(EgonColaShardingProperties.ModeEnum.valueOf("SHARDING_READWRITE"))
                .isEqualTo(EgonColaShardingProperties.ModeEnum.SHARDING_READWRITE);
        assertThat(EgonColaShardingProperties.ConfigStyleEnum.valueOf("NATIVE"))
                .isEqualTo(EgonColaShardingProperties.ConfigStyleEnum.NATIVE);
        assertThat(EgonColaShardingProperties.DataSourceRoleEnum.PRIMARY.name()).isEqualTo("PRIMARY");
        assertThat(EgonColaRoutingProfileBO.TableKindEnum.TENANT_ID_TWO_LEVEL.name())
                .isEqualTo("TENANT_ID_TWO_LEVEL");
        assertThat(EgonColaDdlResult.StatusEnum.APPLIED.name()).isEqualTo("APPLIED");
        assertThat(EgonColaDdlTargetBO.RoleEnum.MASTER_DATA.name()).isEqualTo("MASTER_DATA");
        assertThat(EgonColaRouteQuery.OperationEnum.ROUTE_CANDIDATES.name()).isEqualTo("ROUTE_CANDIDATES");
        assertThat(EgonColaModelValidationGroups.Operation.INSERT.group())
                .isEqualTo(EgonColaModelValidationGroups.Insert.class);
        assertThat(EgonColaModelValidationGroups.Operation.UPDATE.group())
                .isEqualTo(EgonColaModelValidationGroups.Update.class);
        assertThat(EgonColaModelValidationGroups.Operation.DELETE.group())
                .isEqualTo(EgonColaModelValidationGroups.Delete.class);
        assertThat(EgonColaModelValidationGroups.Operation.QUERY.group())
                .isEqualTo(EgonColaModelValidationGroups.Query.class);
        assertThat(EgonColaModelValidationGroups.Operation.LOADED.group())
                .isEqualTo(EgonColaModelValidationGroups.Persisted.class);
    }

    // REQ-003：配置异常以 CommonException 为公共根，int getCode 继承公共合同，原 String code 落到 getStatus。
    @Test
    void theConfigurationExceptionIsRootedOnCommonExceptionAndKeepsItsStableCode() throws Exception {
        Map<String, String> declared = declaredExceptions();

        assertThat(declared.keySet()).as("本模块只有一个自定义异常").containsExactly(
                EgonColaMybatisPlusConfigurationException.class.getSimpleName());
        assertThat(declared.get(EgonColaMybatisPlusConfigurationException.class.getSimpleName()))
                .as("REQ-002 异常保持在所属 exception 包").isEqualTo(EXCEPTION_PACKAGE);
        assertThat(CommonException.class.isAssignableFrom(EgonColaMybatisPlusConfigurationException.class))
                .as("REQ-003 必须以 CommonException 为祖先").isTrue();
        assertThat(EgonColaMybatisPlusConfigurationException.class.getSuperclass())
                .as("REQ-003 必须直接继承 CommonException").isEqualTo(CommonException.class);
        assertThat(EgonColaMybatisPlusConfigurationException.class.getMethod("getCode").getReturnType())
                .as("REQ-006 int getCode 不得被改成 String/enum").isEqualTo(int.class);
        assertThat(Arrays.stream(EgonColaMybatisPlusConfigurationException.class.getDeclaredMethods()))
                .as("不得自带 code/getStatus 影子 getter")
                .noneMatch(method -> method.getName().equals("getCode")
                        || method.getName().equals("getStatus"));

        Throwable failure = configure("INVALID_TOPOLOGY");
        assertThat(codeOf(failure)).as("整数 code 必须继承公共 ResultCode")
                .isEqualTo(ResultCode.SYSTEM_ERROR.getCode());
        assertThat(statusOf(failure)).as("原 String code 必须保存在 getStatus")
                .isEqualTo("INVALID_TOPOLOGY");
        assertThat(failure.getMessage()).as("message 文本必须保持").isEqualTo("INVALID_TOPOLOGY");
        assertThat(retryableOf(failure)).as("配置错误没有重试声明").isFalse();
        assertThat(failure.getCause()).as("单参构造不得凭空补 cause").isNull();

        IllegalStateException cause = new IllegalStateException("already bound by a live owner");
        Constructor<EgonColaMybatisPlusConfigurationException> withCause =
                EgonColaMybatisPlusConfigurationException.class
                        .getConstructor(String.class, Throwable.class);
        assertThat(((Throwable) withCause.newInstance("MODEL_VALIDATION_BINDING_CONFLICT", cause)).getCause())
                .as("cause 必须保留").isSameAs(cause);

        assertThatThrownBy(() -> configure("lower_case"))
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .cause().hasMessage("code must be an uppercase identifier");
        assertThatThrownBy(() -> configure(null))
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(NullPointerException.class);
    }

    private static Throwable configure(String code) throws Exception {
        return (Throwable) EgonColaMybatisPlusConfigurationException.class
                .getConstructor(String.class).newInstance(code);
    }

    private static int codeOf(Throwable failure) throws Exception {
        return (int) CommonException.class.getMethod("getCode").invoke(failure);
    }

    private static String statusOf(Throwable failure) throws Exception {
        return (String) CommonException.class.getMethod("getStatus").invoke(failure);
    }

    private static boolean retryableOf(Throwable failure) throws Exception {
        return (boolean) CommonException.class.getMethod("isRetryable").invoke(failure);
    }

    /** {@code EgonColaRepository.BatchOperation} 是私有枚举，只能按反射常量表治理。 */
    private static final class BatchOperation {

        private static final Class<?> TYPE = typeOf(
                EgonColaRepository.class.getName() + "$BatchOperation");

        private static Class<?> typeOf(String name) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException exception) {
                return fail("缺少 " + name);
            }
        }
    }

    private static List<Object> constants(Class<?> type) {
        List<Object> constants = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            if (field.getType() == type) {
                field.setAccessible(true);
                try {
                    constants.add(field.get(null));
                } catch (IllegalAccessException exception) {
                    throw new AssertionError(exception);
                }
            }
        }
        return constants;
    }

    private static int code(Object constant) throws Exception {
        Method getter = EgonEnum.class.getMethod("getCode");
        getter.setAccessible(true);
        return (int) getter.invoke(constant);
    }

    private static String message(Object constant) throws Exception {
        Method getter = EgonEnum.class.getMethod("getMessage");
        getter.setAccessible(true);
        return (String) getter.invoke(constant);
    }

    @SafeVarargs
    private static Map<Class<?>, List<String>> ordered(Map.Entry<Class<?>, List<String>>... entries) {
        Map<Class<?>, List<String>> mapping = new LinkedHashMap<>();
        for (Map.Entry<Class<?>, List<String>> entry : entries) {
            mapping.put(entry.getKey(), entry.getValue());
        }
        return mapping;
    }

    private static Map.Entry<Class<?>, List<String>> entry(Class<?> type, String... names) {
        return Map.entry(type, List.of(names));
    }

    private static Map<String, String> declaredExceptions() throws IOException {
        Map<String, String> declared = new LinkedHashMap<>();
        for (Path file : sources(MAIN_SOURCES)) {
            String packageName = "";
            for (String line : Files.readAllLines(file)) {
                Matcher packageMatcher = PACKAGE.matcher(line);
                if (packageMatcher.find()) {
                    packageName = packageMatcher.group(1);
                    continue;
                }
                Matcher matcher = EXCEPTION_DECLARATION.matcher(line);
                while (matcher.find()) {
                    declared.put(matcher.group(1), packageName);
                }
            }
        }
        return declared;
    }

    private static List<String> enumHeaders() throws IOException {
        List<String> headers = new ArrayList<>();
        for (Path file : sources(MAIN_SOURCES)) {
            for (String line : Files.readAllLines(file)) {
                Matcher matcher = ENUM_HEADER.matcher(line);
                if (matcher.find()) {
                    headers.add((matcher.group(1) + " " + matcher.group(2).trim()).trim());
                }
            }
        }
        return headers;
    }

    private static List<String> enumConstantsWithoutExplicitCode() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path file : sources(MAIN_SOURCES)) {
            List<String> lines = Files.readAllLines(file);
            for (int index = 0; index < lines.size(); index++) {
                Matcher header = ENUM_HEADER.matcher(lines.get(index));
                if (!header.find()) {
                    continue;
                }
                String enumName = header.group(1);
                for (int cursor = index + 1; cursor < lines.size(); cursor++) {
                    String line = lines.get(cursor);
                    if (line.isBlank()) {
                        continue;
                    }
                    Matcher constant = ENUM_CONSTANT.matcher(line);
                    if (!constant.find()) {
                        break;
                    }
                    if (!EXPLICIT_CODE.matcher(line).find()) {
                        violations.add(enumName + "." + constant.group(1) + " -> " + line.trim());
                    }
                    if (line.contains(";")) {
                        break;
                    }
                }
            }
        }
        return violations;
    }

    private static String text(Path root) throws IOException {
        StringBuilder builder = new StringBuilder();
        for (Path file : sources(root)) {
            builder.append(read(file)).append('\n');
        }
        return builder.toString();
    }

    private static List<Path> sources(Path root) throws IOException {
        if (Files.isRegularFile(root)) {
            return List.of(root);
        }
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }
    }

    private static String read(Path file) throws IOException {
        assertThat(file).as("缺少被治理文件 %s", file).exists();
        return Files.readString(file);
    }
}
