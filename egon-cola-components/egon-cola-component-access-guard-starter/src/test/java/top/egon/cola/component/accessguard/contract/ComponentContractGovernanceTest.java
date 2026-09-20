package top.egon.cola.component.accessguard.contract;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.springframework.util.ClassUtils;
import top.egon.cola.component.accessguard.core.GuardDecision;
import top.egon.cola.component.accessguard.core.GuardEntryType;
import top.egon.cola.component.accessguard.core.GuardOutcome;
import top.egon.cola.component.accessguard.core.GuardResolution;
import top.egon.cola.component.accessguard.core.failure.FailurePoint;
import top.egon.cola.component.accessguard.core.failure.FailurePolicy;
import top.egon.cola.component.accessguard.core.plan.AdmissionConfig;
import top.egon.cola.component.accessguard.core.plan.ExecutionConfig;
import top.egon.cola.component.accessguard.core.plan.FailurePolicies;
import top.egon.cola.component.accessguard.core.plan.GuardPlan;
import top.egon.cola.component.accessguard.core.plan.GuardPlanSnapshot;
import top.egon.cola.component.accessguard.core.plan.GuardPlanValidator;
import top.egon.cola.component.accessguard.core.plan.KeyConfig;
import top.egon.cola.component.accessguard.core.plan.ObservabilityConfig;
import top.egon.cola.component.accessguard.execution.RejectionMode;
import top.egon.cola.component.accessguard.execution.TimeLimitMode;
import top.egon.cola.component.accessguard.execution.TimeLimiterType;
import top.egon.cola.component.accessguard.policy.GuardPolicyType;
import top.egon.cola.component.accessguard.policy.allow.AllowListMode;
import top.egon.cola.component.common.core.enums.ErrorStatus;
import top.egon.cola.component.common.core.enums.EgonEnum;
import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.BusinessException;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.validation.BaseValidator;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Step 5 合同：Access Guard 的自定义异常、手写 enum、validator 与转换必须落在公共合同上
 * （REQ-002、REQ-003、REQ-004、REQ-005、REQ-006、REQ-007、REQ-027）。
 *
 * <p>本文件只固定本 Step 列出的六条算法差异；线格式、注解值与既有业务判定由原回归证明保持。</p>
 */
class ComponentContractGovernanceTest {

    private static final Path MAIN_SOURCES = Path.of("src/main/java");

    private static final Path RPC_MAIN_SOURCES =
            Path.of("../egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java");

    private static final String EXCEPTION_PACKAGE =
            "top.egon.cola.component.accessguard.common.exception";

    /** 算法第 6 行：旧 FQCN 在消费者、反射名称、测试与 README 中必须零残留。 */
    private static final List<String> RELOCATED_TYPES = List.of(
            "top.egon.cola.component.accessguard.api.AccessGuardRejectedException",
            "top.egon.cola.component.accessguard.execution.ExecutorRejectedException",
            "top.egon.cola.component.accessguard.execution.TimeLimitExceededException",
            "top.egon.cola.component.accessguard.key.GuardKeyResolutionException",
            "top.egon.cola.component.accessguard.store.StoreOperationException");

    private static final Path[] GOVERNED_TEXT_ROOTS = {
            MAIN_SOURCES,
            Path.of("src/test/java"),
            RPC_MAIN_SOURCES,
            Path.of("../egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java"),
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

    // 算法第 1 行：自定义异常只在所属 common.exception，祖先链包含规范根。
    @Test
    void customExceptionsAreOwnedByCommonExceptionAndTheNormalizedRoot() throws Exception {
        Map<String, String> declared = declaredExceptions();

        assertThat(declared).containsKeys(
                "AccessGuardRejectedException", "ExecutorRejectedException",
                "TimeLimitExceededException", "GuardKeyResolutionException",
                "StoreOperationException", "GuardOperationException");

        List<String> wrongPackage = new ArrayList<>();
        declared.forEach((name, packageName) -> {
            if (!EXCEPTION_PACKAGE.equals(packageName)) {
                wrongPackage.add(name + " -> " + packageName);
            }
        });
        assertThat(wrongPackage).as("REQ-002 异常只在所属 common.exception").isEmpty();

        for (String name : declared.keySet()) {
            Class<?> type = commonException(name);
            assertThat(CommonException.class.isAssignableFrom(type))
                    .as("REQ-003 %s 必须以 CommonException 为祖先", name).isTrue();
            assertThat(type.getSuperclass().getSimpleName())
                    .as("REQ-003 %s 直接继承公共根", name)
                    .isIn("CommonException", "BusinessException");
        }

        // 线程与虚拟线程两处包装语义一致，因此只保留一个组件公共异常。
        assertThat(text(MAIN_SOURCES))
                .doesNotContain("class GuardOperationException extends RuntimeException");
    }

    // 算法第 2 行：旧 String code 进入 getStatus，继承的 getCode 保持公共 int 合同。
    @Test
    void legacyStringCodesAreCarriedByTheCommonStatusGetter() throws Exception {
        Class<?> rejectedType = commonException("AccessGuardRejectedException");
        GuardOutcome outcome = GuardOutcome.rejected(
                "rpc.order.create", GuardDecision.RATE_LIMITED, "rate-limit", 1L);
        CommonException rejection =
                (CommonException) rejectedType.getConstructor(GuardOutcome.class).newInstance(outcome);

        assertThat(BusinessException.class.isAssignableFrom(rejectedType))
                .as("守卫拒绝是业务规则失败").isTrue();
        assertThat(rejection.getStatus()).isEqualTo("ACCESS_GUARD_REJECTED");
        assertThat(rejection.getMessage())
                .isEqualTo("Access Guard rejected rule=rpc.order.create, decision=RATE_LIMITED, "
                        + "resolution=THROWN");
        assertThat(rejection.getCode()).isEqualTo(ResultCode.FORBIDDEN.getCode());
        assertThat(invoke(rejectedType, rejection, "outcome")).isSameAs(outcome);
        assertThat(List.of(rejectedType.getDeclaredMethods()))
                .as("int getCode 不得被子类覆盖成 String/enum")
                .noneMatch(method -> method.getName().equals("getCode")
                        || method.getName().equals("code"));

        Class<?> keyFailureType = commonException("GuardKeyResolutionException");
        CommonException keyFailure = (CommonException) keyFailureType.getConstructor(String.class)
                .newInstance("REQUIRED_PART_MISSING");
        assertThat(keyFailure.getStatus()).isEqualTo("REQUIRED_PART_MISSING");
        assertThat(keyFailure.getMessage())
                .isEqualTo("Access Guard key resolution failed: REQUIRED_PART_MISSING");
        assertThat(List.of(keyFailureType.getDeclaredMethods()))
                .noneMatch(method -> method.getName().equals("code"));

        Class<?> storeType = commonException("StoreOperationException");
        IOException storeCause = new IOException("redis");
        CommonException storeFailure = (CommonException) storeType
                .getConstructor(String.class, Throwable.class).newInstance("LIST_STORE_FAILED", storeCause);
        assertThat(storeFailure.getStatus()).isEqualTo("LIST_STORE_FAILED");
        assertThat(storeFailure.getMessage()).isEqualTo("LIST_STORE_FAILED");
        assertThat(storeFailure.getCause()).isSameAs(storeCause);
        assertThat(storeFailure.isRetryable()).as("原异常没有重试声明").isFalse();
    }

    // 算法第 1/2 行：cause、安全 message 与既有诊断字段全部保持。
    @Test
    void originalCausesMessagesAndDiagnosticFieldsSurviveTheNormalization() throws Exception {
        IllegalStateException cause = new IllegalStateException("TimeLimiter is closed");
        CommonException executorRejected = (CommonException) commonException("ExecutorRejectedException")
                .getConstructor(Throwable.class).newInstance(cause);
        assertThat(executorRejected.getMessage())
                .isEqualTo("Access Guard executor rejected the operation");
        assertThat(executorRejected.getCause()).isSameAs(cause);
        assertThat(executorRejected.isRetryable()).isFalse();

        Duration timeout = Duration.ofMillis(1500);
        Class<?> timeoutType = commonException("TimeLimitExceededException");
        CommonException exceeded =
                (CommonException) timeoutType.getConstructor(Duration.class).newInstance(timeout);
        assertThat(exceeded.getMessage()).isEqualTo("Access Guard execution exceeded PT1.5S");
        assertThat(invoke(timeoutType, exceeded, "timeout")).isEqualTo(timeout);

        RuntimeException guardedCause = new RuntimeException("worker failure");
        Class<?> guardedType = commonException("GuardOperationException");
        CommonException guardedOperation =
                (CommonException) guardedType.getConstructor(Throwable.class).newInstance(guardedCause);
        assertThat(guardedOperation.getCause()).isSameAs(guardedCause);
        // 解包语义依赖 cause：包装异常只携带 cause，不吞掉原类型。
        assertThat(guardedType.isInstance(guardedOperation)).isTrue();
    }

    // 算法第 3 行：手写 enum 实现 EgonEnum/ErrorStatus，显式声明固定 code，禁止 ordinal 编码。
    @Test
    void handwrittenEnumsImplementTheCommonEnumContract() throws Exception {
        List<String> headers = enumHeaders(MAIN_SOURCES);

        assertThat(headers).as("本模块手写 enum 清单").hasSize(19);
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

        // 已有整数 code 不变；无整数 code 的按当前声明顺序固定 0..N-1；常量名与原线值保持。
        assertThat(codes(GuardPolicyType.class)).containsExactly(0, 1, 2, 3);
        assertThat(codes(GuardResolution.class)).containsExactly(0, 1, 2, 3, 4, 5, 6);
        assertThat(codes(GuardDecision.class)).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
        assertThat(codes(AdmissionConfig.RateLimitAlgorithm.class)).containsExactly(0, 1, 2);
        assertThat(codes(FailurePoint.class)).containsExactly(0, 1, 2, 3, 4, 5, 6);
        assertThat(GuardEntryType.values()).extracting(GuardEntryType::name)
                .containsExactly("AOP", "PROGRAMMATIC");
        assertThat(invoke(GuardEntryType.class, GuardEntryType.AOP, "getMessage"))
                .as("REQ-004 message 使用原常量名").isEqualTo("AOP");
        assertThat(GuardPolicyType.DENY_LIST.id()).as("原协议字符串 id 不变").isEqualTo("deny-list");
    }

    // 算法第 4 行：validator 继承公共扩展合同，关系与协议检查保持，输入失败不触发业务副作用。
    @Test
    void validatorsExtendTheCommonContractAndKeepTheirRelationRules() throws Exception {
        assertThat(BaseValidator.class.isAssignableFrom(GuardPlanValidator.class))
                .as("REQ-007 GuardPlanValidator 必须继承 BaseValidator").isTrue();
        assertThat(BaseValidator.class.isAssignableFrom(Class.forName(
                "top.egon.cola.component.accessguard.autoconfigure.AccessGuardStartupValidator")))
                .as("REQ-007 AccessGuardStartupValidator 必须继承 BaseValidator").isTrue();

        GuardPlanValidator validator = newGuardPlanValidator();
        assertThatCode(() -> validator.validate(
                snapshot(AdmissionConfig.RateLimitAlgorithm.SLIDING_WINDOW, 100, 1)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> validator.validate(
                snapshot(AdmissionConfig.RateLimitAlgorithm.SLIDING_WINDOW, 100, 2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SLIDING_WINDOW requires requestedTokens=1");
        assertThatThrownBy(() -> validator.validate(new GuardPlanSnapshot(
                "other", 1L, Instant.EPOCH, "test",
                snapshot(AdmissionConfig.RateLimitAlgorithm.TOKEN_BUCKET, 100, 1).plan(), "fingerprint")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("snapshot ruleId must match plan id");
    }

    // 算法第 5 行：转换只服务真实 S/T 语义对；codec 与框架 ExceptionMapper 不被机械改写。
    @Test
    void conversionsStayOnTheCommonContractWithoutFakeMappers() throws Exception {
        String sources = text(MAIN_SOURCES);

        assertThat(sources).as("Rule 5 禁止 BeanUtils 复制").doesNotContain("BeanUtils");
        // 本模块没有自有对象转换需求时不得伪造 BaseConverter 实现。
        assertThat(sources).doesNotContain("BaseConverter");
        assertThat(RPC_MAIN_SOURCES).isDirectory();
        String mapper = read(RPC_MAIN_SOURCES.resolve(
                "top/egon/cola/component/rpc/provider/server/RpcAccessGuardExceptionMapper.java"));
        assertThat(mapper).as("框架 ExceptionMapper 保持框架合同")
                .contains("implements RpcProviderExceptionMapper");
        assertThat(mapper).doesNotContain("BaseConverter");
    }

    // 算法第 6 行：API 消费者、反射名称与 README 同一步更新，旧路径零残留。
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
        String rpcAssembly = read(RPC_MAIN_SOURCES.resolve(
                "top/egon/cola/component/rpc/config/RpcAccessGuardAutoConfiguration.java"));
        assertThat(rpcAssembly).contains(EXCEPTION_PACKAGE + ".AccessGuardRejectedException");
    }

    private static GuardPlanValidator newGuardPlanValidator() throws Exception {
        Constructor<?> constructor = GuardPlanValidator.class.getDeclaredConstructors()[0];
        if (constructor.getParameterCount() == 0) {
            return (GuardPlanValidator) constructor.newInstance();
        }
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            return (GuardPlanValidator) constructor.newInstance(
                    new ValidationUtils(factory.getValidator()));
        }
    }

    private static GuardPlanSnapshot snapshot(
            AdmissionConfig.RateLimitAlgorithm algorithm, long capacity, long requestedTokens) {
        GuardPlan plan = new GuardPlan(
                "draw",
                true,
                new KeyConfig(List.of("GLOBAL"), List.of(), "secret"),
                new AdmissionConfig(
                        new AdmissionConfig.DenyListConfig(false),
                        new AdmissionConfig.AllowListConfig(false, AllowListMode.GATE),
                        new AdmissionConfig.PenaltyBoxConfig(
                                false, 1, Duration.ofSeconds(1), Duration.ofSeconds(1)),
                        new AdmissionConfig.RateLimitConfig(
                                true, algorithm, capacity, 1, Duration.ofSeconds(1), requestedTokens)),
                new ExecutionConfig(
                        new ExecutionConfig.TimeLimitConfig(false, TimeLimitMode.DISABLED,
                                TimeLimiterType.CALLER_THREAD, Duration.ofSeconds(1), true),
                        new ExecutionConfig.RejectionConfig(RejectionMode.THROW, "", "")),
                FailurePolicies.uniform(FailurePolicy.FAIL_CLOSED),
                ObservabilityConfig.defaults(),
                "state-v1");
        return new GuardPlanSnapshot("draw", 1L, Instant.EPOCH, "test", plan, "fingerprint");
    }

    private static Class<?> commonException(String simpleName) throws ClassNotFoundException {
        String type = EXCEPTION_PACKAGE + "." + simpleName;
        assertThat(ClassUtils.isPresent(type, ComponentContractGovernanceTest.class.getClassLoader()))
                .as("REQ-002 %s 必须位于 %s", simpleName, EXCEPTION_PACKAGE)
                .isTrue();
        return Class.forName(type);
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

    private static List<String> enumHeaders(Path root) throws IOException {
        List<String> headers = new ArrayList<>();
        for (Path file : sources(root)) {
            for (String line : Files.readAllLines(file)) {
                Matcher matcher = ENUM_HEADER.matcher(line);
                if (matcher.find()) {
                    headers.add(matcher.group(1) + matcher.group(2));
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
