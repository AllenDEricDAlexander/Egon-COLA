package top.egon.cola.component.ruleengine.contract;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.enums.ErrorStatus;
import top.egon.cola.component.common.core.enums.EgonEnum;
import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.validation.BaseValidator;
import top.egon.cola.component.ruleengine.result.RuleStatus;
import top.egon.cola.component.ruleengine.tree.NodeType;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Step 6 合同：Rule Engine 的自定义异常、手写 enum、validator 与转换必须落在公共合同上
 * （REQ-002、REQ-003、REQ-004、REQ-005、REQ-006、REQ-007、REQ-027）。
 *
 * <p>本文件只固定本 Step 列出的六条算法差异；结果线格式与既有引擎判定由原回归证明保持。</p>
 */
class ComponentContractGovernanceTest {

    private static final Path MAIN_SOURCES = Path.of("src/main/java");

    private static final String RULEENGINE_PACKAGE = "top.egon.cola.component.ruleengine";

    private static final String EXCEPTION_PACKAGE = RULEENGINE_PACKAGE + ".common.exception";

    private static final List<String> COMPONENT_EXCEPTIONS = List.of(
            "RuleEngineException", "RuleConfigException", "RuleEmptyChainException",
            "RuleEmptyTreeException", "RuleMaxStepsExceededException", "RuleNodeException",
            "RuleRouteException", "RuleTimeoutException");

    /** 算法第 6 行：旧 FQCN 在消费者、反射名称、测试与 README 中必须零残留。 */
    private static final List<String> RELOCATED_TYPES = COMPONENT_EXCEPTIONS.stream()
            .map(name -> RULEENGINE_PACKAGE + ".exception." + name)
            .toList();

    private static final Path[] GOVERNED_TEXT_ROOTS = {
            MAIN_SOURCES,
            Path.of("src/test/java"),
            Path.of("README.md"),
            Path.of("README.zh-CN.md")};

    private static final Pattern PACKAGE = Pattern.compile("^package\\s+([\\w.]+)\\s*;");
    private static final Pattern ENUM_HEADER =
            Pattern.compile("^\\s*(?:[a-z]+\\s+)*enum\\s+(\\w+)([^{]*)\\{");
    private static final Pattern ENUM_CONSTANT =
            Pattern.compile("^\\s*([A-Z][A-Z0-9_]*)\\s*(?:\\(|,|;)");
    private static final Pattern EXPLICIT_CODE = Pattern.compile("\\(\\s*\\d+\\s*,");
    private static final Pattern EXCEPTION_DECLARATION =
            Pattern.compile("\\bclass\\s+(\\w*Exception)\\s+extends\\s+(\\w+)");
    private static final Pattern CLASS_DECLARATION =
            Pattern.compile("\\bclass\\s+(\\w*Validator)\\b\\s+extends\\s+(\\w+)");

    // 算法第 1 行：自定义异常只在所属 common.exception，技术异常根以 CommonException 为公共根。
    @Test
    void customExceptionsAreOwnedByCommonExceptionAndTheNormalizedRoot() throws Exception {
        Map<String, String> declared = declaredExceptions();

        assertThat(declared.keySet())
                .as("REQ-002 组件异常清单")
                .containsExactlyInAnyOrderElementsOf(COMPONENT_EXCEPTIONS);

        List<String> wrongPackage = new ArrayList<>();
        declared.forEach((name, packageName) -> {
            if (!EXCEPTION_PACKAGE.equals(packageName)) {
                wrongPackage.add(name + " -> " + packageName);
            }
        });
        assertThat(wrongPackage).as("REQ-002 异常只在所属 common.exception").isEmpty();

        assertThat(commonException("RuleEngineException").getSuperclass())
                .as("REQ-003 技术异常根必须直接继承 CommonException")
                .isEqualTo(CommonException.class);
        for (String name : COMPONENT_EXCEPTIONS) {
            Class<?> type = commonException(name);
            assertThat(CommonException.class.isAssignableFrom(type))
                    .as("REQ-003 %s 必须以 CommonException 为祖先", name).isTrue();
            assertThat(type.getSuperclass().getSimpleName())
                    .as("REQ-003 %s 不得直接继承 RuntimeException", name)
                    .isIn("CommonException", "BusinessException", "RuleEngineException");
        }
        assertThat(text(MAIN_SOURCES))
                .doesNotContain("extends RuntimeException");
    }

    // 算法第 1/2 行：cause、安全 message、retryable 保持；继承的 getCode 仍是公共 int 合同。
    @Test
    void originalCausesMessagesAndRetryabilitySurviveTheNormalization() throws Exception {
        Class<?> rootType = commonException("RuleEngineException");
        CommonException messageOnly =
                (CommonException) rootType.getConstructor(String.class).newInstance("boom");
        assertThat(messageOnly.getCode())
                .as("组件没有自有整数 code，只能用公共 ResultCode")
                .isEqualTo(ResultCode.SYSTEM_ERROR.getCode());
        assertThat(messageOnly.getStatus()).isEqualTo(ResultCode.SYSTEM_ERROR.getStatus());
        assertThat(messageOnly.getMessage()).isEqualTo("boom");
        assertThat(messageOnly.isRetryable()).as("原异常没有重试声明").isFalse();
        assertThat(rootType.getMethod("getCode").getReturnType())
                .as("REQ-006 int getCode 不得被改成 String/enum")
                .isEqualTo(int.class);

        IllegalStateException cause = new IllegalStateException("node failure");
        for (String name : COMPONENT_EXCEPTIONS) {
            Class<?> type = commonException(name);
            CommonException withCause = (CommonException) type
                    .getConstructor(String.class, Throwable.class).newInstance("context", cause);
            assertThat(withCause.getCause()).as("%s 必须保留原 cause", name).isSameAs(cause);
            assertThat(withCause.getMessage()).as("%s 必须保留原 message", name).isEqualTo("context");
            assertThat(List.of(type.getDeclaredMethods()))
                    .as("%s 不得自带 code/getStatus 影子 getter", name)
                    .noneMatch(method -> method.getName().equals("getCode")
                            || method.getName().equals("code")
                            || method.getName().equals("getStatus"));
        }
    }

    // 算法第 3 行：手写 enum 实现 EgonEnum，已有整数 code 不变，常量为显式字面量且线值不改。
    @Test
    void handwrittenEnumsImplementTheCommonEnumContract() throws Exception {
        List<String> headers = enumHeaders(MAIN_SOURCES);

        assertThat(headers).as("本模块手写 enum 清单").containsExactlyInAnyOrder(
                "RuleStatus implements EgonEnum", "NodeType implements EgonEnum");
        List<String> missingContract = new ArrayList<>();
        for (String header : headers) {
            if (!header.contains(EgonEnum.class.getSimpleName())
                    && !header.contains(ErrorStatus.class.getSimpleName())) {
                missingContract.add(header);
            }
        }
        assertThat(missingContract).as("REQ-004 手写 enum 必须实现 EgonEnum 或 ErrorStatus").isEmpty();
        assertThat(enumConstantsWithoutExplicitCode(MAIN_SOURCES))
                .as("code 必须是显式字面量，不能来自 ordinal()").isEmpty();
        assertThat(text(MAIN_SOURCES)).doesNotContain("ordinal()");

        // 已有整数 code 保持不变；无整数 code 的 NodeType 按当前声明顺序固定 0..4。
        assertThat(RuleStatus.values()).extracting(Enum::name).containsExactly(
                "SUCCESS", "STOPPED", "FAILED", "TIMEOUT", "MAX_STEPS_EXCEEDED", "NO_ROUTE",
                "EMPTY_CHAIN", "EMPTY_TREE", "NODE_ERROR", "CONFIG_ERROR");
        assertThat(codes(RuleStatus.class)).containsExactly(
                0, 600100, 500100, 500101, 500102, 500103, 500104, 500105, 500106, 500107);
        assertThat(invoke(RuleStatus.class, RuleStatus.SUCCESS, "getMessage"))
                .as("原标签保持不变").isEqualTo("success");
        assertThat(invoke(RuleStatus.class, RuleStatus.NODE_ERROR, "getMessage"))
                .isEqualTo("rule node error");
        assertThat(NodeType.values()).extracting(Enum::name)
                .containsExactly("ROOT", "SWITCH", "BIZ", "END", "OTHER");
        assertThat(codes(NodeType.class)).containsExactly(0, 1, 2, 3, 4);
        assertThat(invoke(NodeType.class, NodeType.BIZ, "getMessage"))
                .as("REQ-004 message 使用原常量名").isEqualTo("BIZ");
    }

    // 算法第 4 行：validator 走公共合同；本模块没有自有 validator，因此不得自建校验设施。
    @Test
    void validatorsStayOnTheCommonContractWithoutPrivateInfrastructure() throws Exception {
        for (String type : declaredValidatorTypes()) {
            assertThat(BaseValidator.class.isAssignableFrom(Class.forName(type)))
                    .as("REQ-007 %s 必须继承 BaseValidator", type).isTrue();
        }
        String sources = text(MAIN_SOURCES);
        assertThat(sources).doesNotContain("buildDefaultValidatorFactory");
        assertThat(sources).doesNotContain("javax.validation");
    }

    // 算法第 5 行：转换只服务真实 S/T 语义对；没有转换需求时不得伪造公共 converter。
    @Test
    void conversionsStayOnTheCommonContractWithoutFakeConverters() throws Exception {
        String sources = text(MAIN_SOURCES);

        assertThat(sources).as("Rule 5 禁止 BeanUtils 复制").doesNotContain("BeanUtils");
        assertThat(sources).as("本模块没有自有对象转换需求").doesNotContain("BaseConverter");
        // 引擎只在内存上下文与结果之间流转，失败不产生 DAO/缓存/MQ 副作用。
        assertThat(sources).doesNotContain("JdbcTemplate")
                .doesNotContain("RedissonClient")
                .doesNotContain("KafkaTemplate");
    }

    // 算法第 6 行：消费者、反射名称与 README 同一步更新，旧路径零残留，包声明与目录一致。
    @Test
    void consumersReflectionNamesAndDocsDropTheLegacyLocations() throws Exception {
        for (Path root : GOVERNED_TEXT_ROOTS) {
            for (Path file : sources(root)) {
                // This fixture names the legacy locations to forbid them, so it cannot be under audit.
                if (file.getFileName().toString().equals("ComponentContractGovernanceTest.java")) {
                    continue;
                }
                String content = read(file);
                for (String legacy : RELOCATED_TYPES) {
                    assertThat(content)
                            .as("%s 不应引用已迁出类型 %s", file, legacy)
                            .doesNotContain(legacy);
                }
            }
        }

        assertThat(packagePathMismatches(MAIN_SOURCES))
                .as("REQ-027 包声明必须与目录一致，且不重新引入旧 exception 包")
                .isEmpty();
        String packageInfo = read(MAIN_SOURCES.resolve(
                "top/egon/cola/component/ruleengine/common/exception/package-info.java"));
        assertThat(packageInfo).contains("package " + EXCEPTION_PACKAGE + ";");
    }

    private static Class<?> commonException(String simpleName) {
        String type = EXCEPTION_PACKAGE + "." + simpleName;
        try {
            return Class.forName(type);
        } catch (ClassNotFoundException exception) {
            return fail("REQ-002 " + simpleName + " 必须位于 " + EXCEPTION_PACKAGE);
        }
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

    private static List<String> declaredValidatorTypes() throws IOException {
        List<String> declared = new ArrayList<>();
        for (Path file : sources(MAIN_SOURCES)) {
            String packageName = "";
            for (String line : Files.readAllLines(file)) {
                Matcher packageMatcher = PACKAGE.matcher(line);
                if (packageMatcher.find()) {
                    packageName = packageMatcher.group(1);
                    continue;
                }
                Matcher matcher = CLASS_DECLARATION.matcher(line);
                while (matcher.find()) {
                    declared.add(packageName + "." + matcher.group(1));
                }
            }
        }
        return declared;
    }

    private static List<String> enumHeaders(Path root) throws IOException {
        List<String> headers = new ArrayList<>();
        for (Path file : sources(root)) {
            for (String line : Files.readAllLines(file)) {
                Matcher matcher = ENUM_HEADER.matcher(line);
                if (matcher.find()) {
                    headers.add((matcher.group(1) + " " + matcher.group(2).trim()).trim());
                }
            }
        }
        return headers;
    }

    private static List<String> enumConstantsWithoutExplicitCode(Path root) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path file : sources(root)) {
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

    private static List<String> packagePathMismatches(Path root) throws IOException {
        List<String> mismatches = new ArrayList<>();
        for (Path file : sources(root)) {
            Path relative = root.relativize(file);
            Path directory = relative.getParent();
            String expected = directory == null ? ""
                    : directory.toString().replace('/', '.');
            String actual = "";
            for (String line : Files.readAllLines(file)) {
                Matcher matcher = PACKAGE.matcher(line);
                if (matcher.find()) {
                    actual = matcher.group(1);
                    break;
                }
            }
            if (!expected.equals(actual)) {
                mismatches.add(file + " -> " + actual + " != " + expected);
            }
        }
        return mismatches;
    }

    private static int[] codes(Class<? extends Enum<?>> enumType) {
        Enum<?>[] constants = enumType.getEnumConstants();
        int[] codes = new int[constants.length];
        for (int index = 0; index < constants.length; index++) {
            codes[index] = ((EgonEnum) constants[index]).getCode();
        }
        return codes;
    }

    private static Object invoke(Class<?> type, Object target, String getter) throws Exception {
        Method method = type.getMethod(getter);
        return method.invoke(target);
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
                    .filter(path -> {
                        String name = path.getFileName().toString();
                        return name.endsWith(".java") || name.endsWith(".md");
                    })
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }
    }

    private static String read(Path file) throws IOException {
        assertThat(file).as("缺少被治理文件 %s", file).exists();
        return Files.readString(file);
    }
}
