package top.egon.cola.component.methodextension.contract;

import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.common.core.enums.EgonEnum;
import top.egon.cola.component.common.core.enums.ErrorStatus;
import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.validation.BaseValidator;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.methodextension.autoconfigure.MethodExtensionAutoConfiguration;
import top.egon.cola.component.methodextension.autoconfigure.MethodExtensionEngine;
import top.egon.cola.component.methodextension.autoconfigure.MethodExtensionNotReadyPolicy;
import top.egon.cola.component.methodextension.autoconfigure.MethodExtensionProperties;
import top.egon.cola.component.methodextension.response.AsyncReturnType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Step 11 合同：Method Extension 的自定义异常、手写 enum 与 validator 必须落在公共合同上
 * （REQ-002、REQ-003、REQ-004、REQ-005、REQ-006、REQ-007、REQ-027）。
 *
 * <p>本文件只固定本 Step 列出的六条算法差异；拦截、事件发布与响应转换行为由原回归证明保持。</p>
 */
class ComponentContractGovernanceTest {

    private static final Path MAIN_SOURCES = Path.of("src/main/java");

    private static final Path TEST_SOURCES = Path.of("src/test/java");

    private static final Path POM = Path.of("pom.xml");

    /** 组件 README 属于聚合模块目录，starter 只负责实现。 */
    private static final Path README = Path.of("../README.md");

    private static final Path BYTECODE = Path.of("../../../egon-cola-component-bytecode");

    private static final Path[] CONSUMER_ROOTS = {
            BYTECODE.resolve("egon-cola-component-bytecode-starter/src/main/java"),
            BYTECODE.resolve("egon-cola-component-bytecode-starter/src/test/java"),
            BYTECODE.resolve("egon-cola-component-bytecode-test/src/test/java")};

    private static final String METHOD_EXTENSION_PACKAGE = "top.egon.cola.component.methodextension";

    private static final String EXCEPTION_PACKAGE = METHOD_EXTENSION_PACKAGE + ".common.exception";

    private static final String LEGACY_EXCEPTION_PACKAGE = METHOD_EXTENSION_PACKAGE + ".exception";

    private static final String CANONICAL_FACADE_BEAN = "egonColaValidationUtils";

    private static final List<String> COMPONENT_EXCEPTIONS = List.of(
            "MethodExtensionException", "MethodExtensionConfigurationException",
            "MethodExtensionResponseException");

    private static final List<String> VALIDATOR_TYPES = List.of(
            METHOD_EXTENSION_PACKAGE + ".autoconfigure.MethodExtensionAgentEngineValidator");

    /** 算法第 6 行：旧 FQCN 在本模块、相邻 bytecode 消费者与 README 中必须零残留。 */
    private static final List<String> RELOCATED_TYPES = COMPONENT_EXCEPTIONS.stream()
            .map(name -> LEGACY_EXCEPTION_PACKAGE + "." + name)
            .toList();

    private static final Path[] GOVERNED_TEXT_ROOTS = {MAIN_SOURCES, TEST_SOURCES, POM};

    /** 本模块 3 个手写 enum 的固定编码：原本没有整数 code，按当前声明顺序落 0..N-1。 */
    private static final Map<String, List<String>> ENUM_CODES = enumCodes();

    /** 3 个 enum 共 3+3+5 个常量，用于证明源码扫描真的命中了声明。 */
    private static final int ENUM_CONSTANT_COUNT = 11;

    private static final Pattern PACKAGE = Pattern.compile("^package\\s+([\\w.]+)\\s*;");
    private static final Pattern ENUM_HEADER =
            Pattern.compile("^\\s*(?:[a-z]+\\s+)*enum\\s+(\\w+)([^{]*)\\{");
    private static final Pattern ENUM_CONSTANT =
            Pattern.compile("^\\s*([A-Z][A-Z0-9_]*)\\s*(?:\\(|,|;)");
    private static final Pattern EXPLICIT_CODE = Pattern.compile("\\(\\s*\\d+\\s*,");
    private static final Pattern EXCEPTION_DECLARATION =
            Pattern.compile("\\bclass\\s+(\\w*Exception)\\s+extends\\s+(\\w+)");
    private static final Pattern VALIDATOR_DECLARATION =
            Pattern.compile("\\b(?:class|interface)\\s+(\\w*Validator)\\b");

    private final List<Object> validatedBeans = new ArrayList<>();

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
        assertThat(wrongPackage)
                .as("REQ-002 异常只在所属 %s", EXCEPTION_PACKAGE)
                .isEmpty();

        Class<?> root = componentException("MethodExtensionException");
        assertThat(root.getSuperclass())
                .as("REQ-003 技术异常根必须直接继承 CommonException")
                .isEqualTo(CommonException.class);
        for (String name : List.of("MethodExtensionConfigurationException", "MethodExtensionResponseException")) {
            assertThat(componentException(name).getSuperclass())
                    .as("REQ-003 %s 继续挂在组件根上", name)
                    .isEqualTo(root);
        }
        assertThat(text(MAIN_SOURCES))
                .as("REQ-003 不得再直接继承 RuntimeException，也不得出现第二套异常根")
                .doesNotContain("extends RuntimeException");

        assertThat(MAIN_SOURCES.resolve(
                "top/egon/cola/component/methodextension/exception"))
                .as("REQ-002 旧 exception 包必须整体迁出")
                .doesNotExist();
    }

    // 算法第 1/2 行：message、cause 与构造器形状保持；继承的 getCode 仍是公共 int 合同。
    @Test
    void messagesCausesAndConstructorShapesSurviveTheNormalization() throws Exception {
        Class<?> rootType = componentException("MethodExtensionException");
        CommonException messageOnly =
                (CommonException) rootType.getConstructor(String.class).newInstance("handler failed");
        assertThat(messageOnly.getCode())
                .as("组件没有自有整数 code，只能用公共 ResultCode")
                .isEqualTo(ResultCode.SYSTEM_ERROR.getCode());
        assertThat(messageOnly.getStatus()).isEqualTo(ResultCode.SYSTEM_ERROR.getStatus());
        assertThat(messageOnly.getMessage()).isEqualTo("handler failed");
        assertThat(messageOnly.isRetryable()).as("原异常没有重试声明").isFalse();
        assertThat(rootType.getMethod("getCode").getReturnType())
                .as("REQ-006 int getCode 不得被改成 String/enum")
                .isEqualTo(int.class);

        IllegalStateException cause = new IllegalStateException("jackson failure");
        for (String name : COMPONENT_EXCEPTIONS) {
            Class<?> type = componentException(name);
            CommonException withoutCause = (CommonException) type
                    .getConstructor(String.class).newInstance("context");
            assertThat(withoutCause.getMessage()).as("%s 必须保留原 message", name).isEqualTo("context");
            assertThat(withoutCause.getCause()).as("%s 单参构造不得凭空补 cause", name).isNull();

            CommonException withCause = (CommonException) type
                    .getConstructor(String.class, Throwable.class).newInstance("context", cause);
            assertThat(withCause.getCause()).as("%s 必须保留原 cause", name).isSameAs(cause);
            assertThat(withCause.getMessage()).as("%s 必须保留原 message", name).isEqualTo("context");

            assertThat(type.getConstructors())
                    .as("%s 构造器形状保持单消息与消息加 cause", name)
                    .hasSize(2);
            assertThat(List.of(type.getDeclaredMethods()))
                    .as("%s 不得自带 code/getStatus 影子 getter", name)
                    .noneMatch(method -> method.getName().equals("getCode")
                            || method.getName().equals("code")
                            || method.getName().equals("getStatus"));
            for (Field field : type.getDeclaredFields()) {
                assertThat(Set.of("code", "status", "retryable"))
                        .as("%s 不得重复声明公共字段", name)
                        .doesNotContain(field.getName());
            }
        }
    }

    // 算法第 3 行：手写 enum 实现 EgonEnum，常量为显式字面量，常量名与判定语义不改。
    @Test
    void handwrittenEnumsImplementTheCommonEnumContract() throws Exception {
        List<String> headers = enumHeaders(MAIN_SOURCES);

        assertThat(headers).as("本模块手写 enum 清单").containsExactlyInAnyOrderElementsOf(
                ENUM_CODES.keySet().stream().map(name -> enumSimpleName(name) + " implements EgonEnum").toList());
        List<String> missingContract = headers.stream()
                .filter(header -> !header.contains(EgonEnum.class.getSimpleName())
                        && !header.contains(ErrorStatus.class.getSimpleName()))
                .toList();
        assertThat(missingContract).as("REQ-004 手写 enum 必须实现 EgonEnum 或 ErrorStatus").isEmpty();
        List<EnumConstantLine> scanned = enumConstantLines(MAIN_SOURCES);
        assertThat(scanned)
                .as("扫描必须真正命中本模块全部 enum 常量，否则该检查为空转")
                .hasSize(ENUM_CONSTANT_COUNT);
        assertThat(constantsWithoutExplicitCode(scanned))
                .as("code 必须是显式字面量，不能来自 ordinal()")
                .isEmpty();
        assertThat(text(MAIN_SOURCES)).doesNotContain("ordinal()");

        for (Map.Entry<String, List<String>> entry : ENUM_CODES.entrySet()) {
            assertThat(codeAssignments(Class.forName(entry.getKey())))
                    .as("REQ-004 %s 的固定编码与顺序保持", simpleName(entry.getKey()))
                    .containsExactlyElementsOf(entry.getValue());
        }

        // 原 name()/判定语义不变：配置绑定、策略分支与异步签名解析都按常量而非 code 工作。
        assertThat(MethodExtensionEngine.valueOf("AGENT")).isSameAs(MethodExtensionEngine.AGENT);
        assertThat(MethodExtensionEngine.values()).containsExactly(
                MethodExtensionEngine.AOP, MethodExtensionEngine.AGENT, MethodExtensionEngine.DISABLED);
        assertThat(MethodExtensionNotReadyPolicy.valueOf("FAIL")).isSameAs(MethodExtensionNotReadyPolicy.FAIL);
        assertThat(new MethodExtensionProperties().getNotReadyPolicy())
                .as("默认策略仍是 PROCEED").isSameAs(MethodExtensionNotReadyPolicy.PROCEED);
        MethodExtensionProperties properties = new MethodExtensionProperties();
        properties.setEngine(null);
        assertThat(properties.effectiveEngine())
                .as("空值归一化行为保持，不改成按 code 判定")
                .isSameAs(MethodExtensionEngine.AOP);

        assertThat(async("plain")).isEqualTo(new AsyncReturnType(AsyncReturnType.Kind.NONE, String.class));
        assertThat(async("plain").supported()).isFalse();
        assertThat(async("completableFuture"))
                .isEqualTo(new AsyncReturnType(AsyncReturnType.Kind.COMPLETABLE_FUTURE, String.class));
        assertThat(async("completionStage").supported()).isTrue();
        assertThat(async("future"))
                .isEqualTo(new AsyncReturnType(AsyncReturnType.Kind.FUTURE, String.class));
        assertThat(async("concreteFuture"))
                .as("具体 Future 子类仍按 UNSUPPORTED_CONCRETE_FUTURE 拒绝")
                .isEqualTo(new AsyncReturnType(
                        AsyncReturnType.Kind.UNSUPPORTED_CONCRETE_FUTURE, Object.class));
        assertThat(async("concreteFuture").supported()).isFalse();
    }

    // 算法第 4 行：validator 继承 BaseValidator，原生约束先于 classpath 关系检查，且只注入具名门面。
    @Test
    void validatorExtendsTheCommonContractAndRunsNativeConstraintsFirst() throws Exception {
        assertThat(declaredValidatorTypes())
                .as("本模块 validator 清单")
                .containsExactlyElementsOf(VALIDATOR_TYPES);
        Class<?> validatorType = Class.forName(VALIDATOR_TYPES.get(0));
        assertThat(BaseValidator.class.isAssignableFrom(validatorType))
                .as("REQ-007 %s 必须继承 BaseValidator", VALIDATOR_TYPES.get(0)).isTrue();
        assertThat(Arrays.stream(validatorType.getConstructors())
                .anyMatch(candidate ->
                        Arrays.asList(candidate.getParameterTypes()).contains(ValidationUtils.class)))
                .as("REQ-007 %s 必须注入具名公共 ValidationUtils", VALIDATOR_TYPES.get(0)).isTrue();

        String validatorSource = read(MAIN_SOURCES.resolve(
                "top/egon/cola/component/methodextension/autoconfigure/"
                        + "MethodExtensionAgentEngineValidator.java"));
        assertThat(validatorSource)
                .as("validator 不得自建校验设施，且必须复用原生约束")
                .doesNotContain("ValidatorFactory")
                .doesNotContain("new ValidationUtils(")
                .contains("validateBean(");
        assertThat(validatorSource)
                .as("输入失败不得触发 DAO/缓存/MQ 副作用")
                .doesNotContain("JdbcTemplate")
                .doesNotContain("RedisTemplate")
                .doesNotContain("RabbitTemplate")
                .doesNotContain("RedissonClient");

        MethodExtensionProperties agentProperties = new MethodExtensionProperties();
        agentProperties.setEngine(MethodExtensionEngine.AGENT);
        assertThatThrownBy(() -> afterPropertiesSet(
                newValidator(agentProperties, recordingValidationUtils())))
                .as("关系检查与安全消息保持")
                .isInstanceOf(CommonException.class)
                .hasMessageContaining("engine=AOP");
        assertThat(validatedBeans)
                .as("REQ-007 原生约束必须先于 classpath 关系检查")
                .containsExactly(agentProperties);

        validatedBeans.clear();
        MethodExtensionProperties aopProperties = new MethodExtensionProperties();
        assertThatCode(() -> afterPropertiesSet(
                newValidator(aopProperties, recordingValidationUtils())))
                .doesNotThrowAnyException();
        assertThat(validatedBeans)
                .as("正常配置也必须先经过原生约束再走关系检查")
                .containsExactly(aopProperties);

        // 独立 starter 没有 MyBatis-Plus 门面可借用，只能按规范名自己发布一次。
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MethodExtensionAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ValidationUtils.class);
                    assertThat(context.getBeanNamesForType(ValidationUtils.class))
                            .as("门面 Bean 不得重复或歧义")
                            .containsExactly(CANONICAL_FACADE_BEAN);
                    assertThat((Object) context.getBean(validatorType))
                            .as("validator 必须被注入已发布的公共门面")
                            .extracting("validationUtils")
                            .isSameAs(context.getBean(CANONICAL_FACADE_BEAN));
                });
    }

    // 算法第 5 行：本模块只有 Jackson 载荷 codec，没有真实 S/T 语义对，不得伪造转换合同。
    @Test
    void conversionsStayOnTheCommonContractWithoutFakeConverters() throws Exception {
        String sources = text(MAIN_SOURCES);

        assertThat(sources).as("Rule 5 禁止 BeanUtils 复制").doesNotContain("BeanUtils");
        assertThat(sources).as("本模块没有自有对象转换语义对").doesNotContain("BaseConverter");
        assertThat(sources).as("不引入新的映射库").doesNotContain("org.mapstruct");
        assertThat(read(MAIN_SOURCES.resolve(
                "top/egon/cola/component/methodextension/response/MethodExtensionResponseResolver.java")))
                .as("REQ-005 响应 codec 仍按原 Jackson 协议转换 returnJson")
                .contains("objectMapper.readerFor(javaType).readValue(returnJson)");
    }

    // 算法第 6 行：消费者、反射名称与 README 同一步更新，旧路径零残留，包声明与目录一致。
    @Test
    void consumersReflectionNamesAndDocsDropTheLegacyLocations() throws Exception {
        List<Path> roots = new ArrayList<>(List.of(GOVERNED_TEXT_ROOTS));
        roots.addAll(List.of(CONSUMER_ROOTS));
        roots.add(README);
        for (Path root : roots) {
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
                .as("REQ-027 包声明必须与目录一致")
                .isEmpty();
        Path packageInfo = MAIN_SOURCES.resolve(
                "top/egon/cola/component/methodextension/common/exception/package-info.java");
        assertThat(packageInfo).as("REQ-027 新公共包必须记录职责").exists();
        assertThat(read(packageInfo)).contains("package " + EXCEPTION_PACKAGE + ";");

        assertThat(read(POM))
                .as("MC-DEP-001 直接使用合同必须显式声明")
                .contains("egon-cola-component-common-core")
                .contains("spring-boot-starter-validation");

        String configuration = read(MAIN_SOURCES.resolve(
                "top/egon/cola/component/methodextension/autoconfigure/MethodExtensionAutoConfiguration.java"));
        assertThat(configuration)
                .as("REQ-007 门面必须按公共规范名条件发布且只包装一次")
                .contains("@Bean(name = \"" + CANONICAL_FACADE_BEAN + "\")")
                .contains("@ConditionalOnMissingBean(name = \"" + CANONICAL_FACADE_BEAN + "\")")
                .contains("ValidationAutoConfiguration.class")
                .contains("@Qualifier(\"" + CANONICAL_FACADE_BEAN + "\")");
        assertThat(countOccurrences(configuration, "new ValidationUtils("))
                .as("门面只包装一次 Validator").isEqualTo(1);

        String readme = read(README);
        assertThat(readme).as("README 只同步本 Step 落地的结构")
                .contains(EXCEPTION_PACKAGE)
                .contains(CANONICAL_FACADE_BEAN)
                .doesNotContain(LEGACY_EXCEPTION_PACKAGE + ".MethodExtension");
    }

    private static Map<String, List<String>> enumCodes() {
        Map<String, List<String>> codes = new LinkedHashMap<>();
        codes.put(METHOD_EXTENSION_PACKAGE + ".autoconfigure.MethodExtensionEngine",
                List.of("AOP=0", "AGENT=1", "DISABLED=2"));
        codes.put(METHOD_EXTENSION_PACKAGE + ".autoconfigure.MethodExtensionNotReadyPolicy",
                List.of("PROCEED=0", "REJECT=1", "FAIL=2"));
        codes.put(METHOD_EXTENSION_PACKAGE + ".response.AsyncReturnType$Kind",
                List.of("NONE=0", "FUTURE=1", "COMPLETION_STAGE=2", "COMPLETABLE_FUTURE=3",
                        "UNSUPPORTED_CONCRETE_FUTURE=4"));
        return Map.copyOf(codes);
    }

    private static AsyncReturnType async(String methodName) throws Exception {
        return AsyncReturnType.from(SampleSignatures.class.getDeclaredMethod(methodName));
    }

    static final class SampleSignatures {

        String plain() {
            return "value";
        }

        CompletableFuture<String> completableFuture() {
            return CompletableFuture.completedFuture("value");
        }

        CompletionStage<String> completionStage() {
            return CompletableFuture.completedFuture("value");
        }

        Future<String> future() {
            return CompletableFuture.completedFuture("value");
        }

        StringFuture concreteFuture() {
            return new StringFuture();
        }
    }

    static final class StringFuture extends CompletableFuture<String> {
    }

    private static Class<?> componentException(String simpleName) {
        String type = EXCEPTION_PACKAGE + "." + simpleName;
        try {
            return Class.forName(type);
        } catch (ClassNotFoundException exception) {
            return fail("REQ-002 " + simpleName + " 必须位于 " + EXCEPTION_PACKAGE);
        }
    }

    /** 反射组装 validator：合同构造器由本 Step 新增，因此原参数按声明顺序补齐。 */
    private static Object newValidator(MethodExtensionProperties properties, ValidationUtils validationUtils)
            throws Exception {
        Class<?> type = Class.forName(VALIDATOR_TYPES.get(0));
        for (Constructor<?> candidate : type.getConstructors()) {
            if (Arrays.equals(candidate.getParameterTypes(),
                    new Class<?>[] {MethodExtensionProperties.class, ValidationUtils.class})) {
                return candidate.newInstance(properties, validationUtils);
            }
        }
        return fail("REQ-007 " + type.getSimpleName() + " 缺少 (properties, ValidationUtils) 合同构造器");
    }

    private static void afterPropertiesSet(Object validator) throws Throwable {
        try {
            validator.getClass().getMethod("afterPropertiesSet").invoke(validator);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }

    /** 固定编码与 message 都必须显式声明：message 取常量名，code 取文档化的字面量。 */
    private static List<String> codeAssignments(Class<?> enumType) throws Exception {
        Method getCode = enumType.getMethod("getCode");
        Method getMessage = enumType.getMethod("getMessage");
        Object[] constants = enumType.getEnumConstants();
        assertThat(constants).as("%s 不得是空 enum", enumType.getSimpleName()).isNotEmpty();
        List<String> rendered = new ArrayList<>();
        for (Object constant : constants) {
            String name = ((Enum<?>) constant).name();
            assertThat(getMessage.invoke(constant))
                    .as("REQ-004 %s.%s 的 message 必须保持常量名", enumType.getSimpleName(), name)
                    .isEqualTo(name);
            assertThat(EgonEnum.class.isAssignableFrom(enumType))
                    .as("REQ-004 %s 必须实现 EgonEnum", enumType.getSimpleName()).isTrue();
            rendered.add(name + "=" + getCode.invoke(constant));
        }
        return rendered;
    }

    private ValidationUtils recordingValidationUtils() {
        Validator recorder = (Validator) Proxy.newProxyInstance(
                Validator.class.getClassLoader(), new Class<?>[] {Validator.class},
                (proxy, method, args) -> {
                    if ("validate".equals(method.getName())) {
                        validatedBeans.add(args[0]);
                        return Set.of();
                    }
                    return fail("validator 门面不应调用 " + method.getName());
                });
        return new ValidationUtils(recorder);
    }

    /** 源码里的 enum 声明只写自己的名字，嵌套 enum 也不带外层类型前缀。 */
    private static String enumSimpleName(String type) {
        String qualified = simpleName(type);
        int nested = qualified.lastIndexOf('.');
        return nested < 0 ? qualified : qualified.substring(nested + 1);
    }

    private static String simpleName(String type) {
        int lastDot = type.lastIndexOf('.');
        int lastPackage = type.lastIndexOf('.', lastDot - 1);
        return lastPackage < 0 ? type : type.substring(lastPackage + 1).replace('$', '.');
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
                Matcher matcher = VALIDATOR_DECLARATION.matcher(line);
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

    /** enum 声明后的每个常量行；命中数与显式 code 一起证明扫描不是空转。 */
    private record EnumConstantLine(String enumName, String constantName, boolean explicitCode) {
    }

    private static List<EnumConstantLine> enumConstantLines(Path root) throws IOException {
        List<EnumConstantLine> scanned = new ArrayList<>();
        for (Path file : sources(root)) {
            List<String> lines = Files.readAllLines(file);
            for (int index = 0; index < lines.size(); index++) {
                Matcher header = ENUM_HEADER.matcher(lines.get(index));
                if (!header.find()) {
                    continue;
                }
                String enumName = header.group(1);
                for (int cursor = index + 1; cursor < lines.size(); cursor++) {
                    String line = lines.get(cursor).trim();
                    if (line.isEmpty() || line.startsWith("//") || line.startsWith("/*")
                            || line.startsWith("*")) {
                        continue;
                    }
                    Matcher constant = ENUM_CONSTANT.matcher(line);
                    if (!constant.find()) {
                        break;
                    }
                    scanned.add(new EnumConstantLine(
                            enumName, constant.group(1), EXPLICIT_CODE.matcher(line).find()));
                    if (line.contains(";")) {
                        break;
                    }
                }
            }
        }
        return scanned;
    }

    private static List<String> constantsWithoutExplicitCode(List<EnumConstantLine> scanned) {
        return scanned.stream()
                .filter(line -> !line.explicitCode())
                .map(line -> line.enumName() + "." + line.constantName())
                .toList();
    }

    private static List<String> packagePathMismatches(Path root) throws IOException {
        List<String> mismatches = new ArrayList<>();
        for (Path file : sources(root)) {
            Path relative = root.relativize(file);
            Path directory = relative.getParent();
            String expected = directory == null ? "" : directory.toString().replace('/', '.');
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

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        for (int index = text.indexOf(needle); index >= 0; index = text.indexOf(needle, index + 1)) {
            count++;
        }
        return count;
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
            assertThat(root).as("缺少被治理文件 %s", root).exists();
            return List.of(root);
        }
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> !path.toString().replace('\\', '/').contains("/target/"))
                    .filter(path -> !path.toString().replace('\\', '/').contains("/.generated/"))
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
