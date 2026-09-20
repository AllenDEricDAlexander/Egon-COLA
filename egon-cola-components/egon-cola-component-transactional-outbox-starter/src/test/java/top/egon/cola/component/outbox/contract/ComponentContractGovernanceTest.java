package top.egon.cola.component.outbox.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.enums.ErrorStatus;
import top.egon.cola.component.common.core.enums.EgonEnum;
import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.validation.BaseValidator;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.outbox.aop.TransactionalMessageMethodValidator;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.autoconfigure.OutboxConfigurationValidator;
import top.egon.cola.component.outbox.autoconfigure.TransactionalOutboxProperties;
import top.egon.cola.component.outbox.delivery.DeliveryResult;
import top.egon.cola.component.outbox.delivery.rabbitmq.RabbitPublishOutcome;
import top.egon.cola.component.outbox.serialization.SerializedOutboxPayload;
import top.egon.cola.component.outbox.store.OutboxSchemaValidator;
import top.egon.cola.component.outbox.store.OutboxStatus;
import top.egon.cola.component.outbox.store.OutboxStore;
import top.egon.cola.component.outbox.validation.OutboxMessageValidator;

import java.io.IOException;
import java.lang.reflect.Constructor;
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
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Step 7 合同：Outbox 的自定义异常、手写 enum、validator 与转换必须落在公共合同上
 * （REQ-002、REQ-003、REQ-004、REQ-005、REQ-006、REQ-007、REQ-027）。
 *
 * <p>本文件只固定本 Step 列出的六条算法差异；SQL 线值、事务分支与既有投递判定由原回归证明保持。</p>
 */
class ComponentContractGovernanceTest {

    private static final Path MAIN_SOURCES = Path.of("src/main/java");

    private static final Path POM = Path.of("pom.xml");

    private static final String OUTBOX_PACKAGE = "top.egon.cola.component.outbox";

    private static final String EXCEPTION_PACKAGE = OUTBOX_PACKAGE + ".common.exception";

    private static final List<String> COMPONENT_EXCEPTIONS = List.of(
            "OutboxException", "OutboxConfigurationException", "OutboxIdempotencyConflictException",
            "OutboxMessageResolutionException", "OutboxSerializationException", "OutboxStorageException",
            "OutboxTransactionMismatchException", "OutboxTransactionRequiredException",
            "OutboxTransactionSynchronizationException", "OutboxValidationException");

    /** 只有三个事务类异常历史上只提供单 message 构造器，归一化后必须继续保持。 */
    private static final List<String> MESSAGE_ONLY_EXCEPTIONS = List.of(
            "OutboxTransactionMismatchException", "OutboxTransactionRequiredException",
            "OutboxTransactionSynchronizationException");

    private static final List<String> VALIDATOR_TYPES = List.of(
            OUTBOX_PACKAGE + ".store.OutboxSchemaValidator",
            OUTBOX_PACKAGE + ".aop.TransactionalMessageMethodValidator",
            OUTBOX_PACKAGE + ".autoconfigure.OutboxConfigurationValidator",
            OUTBOX_PACKAGE + ".validation.OutboxMessageValidator");

    /** 算法第 6 行：旧 FQCN 在消费者、反射名称、测试与 README 中必须零残留。 */
    private static final List<String> RELOCATED_TYPES = COMPONENT_EXCEPTIONS.stream()
            .map(name -> OUTBOX_PACKAGE + ".exception." + name)
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

    private final List<String> storeCalls = new ArrayList<>();

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

        assertThat(commonException("OutboxException").getSuperclass())
                .as("REQ-003 技术异常根必须直接继承 CommonException")
                .isEqualTo(CommonException.class);
        for (String name : COMPONENT_EXCEPTIONS) {
            Class<?> type = commonException(name);
            assertThat(CommonException.class.isAssignableFrom(type))
                    .as("REQ-003 %s 必须以 CommonException 为祖先", name).isTrue();
            assertThat(type.getSuperclass().getSimpleName())
                    .as("REQ-003 %s 不得直接继承 RuntimeException", name)
                    .isIn("CommonException", "OutboxException");
        }
        // 组件没有业务规则拒绝语义，10 个异常都是技术失败，因此不得出现第二套异常根。
        assertThat(text(MAIN_SOURCES)).doesNotContain("extends RuntimeException");
    }

    // 算法第 1/2 行：cause、安全 message、retryable 与原构造器形状保持；继承的 getCode 仍是公共 int 合同。
    @Test
    void originalCausesMessagesAndRetryabilitySurviveTheNormalization() throws Exception {
        Class<?> rootType = commonException("OutboxException");
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

        IllegalStateException cause = new IllegalStateException("jdbc failure");
        for (String name : COMPONENT_EXCEPTIONS) {
            Class<?> type = commonException(name);
            CommonException withoutCause = (CommonException) type
                    .getConstructor(String.class).newInstance("context");
            assertThat(withoutCause.getMessage()).as("%s 必须保留原 message", name).isEqualTo("context");
            assertThat(withoutCause.getCause()).as("%s 单参构造不得凭空补 cause", name).isNull();

            Optional<Constructor<?>> twoArgument = Arrays.stream(type.getConstructors())
                    .filter(candidate -> candidate.getParameterCount() == 2).findFirst();
            if (MESSAGE_ONLY_EXCEPTIONS.contains(name)) {
                assertThat(twoArgument).as("%s 原本就没有 cause 构造器", name).isEmpty();
            } else {
                CommonException withCause = (CommonException) twoArgument.orElseThrow()
                        .newInstance("context", cause);
                assertThat(withCause.getCause()).as("%s 必须保留原 cause", name).isSameAs(cause);
                assertThat(withCause.getMessage()).as("%s 必须保留原 message", name).isEqualTo("context");
            }
            assertThat(List.of(type.getDeclaredMethods()))
                    .as("%s 不得自带 code/getStatus 影子 getter", name)
                    .noneMatch(method -> method.getName().equals("getCode")
                            || method.getName().equals("code")
                            || method.getName().equals("getStatus"));
        }
    }

    // 算法第 3 行：手写 enum 实现 EgonEnum，常量为显式字面量，SQL/线值仍取 name()。
    @Test
    void handwrittenEnumsImplementTheCommonEnumContract() throws Exception {
        List<String> headers = enumHeaders(MAIN_SOURCES);

        assertThat(headers).as("本模块手写 enum 只有三个").hasSize(3);
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

        // 原本就没有整数 code，因此按当前声明顺序固定 0..N-1，常量名与数据库线值不改。
        assertThat(OutboxStatus.values()).extracting(Enum::name).containsExactly(
                "PENDING", "PROCESSING", "RETRY_WAIT", "SUCCEEDED", "DEAD");
        assertThat(codes(OutboxStatus.class)).containsExactly(0, 1, 2, 3, 4);
        assertThat(invoke(OutboxStatus.class, OutboxStatus.DEAD, "getMessage"))
                .as("REQ-004 message 使用原常量名").isEqualTo("DEAD");
        assertThat(DeliveryResult.Kind.values()).extracting(Enum::name)
                .containsExactly("SUCCESS", "RETRYABLE_FAILURE", "PERMANENT_FAILURE");
        assertThat(codes(DeliveryResult.Kind.class)).containsExactly(0, 1, 2);
        assertThat(RabbitPublishOutcome.Kind.values()).extracting(Enum::name)
                .containsExactly("ACK", "NACK", "TIMEOUT", "RETURNED");
        assertThat(codes(RabbitPublishOutcome.Kind.class)).containsExactly(0, 1, 2, 3);

        // 状态列仍按常量名读写，SQL 字面量与 valueOf 解析保持原样。
        String store = read(MAIN_SOURCES.resolve(
                "top/egon/cola/component/outbox/store/PostgresqlJdbcOutboxStore.java"));
        assertThat(store).contains("'PENDING'", "'PROCESSING'", "'RETRY_WAIT'")
                .contains("OutboxStatus.valueOf(resultSet.getString(\"status\"))");
        assertThat(DeliveryResult.success().kind()).isEqualTo(DeliveryResult.Kind.SUCCESS);
        assertThat(RabbitPublishOutcome.ack().kind()).isEqualTo(RabbitPublishOutcome.Kind.ACK);
    }

    // 算法第 4 行：validator 继承 BaseValidator，null/range 走原生约束，协议/关系检查原样保留。
    @Test
    void validatorsExtendTheCommonContractAndKeepTheirProtocolRules() throws Exception {
        assertThat(declaredValidatorTypes())
                .as("本模块 validator 清单")
                .containsExactlyInAnyOrderElementsOf(VALIDATOR_TYPES);
        for (String type : VALIDATOR_TYPES) {
            Class<?> validatorType = Class.forName(type);
            assertThat(BaseValidator.class.isAssignableFrom(validatorType))
                    .as("REQ-007 %s 必须继承 BaseValidator", type).isTrue();
            assertThat(Arrays.stream(validatorType.getConstructors())
                    .anyMatch(candidate ->
                            Arrays.asList(candidate.getParameterTypes()).contains(ValidationUtils.class)))
                    .as("REQ-007 %s 必须注入具名公共 ValidationUtils", type).isTrue();
        }
        for (Path file : validatorSources()) {
            assertThat(read(file))
                    .as("%s 不得自建校验设施或私有 Validator", file)
                    .doesNotContain("buildDefaultValidatorFactory")
                    .doesNotContain("jakarta.validation");
        }

        OutboxConfigurationValidator configurationValidator =
                newValidator(OutboxConfigurationValidator.class);
        TransactionalOutboxProperties properties = new TransactionalOutboxProperties();
        assertThatCode(() -> configurationValidator.validate(properties)).doesNotThrowAnyException();
        assertThat(validatedBeans).as("原生约束必须先于关系检查").containsExactly(properties);

        properties.getDelivery().setLeaseDuration(Duration.ofSeconds(1));
        assertThatThrownBy(() -> configurationValidator.validate(properties))
                .hasMessage("Invalid transactional outbox configuration: delivery.lease-duration")
                .isInstanceOf(CommonException.class);

        validatedBeans.clear();
        OutboxMessageValidator messageValidator = newValidator(OutboxMessageValidator.class,
                new ObjectMapper(), 1024, 64, 1024);
        OutboxMessage envelope = OutboxMessage.builder()
                .channel("order-events").destination("order-created-v1").payload("body").build();
        assertThatCode(() -> messageValidator.validateEnvelope(envelope)).doesNotThrowAnyException();
        assertThat(validatedBeans).as("信封校验必须先跑原生约束").containsExactly(envelope);
        assertThatThrownBy(() -> messageValidator.validateEnvelope(OutboxMessage.builder()
                .channel("order-events").destination("order-created-v1")
                .payload("body").header("authorization", "secret").build()))
                .hasMessage("Outbox header 'authorization' is forbidden")
                .isInstanceOf(CommonException.class);
        assertThatThrownBy(() -> messageValidator.validateEnvelope(null))
                .hasMessage("Invalid outbox message").isInstanceOf(CommonException.class);
        assertThatThrownBy(() -> messageValidator.validateSerialized(
                new SerializedOutboxPayload("x".repeat(4096), 4096), Map.of()))
                .hasMessage("Outbox payload exceeds configured byte limit")
                .isInstanceOf(CommonException.class);

        TransactionalMessageMethodValidator methodValidator =
                newValidator(TransactionalMessageMethodValidator.class, "ordersTransactionManager");
        Method allowed = SampleTarget.class.getDeclaredMethod("publicBoundary");
        assertThat(methodValidator.validate(allowed, SampleTarget.class)).isEqualTo(allowed);
        assertThatThrownBy(() -> methodValidator.validate(
                SampleTarget.class.getDeclaredMethod("staticBoundary"), SampleTarget.class))
                .hasMessage("Invalid @TransactionalMessage method boundary")
                .isInstanceOf(CommonException.class);

        newValidator(OutboxSchemaValidator.class, recordingStore()).validate();
        assertThat(storeCalls).as("schema 校验只允许一次只读元数据检查")
                .containsExactly("validateSchema");
    }

    // 算法第 5 行：转换只服务真实 S/T 语义对；codec 与协议序列化不被机械改写，校验无外部副作用。
    @Test
    void conversionsStayOnTheCommonContractWithoutFakeConverters() throws Exception {
        String sources = text(MAIN_SOURCES);

        assertThat(sources).as("Rule 5 禁止 BeanUtils 复制").doesNotContain("BeanUtils");
        // 本模块只有载荷 codec，没有自有对象转换语义对，因此不得伪造 BaseConverter 实现。
        assertThat(sources).as("本模块没有自有对象转换需求").doesNotContain("BaseConverter");
        String codec = read(MAIN_SOURCES.resolve(
                "top/egon/cola/component/outbox/serialization/JacksonOutboxMessageSerializer.java"));
        assertThat(codec).as("codec 仍按原 Jackson 协议编解码载荷")
                .contains("objectMapper.writeValueAsString");
        assertThat(validatorText())
                .as("输入失败不得触发 DAO/缓存/MQ 副作用")
                .doesNotContain("JdbcTemplate")
                .doesNotContain("RabbitTemplate")
                .doesNotContain("RedisTemplate")
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
        Path packageInfo = MAIN_SOURCES.resolve(
                "top/egon/cola/component/outbox/common/exception/package-info.java");
        assertThat(packageInfo).as("REQ-027 所属包必须记录职责").exists();
        assertThat(read(packageInfo)).contains("package " + EXCEPTION_PACKAGE + ";");

        String pom = read(POM);
        assertThat(pom).as("MC-DEP-001 直接使用合同必须显式声明")
                .contains("egon-cola-component-common-core")
                .contains("spring-boot-starter-validation");
        String readme = read(Path.of("README.md"));
        assertThat(readme).as("File 57 只同步本 Step 落地的结构")
                .contains(EXCEPTION_PACKAGE)
                .contains("egonColaValidationUtils");
    }

    private static final Map<Class<?>, Class<?>> WRAPPERS = Map.of(
            int.class, Integer.class, long.class, Long.class, double.class, Double.class,
            float.class, Float.class, short.class, Short.class, byte.class, Byte.class,
            boolean.class, Boolean.class, char.class, Character.class);

    /** 兼容本 Step 前后的构造器形状：合同参数按类型补齐，其余参数按原顺序消费。 */
    private <T> T newValidator(Class<T> type, Object... known) throws Exception {
        for (Constructor<?> candidate : type.getConstructors()) {
            Class<?>[] parameters = candidate.getParameterTypes();
            Object[] arguments = new Object[parameters.length];
            int cursor = 0;
            boolean assembled = true;
            for (int index = 0; index < parameters.length && assembled; index++) {
                if (cursor < known.length && matches(parameters[index], known[cursor])) {
                    arguments[index] = known[cursor++];
                } else if (parameters[index].equals(ValidationUtils.class)) {
                    arguments[index] = recordingValidationUtils();
                } else {
                    assembled = false;
                }
            }
            if (assembled && cursor == known.length) {
                return type.cast(candidate.newInstance(arguments));
            }
        }
        return fail("REQ-007 " + type.getSimpleName() + " 缺少可组装的公共校验合同构造器");
    }

    private static boolean matches(Class<?> parameter, Object value) {
        Class<?> expected = WRAPPERS.getOrDefault(parameter, parameter);
        return value == null ? !parameter.isPrimitive() : expected.isInstance(value);
    }

    private ValidationUtils recordingValidationUtils() {
        Validator validator = (Validator) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{Validator.class},
                (proxy, method, arguments) -> {
                    if ("validate".equals(method.getName())
                            && arguments != null && arguments.length > 0) {
                        validatedBeans.add(arguments[0]);
                        return Set.of();
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        return new ValidationUtils(validator);
    }

    private OutboxStore recordingStore() {
        return (OutboxStore) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{OutboxStore.class},
                (proxy, method, arguments) -> {
                    storeCalls.add(method.getName());
                    Class<?> returns = method.getReturnType();
                    if (returns == boolean.class) {
                        return false;
                    }
                    if (returns == int.class) {
                        return 0;
                    }
                    return returns == long.class ? 0L : null;
                });
    }

    private static List<Path> validatorSources() {
        return VALIDATOR_TYPES.stream()
                .map(type -> MAIN_SOURCES.resolve(type.replace('.', '/') + ".java"))
                .toList();
    }

    private static String validatorText() throws IOException {
        StringBuilder builder = new StringBuilder();
        for (Path file : validatorSources()) {
            builder.append(read(file)).append('\n');
        }
        return builder.toString();
    }

    static final class SampleTarget {

        public void publicBoundary() {
        }

        static void staticBoundary() {
        }
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
