package top.egon.cola.component.bytecode.contract;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureLayer;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureSeverity;
import top.egon.cola.component.bytecode.api.architecture.DependencyKind;
import top.egon.cola.component.bytecode.api.architecture.LocationKind;
import top.egon.cola.component.bytecode.api.observation.ObservationResult;
import top.egon.cola.component.bytecode.bridge.AgentBridgeStatus;
import top.egon.cola.component.bytecode.bridge.BridgeCapability;
import top.egon.cola.component.bytecode.bridge.BridgeProtocol;
import top.egon.cola.component.bytecode.bridge.DecisionKind;
import top.egon.cola.component.bytecode.bridge.DispatcherRegistry;
import top.egon.cola.component.bytecode.starter.BytecodeAutoConfiguration;
import top.egon.cola.component.bytecode.starter.BytecodeStartupValidator;
import top.egon.cola.component.bytecode.starter.observation.ObservationMetadataValidator;
import top.egon.cola.component.common.core.enums.EgonEnum;
import top.egon.cola.component.common.core.validation.BaseValidator;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Step 12 合同：Bytecode 各模块的手写 enum、validator 与打包白名单必须落在公共合同上
 * （REQ-002、REQ-003、REQ-004、REQ-005、REQ-006、REQ-007、REQ-027）。
 *
 * <p>本组件没有自定义异常，所以 REQ-002/REQ-003 在此固定为“不得出现绕过公共根的技术异常”。
 * 拦截、增强与协议握手行为由原回归证明保持。</p>
 */
class ComponentContractGovernanceTest {

    private static final Path STARTER_SOURCES = Path.of("src/main/java");

    private static final Path STARTER_POM = Path.of("pom.xml");

    /** 扫描根：除 starter 外，其余 enum 属于同级模块，只能按源码固定，不参与反射。 */
    private static final Path[] GOVERNED_SOURCES = {
            Path.of("../egon-cola-component-bytecode-agent/src/main/java"),
            Path.of("../egon-cola-component-bytecode-api/src/main/java"),
            Path.of("../egon-cola-component-bytecode-architecture-maven-plugin/src/main/java"),
            Path.of("../egon-cola-component-bytecode-benchmark/src/main/java"),
            Path.of("../egon-cola-component-bytecode-bridge/src/main/java"),
            Path.of("../egon-cola-component-bytecode-core/src/main/java"),
            STARTER_SOURCES};

    private static final Path[] GOVERNED_POMS = {
            Path.of("../egon-cola-component-bytecode-agent/pom.xml"),
            Path.of("../egon-cola-component-bytecode-api/pom.xml"),
            Path.of("../egon-cola-component-bytecode-architecture-maven-plugin/pom.xml"),
            Path.of("../egon-cola-component-bytecode-benchmark/pom.xml"),
            Path.of("../egon-cola-component-bytecode-bridge/pom.xml"),
            Path.of("../egon-cola-component-bytecode-core/pom.xml"),
            STARTER_POM};

    /** 只依赖 common-core 自身、不得继承其传递依赖的 JDK-only 模块。 */
    private static final List<Path> TRANSITIVE_FREE_POMS = List.of(
            Path.of("../egon-cola-component-bytecode-api/pom.xml"),
            Path.of("../egon-cola-component-bytecode-bridge/pom.xml"));

    private static final Path AGENT_POM = Path.of("../egon-cola-component-bytecode-agent/pom.xml");

    private static final Path README = Path.of("../README.md");

    private static final String EGON_ENUM_CLASS_FILE =
            "top/egon/cola/component/common/core/enums/EgonEnum.class";

    private static final String COMMON_CORE_ARTIFACT = "egon-cola-component-common-core";

    private static final String CANONICAL_FACADE_BEAN = "egonColaValidationUtils";

    private static final String BYTECODE_PACKAGE = "top.egon.cola.component.bytecode";

    private static final ValidationUtils NATIVE_VALIDATION_UTILS =
            new ValidationUtils(Validation.buildDefaultValidatorFactory().getValidator());

    /** 13 个 enum 的常量总数，用于证明源码扫描真的命中了声明而不是空集合通过断言。 */
    private static final int ENUM_CONSTANT_COUNT = 69;

    /** 本组件 13 个手写 enum 的固定编码：原本没有整数 code，按当前声明顺序落 0..N-1。 */
    private static final Map<String, List<String>> ENUM_CODES = enumCodes();

    /** 只有 starter 测试类路径可见的 enum 才能做反射校验。 */
    private static final List<Class<? extends Enum<?>>> LOADABLE_ENUMS = List.of(
            ArchitectureLayer.class, ArchitectureSeverity.class, DependencyKind.class,
            LocationKind.class, ObservationResult.class, BridgeCapability.class,
            DecisionKind.class);

    private static final List<String> VALIDATOR_TYPES = List.of(
            BYTECODE_PACKAGE + ".starter.BytecodeStartupValidator",
            BYTECODE_PACKAGE + ".starter.observation.ObservationMetadataValidator");

    private static final Pattern PACKAGE = Pattern.compile("^package\\s+([\\w.]+)\\s*;");
    private static final Pattern ENUM_HEADER =
            Pattern.compile("^\\s*(?:[a-z]+\\s+)*enum\\s+(\\w+)([^{]*)\\{");
    private static final Pattern ENUM_CONSTANT =
            Pattern.compile("^\\s*([A-Z][A-Z0-9_]*)(?:\\s*\\((.*)\\))?\\s*(?:[,;])?\\s*$");
    private static final Pattern CODE_AND_MESSAGE =
            Pattern.compile("^\\s*(\\d+)\\s*,\\s*\"([^\"]*)\"\\s*(?:,.*)?$");
    private static final Pattern EXCEPTION_DECLARATION =
            Pattern.compile("\\bclass\\s+(\\w*Exception)\\s+extends\\s+(\\w+)");
    private static final Pattern VALIDATOR_DECLARATION =
            Pattern.compile("\\b(?:class|interface)\\s+(\\w*Validator)\\b");

    private final List<Object> validatedBeans = new ArrayList<>();

    @AfterEach
    void resetAgentStatus() {
        DispatcherRegistry.publishAgentStatus(AgentBridgeStatus.disabled());
    }

    // 算法第 3 行：手写 enum 实现 EgonEnum，编码是显式字面量，禁止 ordinal() 派生。
    @Test
    void handwrittenEnumsImplementTheCommonEnumContract() throws IOException {
        Map<String, String> headers = enumHeaders();
        Map<String, List<EnumConstantLine>> declared = declaredEnumConstants();

        assertThat(declared.keySet())
                .as("REQ-004 手写 enum 清单必须与本 Step 声明一致")
                .containsExactlyInAnyOrderElementsOf(ENUM_CODES.keySet());

        List<String> missingContract = new ArrayList<>();
        declared.forEach((name, constants) -> {
            String header = headers.getOrDefault(name, "");
            if (!header.contains(EgonEnum.class.getSimpleName())) {
                missingContract.add(name + header);
            }
        });
        assertThat(missingContract)
                .as("REQ-004 每个手写 enum 必须实现 EgonEnum")
                .isEmpty();

        List<EnumConstantLine> constants = declared.values().stream()
                .flatMap(List::stream)
                .toList();
        assertThat(constants)
                .as("REQ-004 常量总数必须等于 %d，否则扫描是空跑", ENUM_CONSTANT_COUNT)
                .hasSize(ENUM_CONSTANT_COUNT);
        assertThat(constants.stream().filter(EnumConstantLine::explicitCode).toList())
                .as("REQ-004 每个常量都要显式写出整数 code 与 message")
                .hasSize(ENUM_CONSTANT_COUNT);

        List<String> wrongCodes = new ArrayList<>();
        ENUM_CODES.forEach((enumName, expected) -> {
            List<String> actual = declared.getOrDefault(enumName, List.of()).stream()
                    .map(line -> line.constantName() + "=" + line.code())
                    .toList();
            if (!actual.equals(expected)) {
                wrongCodes.add(enumName + " expected=" + expected + " actual=" + actual);
            }
        });
        assertThat(wrongCodes).as("REQ-004 固定编码表").isEmpty();

        assertThat(text(GOVERNED_SOURCES))
                .as("REQ-004 code 不得来自 ordinal()")
                .doesNotContain("ordinal()");
    }

    // 算法第 3 行后半句：原 wire/name 值不改，name()/valueOf()/values() 语义保持。
    @Test
    void loadableEnumsKeepTheirWireNamesAndExposeCodesThroughTheCommonContract() throws Exception {
        List<String> rendered = new ArrayList<>();
        for (Class<? extends Enum<?>> enumType : LOADABLE_ENUMS) {
            assertThat(EgonEnum.class.isAssignableFrom(enumType))
                    .as("REQ-004 %s 必须实现 EgonEnum", enumType.getSimpleName())
                    .isTrue();
            Method getCode = enumType.getMethod("getCode");
            Method getMessage = enumType.getMethod("getMessage");
            Method valueOf = enumType.getMethod("valueOf", String.class);
            for (Object constant : enumType.getEnumConstants()) {
                String name = ((Enum<?>) constant).name();
                assertThat(getMessage.invoke(constant))
                        .as("REQ-004 %s.%s 的 message 必须保持常量名", enumType.getSimpleName(), name)
                        .isEqualTo(name);
                assertThat(valueOf.invoke(null, name))
                        .as("REQ-004 %s.%s 的 wire 名必须保持", enumType.getSimpleName(), name)
                        .isSameAs(constant);
                rendered.add(enumType.getSimpleName() + "." + name + "=" + getCode.invoke(constant));
            }
        }

        List<String> expected = new ArrayList<>();
        for (Class<? extends Enum<?>> enumType : LOADABLE_ENUMS) {
            ENUM_CODES.get(enumType.getSimpleName()).forEach(pair ->
                    expected.add(enumType.getSimpleName() + "." + pair));
        }
        assertThat(rendered)
                .as("REQ-004 可见 enum 的固定编码与声明顺序一致")
                .isEqualTo(expected);

        assertThat(codeOf(DependencyKind.valueOf("CONSTANT_POOL")))
                .as("REQ-004 末位常量编码固定为 20")
                .isEqualTo(20);
        assertThat(BridgeCapability.values())
                .as("REQ-004 跨 ClassLoader 的能力枚举集合不改")
                .containsExactly(BridgeCapability.EXECUTOR, BridgeCapability.OBSERVATION,
                        BridgeCapability.METHOD_EXTENSION);
        assertThat(AgentBridgeStatus.disabled().state())
                .as("REQ-004 协议状态仍以名字对外")
                .isEqualTo("DISABLED");
    }

    // 算法第 1、2 行：本组件不得出现绕过公共合同的自定义异常。
    @Test
    void noCustomExceptionBypassesTheCommonRoot() throws IOException {
        assertThat(declaredExceptions())
                .as("REQ-002 Bytecode 只有 JDK 与框架异常，不得新增自有异常")
                .isEmpty();
        assertThat(text(GOVERNED_SOURCES))
                .as("REQ-003 不得出现直接继承 RuntimeException 的技术异常")
                .doesNotContain("extends RuntimeException")
                .doesNotContain("extends Error");
    }

    // 算法第 4 行：validator 继承 BaseValidator，原生约束先于协议/关系检查。
    @Test
    void validatorsExtendTheCommonContractAndRunNativeConstraintsFirst() throws Exception {
        assertThat(declaredValidatorTypes())
                .as("REQ-007 本组件 validator 清单")
                .containsExactlyInAnyOrderElementsOf(VALIDATOR_TYPES);

        for (String type : VALIDATOR_TYPES) {
            Class<?> validatorType = Class.forName(type);
            assertThat(BaseValidator.class.isAssignableFrom(validatorType))
                    .as("REQ-007 %s 必须继承公共 BaseValidator", validatorType.getSimpleName())
                    .isTrue();
            assertThat(Arrays.stream(validatorType.getConstructors())
                    .anyMatch(candidate ->
                            Arrays.asList(candidate.getParameterTypes()).contains(ValidationUtils.class)))
                    .as("REQ-007 %s 必须以 ValidationUtils 构造注入", validatorType.getSimpleName())
                    .isTrue();
            String source = read(sourcesOf(validatorType));
            assertThat(source)
                    .as("REQ-005 %s 必须通过 validateBean 走原生约束", validatorType.getSimpleName())
                    .contains("validateBean(");
            assertThat(source)
                    .as("MC-BEAN-001 validator 不得自建 Validator 或门面")
                    .doesNotContain("ValidatorFactory")
                    .doesNotContain("new ValidationUtils(");
        }

        Map<String, String> invalidTags = Map.of("dynamic", "${secret}");
        ObservationMetadataValidator rejecting =
                newObservationValidator(recordingValidationUtils());
        assertThatThrownBy(() -> rejecting.validate(invalidTags))
                .as("REQ-005 输入失败不得产生指标副作用")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("dynamic observation static tag: dynamic");
        assertThat(validatedBeans)
                .as("REQ-005 原生约束先于关系检查")
                .containsExactly((Object) invalidTags);

        ObservationMetadataValidator accepting =
                newObservationValidator(recordingValidationUtils());
        assertThatCode(() -> accepting.validate(Map.of("channel", "test")))
                .as("REQ-005 合法标签仍然通过")
                .doesNotThrowAnyException();

        AgentBridgeStatus incompatible = new AgentBridgeStatus(
                "test", "ACTIVE", BridgeProtocol.MAJOR + 1, BridgeProtocol.MINOR,
                Set.of(BridgeCapability.EXECUTOR), Set.of(BridgeCapability.EXECUTOR), 0, 0, 0, List.of());
        DispatcherRegistry.publishAgentStatus(incompatible);
        assertThatThrownBy(() -> constructStartupValidator(recordingValidationUtils()))
                .as("REQ-007 协议关系检查必须保留")
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Bytecode Agent protocol mismatch: bridge=");
        assertThat(validatedBeans)
                .as("REQ-005 状态对象在协议比对前先过原生约束")
                .endsWith((Object) incompatible);

        DispatcherRegistry.publishAgentStatus(AgentBridgeStatus.disabled());
        assertThatCode(() -> constructStartupValidator(NATIVE_VALIDATION_UTILS))
                .as("REQ-007 Agent 缺席时启动仍然通过")
                .doesNotThrowAnyException();
    }

    // 算法第 4 行 + REQ-007：standalone 启动按名称条件发布门面，validator 复用同一实例。
    @Test
    void standaloneStarterPublishesTheCanonicalFacadeOnce() throws IOException {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(BytecodeAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ValidationUtils.class);
                    assertThat(context.getBeanNamesForType(ValidationUtils.class))
                            .as("REQ-007 门面必须使用公共规范名")
                            .containsExactly(CANONICAL_FACADE_BEAN);
                    assertThat((Object) context.getBean(BytecodeStartupValidator.class))
                            .extracting("validationUtils")
                            .isSameAs(context.getBean(CANONICAL_FACADE_BEAN));
                });

        String configuration = read(STARTER_SOURCES.resolve(
                "top/egon/cola/component/bytecode/starter/BytecodeAutoConfiguration.java"));
        assertThat(configuration)
                .as("REQ-007 门面按名称条件发布并注入 validator")
                .contains("@Bean(name = \"" + CANONICAL_FACADE_BEAN + "\")")
                .contains("@ConditionalOnMissingBean(name = \"" + CANONICAL_FACADE_BEAN + "\")")
                .contains("ValidationAutoConfiguration.class")
                .contains("@Qualifier(\"" + CANONICAL_FACADE_BEAN + "\")");
        assertThat(countOccurrences(configuration, "new ValidationUtils("))
                .as("门面只包装一次 Validator")
                .isEqualTo(1);

        String metrics = read(STARTER_SOURCES.resolve(
                "top/egon/cola/component/bytecode/starter/metrics/BytecodeMetricsAutoConfiguration.java"));
        assertThat(metrics)
                .as("REQ-007 第二个 validator 同样按名称取用门面，且不重复发布")
                .contains("@Qualifier(\"" + CANONICAL_FACADE_BEAN + "\")")
                .doesNotContain("@Bean(name = \"" + CANONICAL_FACADE_BEAN + "\")");
    }

    // 算法第 5、6 行：不引入伪造转换层，依赖方向与打包白名单保持精确。
    @Test
    void dependencyDirectionAndPackagingWhitelistStayExact() throws IOException {
        assertThat(text(GOVERNED_SOURCES))
                .as("Rule 5 禁止 BeanUtils 复制；本组件没有自有对象转换语义对")
                .doesNotContain("BeanUtils")
                .doesNotContain("BaseConverter")
                .doesNotContain("org.mapstruct");

        for (Path pom : GOVERNED_POMS) {
            assertThat(read(pom))
                    .as("MC-DEP-001 直接使用 EgonEnum 的模块必须显式声明 common-core")
                    .contains(COMMON_CORE_ARTIFACT);
        }
        for (Path pom : TRANSITIVE_FREE_POMS) {
            String content = read(pom);
            assertThat(content)
                    .as("MC-ARCH-001 JDK-only 模块不得继承 common-core 的传递依赖")
                    .contains("<exclusion>");
            assertThat(content)
                    .as("MC-ARCH-001 JDK-only 模块不得引入 Guava")
                    .doesNotContain("guava");
        }
        assertThat(read(STARTER_POM))
                .as("REQ-007 standalone 校验 provider 必须存在")
                .contains("spring-boot-starter-validation");

        String agentPom = read(AGENT_POM);
        assertThat(agentPom)
                .as("MC-ARCH-001 agent 只白名单打包 EgonEnum 一个公共类")
                .contains("<include>top.egon:" + COMMON_CORE_ARTIFACT + "</include>")
                .contains("<include>" + EGON_ENUM_CLASS_FILE + "</include>");

        assertThat(packagePathMismatches())
                .as("REQ-027 包声明必须与目录一致")
                .isEmpty();

        String readme = read(README);
        assertThat(readme)
                .as("README 只同步本 Step 落地的结构")
                .contains(EgonEnum.class.getSimpleName())
                .contains(CANONICAL_FACADE_BEAN)
                .contains(COMMON_CORE_ARTIFACT);
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

    /** (ValidationUtils) 合同构造器由本 Step 新增，因此反射组装以避免 RED 退化为编译错误。 */
    private static Object newValidator(Class<?> type, ValidationUtils validationUtils) throws Exception {
        for (Constructor<?> candidate : type.getConstructors()) {
            if (Arrays.equals(candidate.getParameterTypes(), new Class<?>[] {ValidationUtils.class})) {
                return candidate.newInstance(validationUtils);
            }
        }
        return fail("REQ-007 " + type.getSimpleName() + " 缺少 (ValidationUtils) 合同构造器");
    }

    private ObservationMetadataValidator newObservationValidator(ValidationUtils validationUtils)
            throws Exception {
        return (ObservationMetadataValidator) newValidator(
                ObservationMetadataValidator.class, validationUtils);
    }

    private void constructStartupValidator(ValidationUtils validationUtils) throws Throwable {
        try {
            newValidator(BytecodeStartupValidator.class, validationUtils);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }

    private static int codeOf(Object constant) throws Exception {
        return (Integer) constant.getClass().getMethod("getCode").invoke(constant);
    }


    private static Map<String, List<String>> enumCodes() {
        Map<String, List<String>> codes = new LinkedHashMap<>();
        codes.put("AgentFailurePolicy", List.of("SKIP_CLASS=0", "DISABLE_FEATURE=1", "MARK_FATAL=2"));
        codes.put("AgentState", List.of("DISABLED=0", "STARTING=1", "ACTIVE=2", "DEGRADED=3", "FAILED=4"));
        codes.put("ArchitectureLayer", List.of("DOMAIN=0", "APPLICATION=1", "INFRASTRUCTURE=2",
                "ADAPTER=3", "FACADE=4", "STARTER=5", "COMMON=6", "UNKNOWN=7"));
        codes.put("ArchitectureSeverity", List.of("ERROR=0", "WARNING=1", "INFO=2"));
        codes.put("DependencyKind", List.of("EXTENDS=0", "IMPLEMENTS=1", "FIELD=2", "PARAMETER=3",
                "RETURN=4", "THROWS=5", "SIGNATURE=6", "ANNOTATION=7", "NEW=8", "ARRAY=9", "CAST=10",
                "INSTANCEOF=11", "FIELD_READ=12", "FIELD_WRITE=13", "METHOD_CALL=14",
                "CONSTRUCTOR_CALL=15", "METHOD_HANDLE=16", "INVOKEDYNAMIC=17", "LAMBDA_TARGET=18",
                "CONSTANT_DYNAMIC=19", "CONSTANT_POOL=20"));
        codes.put("LocationKind", List.of("CLASS=0", "FIELD=1", "METHOD=2", "INSTRUCTION=3"));
        codes.put("ObservationResult", List.of("SUCCESS=0", "ERROR=1"));
        codes.put("ArchitectureFailurePolicy", List.of("FAIL=0", "WARN=1", "REPORT_ONLY=2"));
        codes.put("UnknownLayerPolicy", List.of("IGNORE=0", "WARN=1", "FAIL=2"));
        codes.put("ArchitectureClassLayer", List.of("DOMAIN=0", "APPLICATION=1", "INFRASTRUCTURE=2",
                "ADAPTER=3", "FACADE=4", "STARTER=5", "COMMON=6"));
        codes.put("BridgeCapability", List.of("EXECUTOR=0", "OBSERVATION=1", "METHOD_EXTENSION=2"));
        codes.put("DecisionKind", List.of("PROCEED=0", "RETURN_NULL=1", "RETURN_VALUE=2", "THROW=3"));
        codes.put("EnhancementFeature", List.of("EXECUTOR=0", "OBSERVATION=1", "METHOD_EXTENSION=2"));
        return Map.copyOf(codes);
    }

    /** 逐行解析 enum 声明块：常量列表在 {@code ;} 之后的第一行非常量处结束。 */
    private static Map<String, List<EnumConstantLine>> declaredEnumConstants() throws IOException {
        Map<String, List<EnumConstantLine>> constants = new LinkedHashMap<>();
        for (Path root : GOVERNED_SOURCES) {
            for (Path file : sources(root)) {
                List<String> lines = Files.readAllLines(file);
                for (int index = 0; index < lines.size(); index++) {
                    Matcher header = ENUM_HEADER.matcher(lines.get(index));
                    if (header.matches()) {
                        constants.put(header.group(1),
                                readConstants(lines.subList(index + 1, lines.size())));
                    }
                }
            }
        }
        return constants;
    }

    private static List<EnumConstantLine> readConstants(List<String> lines) {
        List<EnumConstantLine> constants = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!Character.isUpperCase(trimmed.isEmpty() ? ' ' : trimmed.charAt(0))
                    && !constants.isEmpty()) {
                return constants;
            }
            Matcher matcher = ENUM_CONSTANT.matcher(line);
            if (!matcher.matches()) {
                return constants;
            }
            Matcher code = matcher.group(2) == null
                    ? null : CODE_AND_MESSAGE.matcher(matcher.group(2));
            boolean explicitCode = code != null && code.matches();
            constants.add(new EnumConstantLine(matcher.group(1),
                    explicitCode ? Integer.parseInt(code.group(1)) : -1, explicitCode));
            if (trimmed.endsWith(";") || trimmed.endsWith("}")) {
                return constants;
            }
        }
        return constants;
    }

    private static Map<String, String> enumHeaders() throws IOException {
        Map<String, String> headers = new LinkedHashMap<>();
        for (Path root : GOVERNED_SOURCES) {
            for (Path file : sources(root)) {
                for (String line : Files.readAllLines(file)) {
                    Matcher matcher = ENUM_HEADER.matcher(line);
                    if (matcher.matches()) {
                        headers.put(matcher.group(1), matcher.group(2));
                    }
                }
            }
        }
        return headers;
    }

    private static Map<String, String> declaredExceptions() throws IOException {
        Map<String, String> declared = new LinkedHashMap<>();
        for (Path root : GOVERNED_SOURCES) {
            for (Path file : sources(root)) {
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
        }
        return declared;
    }

    private static Set<String> declaredValidatorTypes() throws IOException {
        Set<String> validators = new LinkedHashSet<>();
        for (Path root : GOVERNED_SOURCES) {
            for (Path file : sources(root)) {
                String packageName = "";
                for (String line : Files.readAllLines(file)) {
                    Matcher packageMatcher = PACKAGE.matcher(line);
                    if (packageMatcher.find()) {
                        packageName = packageMatcher.group(1);
                        continue;
                    }
                    Matcher matcher = VALIDATOR_DECLARATION.matcher(line);
                    while (matcher.find()) {
                        validators.add(packageName + "." + matcher.group(1));
                    }
                }
            }
        }
        return validators;
    }

    private static List<String> packagePathMismatches() throws IOException {
        List<String> mismatches = new ArrayList<>();
        for (Path root : GOVERNED_SOURCES) {
            String prefix = root.toString() + java.io.File.separator;
            for (Path file : sources(root)) {
                String declared = "";
                for (String line : Files.readAllLines(file)) {
                    Matcher matcher = PACKAGE.matcher(line);
                    if (matcher.find()) {
                        declared = matcher.group(1);
                        break;
                    }
                }
                String relative = file.toString().substring(prefix.length());
                String expected = relative.substring(0, relative.lastIndexOf(java.io.File.separatorChar))
                        .replace(java.io.File.separatorChar, '.');
                if (!declared.equals(expected)) {
                    mismatches.add(file + " declares " + declared + " but lives in " + expected);
                }
            }
        }
        return mismatches;
    }

    private static Path sourcesOf(Class<?> type) {
        String relative = type.getName().replace('.', '/') + ".java";
        for (Path root : GOVERNED_SOURCES) {
            Path candidate = root.resolve(relative);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return fail("REQ-027 找不到 " + type.getName() + " 的源码");
    }

    private static List<Path> sources(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.toString().contains("/target/"))
                    .filter(path -> !path.toString().contains("/.generated/"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }
    }

    private static String text(Path[] roots) throws IOException {
        StringBuilder text = new StringBuilder();
        for (Path root : roots) {
            for (Path file : sources(root)) {
                text.append(read(file));
            }
        }
        return text.toString();
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            return fail("无法读取 " + path, exception);
        }
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        for (int index = text.indexOf(needle); index >= 0; index = text.indexOf(needle, index + 1)) {
            count++;
        }
        return count;
    }

    private record EnumConstantLine(String constantName, int code, boolean explicitCode) {
    }
}
