package top.egon.cola.component.agentflow.contract;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.agentflow.autoconfigure.AgentFlowProperties;
import top.egon.cola.component.agentflow.config.AgentConfigDTO;
import top.egon.cola.component.agentflow.config.AgentFlowConfigDTO;
import top.egon.cola.component.agentflow.config.AgentFlowConfigValidator;
import top.egon.cola.component.agentflow.config.AgentWorkflowTypeEnum;
import top.egon.cola.component.agentflow.runtime.AgentFlowRuntimeBO;
import top.egon.cola.component.agentflow.workflow.AgentWorkflowBuilderStrategy;
import top.egon.cola.component.agentflow.workflow.AgentWorkflowStrategyFactory;
import top.egon.cola.component.agentflow.workflow.LoopAgentWorkflowBuilderStrategy;
import top.egon.cola.component.agentflow.workflow.ParallelAgentWorkflowBuilderStrategy;
import top.egon.cola.component.agentflow.workflow.SequentialAgentWorkflowBuilderStrategy;
import top.egon.cola.component.common.core.enums.EgonEnum;
import top.egon.cola.component.common.core.enums.ErrorStatus;
import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.validation.BaseValidator;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
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
 * Step 8 合同：Agent Flow 的自定义异常、手写 enum、validator 与转换必须落在公共合同上
 * （REQ-002、REQ-003、REQ-004、REQ-005、REQ-006、REQ-007、REQ-027）。
 *
 * <p>本文件只固定本 Step 列出的六条算法差异；ADK 图规则、租约状态机与 Reactor 超时分支由原回归证明保持。</p>
 */
class ComponentContractGovernanceTest {

    private static final Path MAIN_SOURCES = Path.of("src/main/java");

    private static final Path POM = Path.of("pom.xml");

    private static final String AGENT_FLOW_PACKAGE = "top.egon.cola.component.agentflow";

    private static final String EXCEPTION_PACKAGE = AGENT_FLOW_PACKAGE + ".common.exception";

    private static final String CANONICAL_FACADE_BEAN = "egonColaValidationUtils";

    private static final String LEGACY_FACADE_BEAN = "agentFlowValidationUtils";

    private static final List<String> COMPONENT_EXCEPTIONS = List.of(
            "AgentFlowException", "AgentFlowConfigurationException", "AgentFlowExecutionException",
            "AgentFlowExecutionTimeoutException", "AgentFlowNotFoundException", "AgentFlowSessionBusyException",
            "AgentFlowSessionNotFoundException");

    /** 只有配置异常携带 flowId/nodeName/reason 诊断入参，其余异常历史上就只接收已拼好的安全消息。 */
    private static final List<String> MESSAGE_ONLY_EXCEPTIONS = List.of(
            "AgentFlowException", "AgentFlowNotFoundException", "AgentFlowSessionBusyException",
            "AgentFlowSessionNotFoundException");

    private static final List<String> VALIDATOR_TYPES = List.of(
            AGENT_FLOW_PACKAGE + ".config.AgentFlowConfigValidator");

    /** 算法第 6 行：旧 FQCN 与旧私有门面 Bean 名在消费者、反射名称、测试与 README 中必须零残留。 */
    private static final List<String> RELOCATED_TYPES = COMPONENT_EXCEPTIONS.stream()
            .map(name -> AGENT_FLOW_PACKAGE + ".exception." + name)
            .toList();

    private static final Path[] GOVERNED_TEXT_ROOTS = {
            MAIN_SOURCES,
            Path.of("src/test/java"),
            POM,
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
    private static final Pattern VALIDATOR_DECLARATION =
            Pattern.compile("\\bclass\\s+(\\w*Validator)\\b");

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

        Class<?> root = commonException("AgentFlowException");
        assertThat(root.getSuperclass())
                .as("REQ-002 技术异常根必须继承 CommonException")
                .isEqualTo(CommonException.class);
        List<String> wrongParent = new ArrayList<>();
        declared.forEach((name, packageName) -> {
            if (name.equals("AgentFlowException")) {
                return;
            }
            try {
                Class<?> parent = Class.forName(packageName + "." + name).getSuperclass();
                if (parent != root && parent != CommonException.class) {
                    wrongParent.add(name + " -> " + parent.getName());
                }
            } catch (ClassNotFoundException exception) {
                wrongParent.add(name + " -> 无法加载");
            }
        });
        assertThat(wrongParent).as("REQ-002 其余异常继续挂在组件根上").isEmpty();
        assertThat(text(MAIN_SOURCES))
                .as("REQ-002 不得再直接继承 RuntimeException")
                .doesNotContain("extends RuntimeException");
    }

    // 算法第 2 行：原 cause/安全消息/构造形态保持，int code 与 status 只来自公共根，不新增诊断字段。
    @Test
    void originalCausesMessagesAndDiagnosticShapesSurviveTheNormalization() throws Exception {
        Class<?> root = commonException("AgentFlowException");
        CommonException plain = (CommonException) root
                .getConstructor(String.class).newInstance("Agent Flow registry is not open: CLOSING");
        assertThat(plain.getCode())
                .as("REQ-003 归一化后 code 仍是公共 int 合同")
                .isEqualTo(ResultCode.SYSTEM_ERROR.getCode());
        assertThat(plain.getStatus()).isEqualTo(ResultCode.SYSTEM_ERROR.getStatus());
        assertThat(plain.getMessage()).isEqualTo("Agent Flow registry is not open: CLOSING");
        assertThat(plain.isRetryable()).isFalse();

        Class<?> configuration = commonException("AgentFlowConfigurationException");
        assertThat(configuration.getSuperclass()).isEqualTo(root);
        CommonException withoutCause = (CommonException) configuration
                .getConstructor(String.class, String.class, String.class)
                .newInstance("research-flow", "planner", "duplicate node name");
        assertThat(withoutCause.getMessage())
                .as("安全消息只暴露 flow/node 边界，不泄漏载荷")
                .isEqualTo("Invalid Agent Flow configuration for flow 'research-flow'"
                        + " at 'planner': duplicate node name");
        assertThat(withoutCause.getCause()).isNull();
        RuntimeException origin = new RuntimeException("origin");
        CommonException withCause = (CommonException) configuration
                .getConstructor(String.class, String.class, String.class, Throwable.class)
                .newInstance("properties", "flows", "constraint violation", origin);
        assertThat(withCause.getCause())
                .as("REQ-003 原 cause 必须保持同一实例")
                .isSameAs(origin);
        assertThat(withCause.getMessage())
                .isEqualTo("Invalid Agent Flow configuration for flow 'properties'"
                        + " at 'flows': constraint violation");

        for (String name : List.of("AgentFlowExecutionException", "AgentFlowExecutionTimeoutException")) {
            Class<?> type = commonException(name);
            assertThat(type.getSuperclass()).isEqualTo(root);
            assertThat(type.getConstructors())
                    .as("%s 只有 (String, Throwable) 诊断构造器", name)
                    .hasSize(1);
            RuntimeException cause = new RuntimeException("cause");
            CommonException failure = (CommonException) type
                    .getConstructor(String.class, Throwable.class).newInstance("research-flow", cause);
            assertThat(failure.getCause()).isSameAs(cause);
            assertThat(failure.getMessage())
                    .as("%s 保留原消息模板", name)
                    .contains("research-flow");
        }

        for (String name : List.of("AgentFlowNotFoundException", "AgentFlowSessionBusyException",
                "AgentFlowSessionNotFoundException")) {
            Class<?> type = commonException(name);
            assertThat(type.getSuperclass()).isEqualTo(root);
            assertThat(type.getConstructors())
                    .as("%s 只有单 message 构造器", name)
                    .hasSize(1);
            CommonException failure = (CommonException) type
                    .getConstructor(String.class).newInstance("research-flow");
            assertThat(failure.getMessage()).contains("research-flow");
            assertThat(failure.getCause()).isNull();
        }
        assertThat(MESSAGE_ONLY_EXCEPTIONS).allMatch(COMPONENT_EXCEPTIONS::contains);

        List<String> shadowed = new ArrayList<>();
        for (String name : COMPONENT_EXCEPTIONS) {
            Class<?> type = commonException(name);
            for (Method method : type.getDeclaredMethods()) {
                if (Set.of("getCode", "getStatus", "isRetryable").contains(method.getName())) {
                    shadowed.add(name + "#" + method.getName());
                }
            }
            for (Field field : type.getDeclaredFields()) {
                if (Set.of("code", "status", "retryable").contains(field.getName())) {
                    shadowed.add(name + "#" + field.getName());
                }
            }
        }
        assertThat(shadowed)
                .as("REQ-003 不得遮蔽公共 int code/status/retryable 合同")
                .isEmpty();
    }

    // 算法第 3 行：手写 enum 实现 EgonEnum，常量为显式字面量，既有整数线值与名称不改。
    @Test
    void handwrittenEnumsImplementTheCommonEnumContract() throws Exception {
        List<String> headers = enumHeaders(MAIN_SOURCES);

        assertThat(headers).as("本模块手写 enum 清单").containsExactly(
                "AgentWorkflowTypeEnum implements EgonEnum",
                "State implements EgonEnum",
                "State implements EgonEnum");
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

        assertThat(AgentWorkflowTypeEnum.values()).extracting(Enum::name)
                .containsExactly("SEQUENTIAL", "PARALLEL", "LOOP");
        assertThat(codes(AgentWorkflowTypeEnum.class)).containsExactly(0, 1, 2);
        assertThat(invoke(AgentWorkflowTypeEnum.values(), AgentWorkflowTypeEnum.LOOP, "getMessage"))
                .as("REQ-004 message 使用原常量名").isEqualTo("LOOP");

        assertThat(privateEnum("execution.AgentFlowSessionExecutionGuard", "State"))
                .containsExactly("OPEN=0", "CLOSING=1", "CLOSED=2");
        assertThat(privateEnum("runtime.DefaultAgentFlowRegistry", "State"))
                .containsExactly("OPEN=0", "CLOSING=1", "CLOSED=2");

        // 原 wire/name 语义不变：状态消息仍按常量名输出，策略工厂仍按枚举选择策略。
        String guard = read(MAIN_SOURCES.resolve(
                "top/egon/cola/component/agentflow/execution/AgentFlowSessionExecutionGuard.java"));
        assertThat(guard).contains("\"Agent Flow execution guard is not open: \" + state");
        String registry = read(MAIN_SOURCES.resolve(
                "top/egon/cola/component/agentflow/runtime/DefaultAgentFlowRegistry.java"));
        assertThat(registry).contains("\"Agent Flow registry is not open: \" + current");
        assertThat(new AgentWorkflowStrategyFactory(List.<AgentWorkflowBuilderStrategy>of(
                new SequentialAgentWorkflowBuilderStrategy(),
                new ParallelAgentWorkflowBuilderStrategy(),
                new LoopAgentWorkflowBuilderStrategy()))
                .getStrategy(AgentWorkflowTypeEnum.LOOP))
                .isInstanceOf(LoopAgentWorkflowBuilderStrategy.class);
    }

    // 算法第 4 行：validator 继承 BaseValidator，原生约束先行，ADK 图规则与只读语义保持。
    @Test
    void validatorsExtendTheCommonContractAndRunNativeConstraintsFirst() throws Exception {
        assertThat(declaredValidatorTypes())
                .as("本模块 validator 清单")
                .containsExactlyElementsOf(VALIDATOR_TYPES);
        for (String type : VALIDATOR_TYPES) {
            Class<?> validatorType = Class.forName(type);
            assertThat(BaseValidator.class.isAssignableFrom(validatorType))
                    .as("REQ-007 %s 必须继承 BaseValidator", type).isTrue();
            assertThat(Arrays.stream(validatorType.getConstructors())
                    .anyMatch(candidate ->
                            Arrays.asList(candidate.getParameterTypes()).contains(ValidationUtils.class)))
                    .as("REQ-007 %s 必须注入具名公共 ValidationUtils", type).isTrue();
        }

        String autoConfiguration = read(MAIN_SOURCES.resolve(
                "top/egon/cola/component/agentflow/autoconfigure/AgentFlowAutoConfiguration.java"));
        assertThat(autoConfiguration)
                .as("REQ-007 门面必须使用公共规范 Bean 名并只包装一次")
                .contains("@Bean(name = \"" + CANONICAL_FACADE_BEAN + "\")")
                .contains("@ConditionalOnMissingBean(name = \"" + CANONICAL_FACADE_BEAN + "\")");
        assertThat(countOccurrences(autoConfiguration, "new ValidationUtils("))
                .as("门面 Bean 只包装一次 Validator").isEqualTo(1);
        assertThat(text(MAIN_SOURCES))
                .as("REQ-007 旧私有门面名不得残留")
                .doesNotContain(LEGACY_FACADE_BEAN);
        for (Path file : validatorSources()) {
            assertThat(read(file))
                    .as("%s 不得自建校验设施", file)
                    .doesNotContain("buildDefaultValidatorFactory")
                    .doesNotContain("ValidatorFactory")
                    .contains("validateBean(");
        }
        assertThat(validatorText())
                .as("输入失败不得触发 DAO/缓存/MQ 副作用")
                .doesNotContain("JdbcTemplate")
                .doesNotContain("RedisTemplate")
                .doesNotContain("RabbitTemplate")
                .doesNotContain("KafkaTemplate");

        AgentFlowProperties enabled = properties(validFlow());
        assertThatCode(() -> new AgentFlowConfigValidator(recordingValidationUtils()).validate(enabled))
                .doesNotThrowAnyException();
        assertThat(validatedBeans).as("原生约束必须先于图规则").containsExactly(enabled);

        AgentFlowConfigValidator validator = new AgentFlowConfigValidator(realValidationUtils());
        AgentFlowProperties emptyAgents = properties(new AgentFlowConfigDTO(
                "researchChatModel", "research-model", "planner", List.of(), List.of()));
        assertThatThrownBy(() -> validator.validate(emptyAgents))
                .as("字段规则必须走原生约束再映射为组件异常")
                .isInstanceOf(CommonException.class)
                .hasMessageStartingWith("Invalid Agent Flow configuration for flow 'properties' at 'flows")
                .hasMessageEndingWith(": constraint violation")
                .cause().isInstanceOf(ConstraintViolationException.class);

        AgentFlowProperties duplicated = properties(new AgentFlowConfigDTO(
                "researchChatModel", "research-model", "root",
                List.of(agent("root", "first"), agent("root", "second")), List.of()));
        assertThatThrownBy(() -> validator.validate(duplicated))
                .as("既有图规则与消息保持不变")
                .isInstanceOf(CommonException.class)
                .hasMessage("Invalid Agent Flow configuration for flow 'research-flow' at 'root'"
                        + ": duplicate node name");
        assertThatThrownBy(() -> validator.validate(null))
                .as("空输入仍走公共参数校验且不触发装配副作用")
                .isInstanceOf(IllegalArgumentException.class);

        AgentFlowProperties disabled = new AgentFlowProperties(
                false, Duration.ofSeconds(30), Duration.ofSeconds(10), Map.of("research-flow", duplicated.flows()
                .get("research-flow")));
        assertThat(validator.validate(disabled))
                .as("enabled=false 仍只短路图规则，原生约束先行")
                .isSameAs(disabled);
    }

    // 算法第 5 行：转换只服务真实 S/T 语义对；本模块直接构造 record 载体，不得伪造公共 converter。
    @Test
    void conversionsStayOnTheCommonContractWithoutFakeConverters() throws Exception {
        String sources = text(MAIN_SOURCES);

        assertThat(sources).as("Rule 5 禁止 BeanUtils 复制").doesNotContain("BeanUtils");
        assertThat(sources).as("本模块没有自有对象转换语义对").doesNotContain("BaseConverter");
        assertThat(sources).as("ADK/ChatModel 由框架对象直接装配，不引入映射库").doesNotContain("org.mapstruct");
        for (Class<?> carrier : List.of(
                AgentFlowProperties.class, AgentFlowConfigDTO.class, AgentConfigDTO.class,
                AgentFlowRuntimeBO.class)) {
            assertThat(carrier.isRecord())
                    .as("Rule 3 简单载体继续保持 record 合同").isTrue();
        }
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
                assertThat(content)
                        .as("%s 不应引用旧私有门面 Bean %s", file, LEGACY_FACADE_BEAN)
                        .doesNotContain(LEGACY_FACADE_BEAN);
            }
        }

        assertThat(packagePathMismatches(MAIN_SOURCES))
                .as("REQ-027 包声明必须与目录一致，且不重新引入旧 exception 包")
                .isEmpty();
        Path packageInfo = MAIN_SOURCES.resolve(
                "top/egon/cola/component/agentflow/common/exception/package-info.java");
        assertThat(packageInfo).as("REQ-027 所属包必须记录职责").exists();
        assertThat(read(packageInfo)).contains("package " + EXCEPTION_PACKAGE + ";");

        String pom = read(POM);
        assertThat(pom).as("MC-DEP-001 直接使用合同必须显式声明")
                .contains("egon-cola-component-common-core")
                .contains("spring-boot-starter-validation");
        String readme = read(Path.of("README.md"));
        assertThat(readme).as("File 28 只同步本 Step 落地的结构")
                .contains(EXCEPTION_PACKAGE)
                .contains(CANONICAL_FACADE_BEAN);
    }

    private static Class<?> commonException(String simpleName) {
        String type = EXCEPTION_PACKAGE + "." + simpleName;
        try {
            return Class.forName(type);
        } catch (ClassNotFoundException exception) {
            return fail("REQ-002 " + simpleName + " 必须位于 " + EXCEPTION_PACKAGE);
        }
    }

    /** 状态机 enum 只在宿主类内部使用，因此按名反射读取其固定编码。 */
    private static List<String> privateEnum(String host, String simpleName) throws Exception {
        Class<?> type = Class.forName(AGENT_FLOW_PACKAGE + "." + host + "$" + simpleName);
        Method getCode;
        try {
            getCode = type.getMethod("getCode");
        } catch (NoSuchMethodException missing) {
            return fail("REQ-004 " + type.getName() + " 必须实现 EgonEnum 并提供 getCode()");
        }
        getCode.setAccessible(true);
        List<String> rendered = new ArrayList<>();
        for (Object constant : type.getEnumConstants()) {
            rendered.add(((Enum<?>) constant).name() + "=" + getCode.invoke(constant));
        }
        return rendered;
    }

    private static AgentFlowProperties properties(AgentFlowConfigDTO flow) {
        return new AgentFlowProperties(true, Duration.ofSeconds(30), Duration.ofSeconds(10),
                Map.of("research-flow", flow));
    }

    private static AgentFlowConfigDTO validFlow() {
        return new AgentFlowConfigDTO("researchChatModel", "research-model", "planner",
                List.of(agent("planner", "result")), List.of());
    }

    private static AgentConfigDTO agent(String name, String outputKey) {
        return new AgentConfigDTO(name, null, "Do the work.", outputKey);
    }

    private static ValidationUtils realValidationUtils() {
        return new ValidationUtils(Validation.buildDefaultValidatorFactory().getValidator());
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

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        for (int index = text.indexOf(needle); index >= 0; index = text.indexOf(needle, index + 1)) {
            count++;
        }
        return count;
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

    /** 常量类是 final 的，因此 getter 只能按公共合同反射读取，保证本文件在 RED 阶段仍可编译。 */
    private static Object invoke(Enum<?>[] constants, Enum<?> target, String getter) throws Exception {
        Method method = target.getClass().getMethod(getter);
        method.setAccessible(true);
        return method.invoke(target);
    }

    private static List<Path> validatorSources() throws IOException {
        List<Path> files = new ArrayList<>();
        for (String type : VALIDATOR_TYPES) {
            files.add(MAIN_SOURCES.resolve(type.replace('.', '/') + ".java"));
        }
        return files;
    }

    private static String validatorText() throws IOException {
        StringBuilder builder = new StringBuilder();
        for (Path file : validatorSources()) {
            builder.append(read(file)).append('\n');
        }
        return builder.toString();
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
