package top.egon.cola.component.rpc.contract;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.core.enums.EgonEnum;
import top.egon.cola.component.common.core.enums.ErrorStatus;
import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.validation.BaseValidator;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.rpc.annotation.FailStrategy;
import top.egon.cola.component.rpc.annotation.LoadBalance;
import top.egon.cola.component.rpc.consumer.gateway.RpcGatewayState;
import top.egon.cola.component.rpc.consumer.invocation.RpcInvocationMode;
import top.egon.cola.component.rpc.consumer.lifecycle.RpcConsumerRuntimeState;
import top.egon.cola.component.rpc.consumer.reference.RpcReferenceMode;
import top.egon.cola.component.rpc.context.invocation.RpcFailureStage;
import top.egon.cola.component.rpc.contract.descriptor.RpcType;
import top.egon.cola.component.rpc.provider.lifecycle.RpcProviderRuntimeState;
import top.egon.cola.component.rpc.provider.registration.RpcLeaseOperationResult;
import top.egon.cola.component.rpc.provider.registration.RpcProviderRegistrationMode;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Step 10 合同：RPC 的自定义异常、typed code、手写 enum 与 validator 必须落在公共合同上
 * （REQ-002、REQ-003、REQ-004、REQ-005、REQ-006、REQ-007、REQ-027）。
 *
 * <p>本文件只固定本 Step 列出的六条算法差异；Channel 生命周期、租约与 gRPC 状态映射行为由原回归证明保持。</p>
 */
class ComponentContractGovernanceTest {

    private static final Path MAIN_SOURCES = Path.of("src/main/java");

    private static final Path POM = Path.of("pom.xml");

    private static final Path README = Path.of("../README.md");

    private static final Path TIANSHU = Path.of("../egon-cola-component-rpc-tianshu-adapter");

    private static final Path TIANSHU_SOURCES = TIANSHU.resolve("src/main/java");

    private static final Path TIANSHU_POM = TIANSHU.resolve("pom.xml");

    private static final Path TIANSHU_SECURITY_ENUM = TIANSHU_SOURCES.resolve(
            "top/egon/cola/component/rpc/tianshu/security/DdcRpcOperation.java");

    private static final String RPC_PACKAGE = "top.egon.cola.component.rpc";

    private static final String EXCEPTION_PACKAGE = RPC_PACKAGE + ".common.exception";

    private static final String ENUM_PACKAGE = RPC_PACKAGE + ".common.enums";

    private static final String LEGACY_EXCEPTION_PACKAGE = RPC_PACKAGE + ".exception";

    /** 相邻模块的 enum 不在本模块测试类路径上，固定编码改由源码解析。 */
    private static final String TIANSHU_PACKAGE = RPC_PACKAGE + ".tianshu";

    private static final String CANONICAL_FACADE_BEAN = "egonColaValidationUtils";

    private static final String FRAMEWORK_MAPPER = LEGACY_EXCEPTION_PACKAGE + ".RpcStatusExceptionMapper";

    private static final List<String> COMPONENT_EXCEPTIONS = List.of(
            "EgonRpcException", "EgonRpcRejectedException");

    private static final String ERROR_CODE_TYPE = ENUM_PACKAGE + ".EgonRpcErrorCode";

    private static final List<String> VALIDATOR_TYPES = List.of(
            RPC_PACKAGE + ".contract.validation.RpcContractValidator");

    /** 算法第 6 行：旧 FQCN 在本模块、相邻适配器与家庭消费者中必须零残留。 */
    private static final List<String> RELOCATED_TYPES = List.of(
            LEGACY_EXCEPTION_PACKAGE + ".EgonRpcException",
            LEGACY_EXCEPTION_PACKAGE + ".EgonRpcRejectedException",
            LEGACY_EXCEPTION_PACKAGE + ".EgonRpcErrorCode");

    private static final Path[] GOVERNED_TEXT_ROOTS = {MAIN_SOURCES, Path.of("src/test/java"), POM};

    private static final Path[] ADJACENT_MODULE_ROOTS = {
            TIANSHU_SOURCES,
            TIANSHU.resolve("src/test/java"),
            TIANSHU_POM};

    private static final Path REPO_ROOT = Path.of("../../../");

    private static final Path[] FAMILY_TEXT_ROOTS = {
            repo("egon-cola-archetypes/egon-cola-evaluation-facade/src"),
            repo("egon-cola-archetypes/egon-cola-organization-facade/src"),
            repo("egon-cola-archetypes/source-projects/egon-cola-source-light/src"),
            repo("egon-cola-archetypes/source-projects/egon-cola-source-service"),
            repo("egon-cola-archetypes/source-projects/egon-cola-source-web"),
            repo("egon-cola-xingyuan/egon-cola-tianquan-shoubing/egon-cola-tianquan-shoubing-rpc-contract/src")};

    /** 本模块 13 个手写 enum 的固定编码；已有数字保持，无编码常量按当前顺序落 0..N-1。 */
    private static final Map<String, List<String>> ENUM_CODES = enumCodes();

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
    private static final Pattern SHARED_VALIDATOR_FACTORY = Pattern.compile(
            "static\\s+final\\s+ValidationUtils\\s+\\w+\\s*=\\s*new ValidationUtils\\(\\s*"
                    + "Validation\\.buildDefaultValidatorFactory\\(\\)\\.getValidator\\(\\)\\s*\\)");

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

        Class<?> root = componentException("EgonRpcException");
        assertThat(root.getSuperclass())
                .as("REQ-002 技术异常根必须继承 CommonException")
                .isEqualTo(CommonException.class);
        assertThat(componentException("EgonRpcRejectedException").getSuperclass())
                .as("REQ-002 派生异常继续挂在组件根上")
                .isEqualTo(root);
        assertThat(text(MAIN_SOURCES))
                .as("REQ-002 不得再直接继承 RuntimeException")
                .doesNotContain("extends RuntimeException");

        List<String> remaining = directoryNames(MAIN_SOURCES.resolve(
                "top/egon/cola/component/rpc/exception"));
        assertThat(remaining)
                .as("REQ-002 旧包只保留框架状态映射器，异常与 code 类型必须迁出")
                .containsExactly("RpcStatusExceptionMapper.java");
    }

    // 算法第 2 行：typed code 由公共 status 与具名 getter 同时承载，原 message/cause 不改。
    @Test
    void typedCodesBecomeTheCommonStatusAndKeepMessageAndCause() throws Exception {
        Class<?> root = componentException("EgonRpcException");
        Class<?> codeType = Class.forName(ERROR_CODE_TYPE);
        Object rateLimited = constant(codeType, "RPC_RATE_LIMITED");

        assertThat(root.getMethod("getCode").getReturnType())
                .as("REQ-003 继承的 int code 不得被 enum 覆盖")
                .isEqualTo(int.class);
        assertThat(Arrays.stream(root.getMethods())
                .filter(method -> method.getName().equals("getRpcErrorCode"))
                .map(Method::getReturnType).findFirst().orElse(null))
                .as("REQ-003 原 typed code 必须由具名 getter 暴露")
                .isEqualTo(codeType);

        Constructor<?> messageConstructor = root.getConstructor(codeType, String.class);
        CommonException plain = (CommonException) messageConstructor
                .newInstance(rateLimited, "RPC request was rate limited");
        assertThat(plain.getCode())
                .as("REQ-003 归一化后 code 仍是公共 int 合同")
                .isEqualTo(ResultCode.SYSTEM_ERROR.getCode());
        assertThat(plain.getStatus())
                .as("REQ-003 旧 typed code 必须由公共 status 承载")
                .isEqualTo("RPC_RATE_LIMITED");
        assertThat(plain.getMessage()).isEqualTo("RPC request was rate limited");
        assertThat(plain.isRetryable()).isFalse();
        assertThat(plain.getCause()).isNull();
        assertThat(root.getMethod("getRpcErrorCode").invoke(plain))
                .as("REQ-003 typed code 保持同一枚举常量身份")
                .isSameAs(rateLimited);

        RuntimeException origin = new RuntimeException("origin");
        CommonException rooted = (CommonException) root
                .getConstructor(codeType, String.class, Throwable.class)
                .newInstance(rateLimited, "RPC request was rate limited", origin);
        assertThat(rooted.getCause()).as("REQ-003 原 cause 必须保持同一实例").isSameAs(origin);

        Class<?> rejected = componentException("EgonRpcRejectedException");
        assertThat(rejected.getConstructors())
                .as("EgonRpcRejectedException 只有单消息构造器")
                .hasSize(1);
        CommonException rejection = (CommonException) rejected
                .getConstructor(String.class).newInstance("RPC Provider rejected the request");
        assertThat(rejection.getStatus()).isEqualTo("RPC_PROVIDER_REJECTED");
        assertThat(rejection.getCode()).isEqualTo(ResultCode.SYSTEM_ERROR.getCode());
        assertThat(root.getMethod("getRpcErrorCode").invoke(rejection))
                .as("REQ-003 派生异常继续携带父类的 typed code")
                .isSameAs(constant(codeType, "RPC_PROVIDER_REJECTED"));

        List<String> shadowed = new ArrayList<>();
        for (String name : COMPONENT_EXCEPTIONS) {
            Class<?> type = componentException(name);
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
                .as("REQ-003 不得遮蔽公共 int code/status/retryable，也不得保留旧 typed code 字段")
                .isEmpty();

        String sources = text(MAIN_SOURCES);
        assertThat(sources)
                .as("REQ-003 typed 消费者必须改用具名 getter")
                .doesNotContain("getCode() == EgonRpcErrorCode")
                .doesNotContain(".getCode()).isEqualTo(EgonRpcErrorCode")
                .contains("getRpcErrorCode()");
    }

    // 算法第 3 行：手写 enum 实现 EgonEnum，常量为显式字面量，既有名称与 wire 表示不改。
    @Test
    void handwrittenEnumsImplementTheCommonEnumContract() throws Exception {
        List<String> headers = enumHeaders(MAIN_SOURCES);
        headers.addAll(enumHeaders(TIANSHU_SOURCES));

        assertThat(headers).as("本 Step 手写 enum 清单").containsExactlyInAnyOrderElementsOf(
                ENUM_CODES.keySet().stream().map(name -> enumSimpleName(name) + " implements EgonEnum").toList());
        List<String> missingContract = headers.stream()
                .filter(header -> !header.contains(EgonEnum.class.getSimpleName())
                        && !header.contains(ErrorStatus.class.getSimpleName()))
                .toList();
        assertThat(missingContract).as("REQ-004 手写 enum 必须实现 EgonEnum 或 ErrorStatus").isEmpty();
        assertThat(enumConstantsWithoutExplicitCode(MAIN_SOURCES))
                .as("code 必须是显式字面量，不能来自 ordinal()")
                .isEmpty();
        assertThat(enumConstantsWithoutExplicitCode(TIANSHU_SOURCES))
                .as("code 必须是显式字面量，不能来自 ordinal()")
                .isEmpty();
        assertThat(text(MAIN_SOURCES) + text(TIANSHU_SOURCES)).doesNotContain("ordinal()");

        for (Map.Entry<String, List<String>> entry : ENUM_CODES.entrySet()) {
            List<String> assignments = entry.getKey().startsWith(TIANSHU_PACKAGE)
                    ? sourceCodeAssignments(TIANSHU_SOURCES, enumSimpleName(entry.getKey()))
                    : codeAssignments(Class.forName(entry.getKey()));
            assertThat(assignments)
                    .as("REQ-004 %s 的固定编码与顺序保持", simpleName(entry.getKey()))
                    .containsExactlyElementsOf(entry.getValue());
        }

        // 原 wire/name 语义不变：注解仍按常量名解析，元数据仍按原字符串出网。
        assertThat(RpcFailureStage.valueOf("GATEWAY").wireValue()).isEqualTo("yuheng");
        assertThat(RpcFailureStage.valueOf("PROVIDER").wireValue()).isEqualTo("provider");
        assertThat(RpcFailureStage.from(null)).isEmpty();
        assertThat(RpcConsumerRuntimeState.READY.accepting()).isTrue();
        assertThat(RpcConsumerRuntimeState.DRAINING.accepting()).isFalse();
        assertThat(RpcProviderRuntimeState.DEGRADED.servingNewCalls()).isTrue();
        assertThat(RpcProviderRuntimeState.FAILED.servingNewCalls()).isFalse();
        assertThat(read(TIANSHU_SECURITY_ENUM))
                .as("REQ-004 管理面判定语义保持，不改成按 code 分支")
                .contains("public boolean management()")
                .contains("name().startsWith(\"MANAGEMENT_\")");
        assertThat(FailStrategy.valueOf("INHERIT")).isSameAs(FailStrategy.INHERIT);
        assertThat(LoadBalance.valueOf("SMOOTH_WEIGHTED_ROUND_ROBIN"))
                .isSameAs(LoadBalance.SMOOTH_WEIGHTED_ROUND_ROBIN);
        assertThat(RpcLeaseOperationResult.renewed(java.time.Instant.EPOCH).renewed()).isTrue();
        assertThat(RpcLeaseOperationResult.leaseMismatch().status())
                .isSameAs(RpcLeaseOperationResult.Status.LEASE_MISMATCH);
        assertThat(RpcGatewayState.values()).hasSize(5);
        assertThat(RpcInvocationMode.values()).containsExactly(
                RpcInvocationMode.BLOCKING, RpcInvocationMode.ASYNC);
        assertThat(RpcReferenceMode.values()).containsExactly(
                RpcReferenceMode.GATEWAY, RpcReferenceMode.DIRECT);
        assertThat(RpcType.values()).containsExactly(RpcType.UNARY);
        assertThat(RpcProviderRegistrationMode.values()).containsExactly(
                RpcProviderRegistrationMode.REQUIRED, RpcProviderRegistrationMode.DISABLED);
    }

    // 算法第 4 行：validator 继承 BaseValidator，原生约束先于协议关系规则，且只注入具名门面。
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

        for (Path configuration : List.of(
                MAIN_SOURCES.resolve("top/egon/cola/component/rpc/config/EgonRpcAutoConfig.java"),
                MAIN_SOURCES.resolve(
                        "top/egon/cola/component/rpc/config/RpcAccessGuardAutoConfiguration.java"),
                TIANSHU_SOURCES.resolve(
                        "top/egon/cola/component/rpc/tianshu/autoconfigure/DdcRpcAutoConfiguration.java"))) {
            String content = read(configuration);
            assertThat(content)
                    .as("REQ-007 %s 必须用公共规范 Bean 名发布门面", configuration)
                    .contains("@Bean(name = \"" + CANONICAL_FACADE_BEAN + "\")")
                    .contains("@ConditionalOnMissingBean(name = \"" + CANONICAL_FACADE_BEAN + "\")")
                    .contains("ValidationAutoConfiguration.class");
            assertThat(countOccurrences(content, "new ValidationUtils("))
                    .as("%s 只包装一次 Validator", configuration).isEqualTo(1);
        }

        for (Path file : validatorSources()) {
            assertThat(read(file))
                    .as("%s 不得自建校验设施", file)
                    .doesNotContain("ValidatorFactory")
                    .doesNotContain("new ValidationUtils(")
                    .contains("validateBean(");
        }
        assertThat(validatorText())
                .as("输入失败不得触发 DAO/缓存/MQ 副作用")
                .doesNotContain("JdbcTemplate")
                .doesNotContain("RedisTemplate")
                .doesNotContain("RabbitTemplate")
                .doesNotContain("KafkaTemplate")
                .doesNotContain("RedissonClient");

        // 非 Spring 编程式入口仍可用，但进程级门面只能绑定一次，不得按请求重建 ValidatorFactory。
        String sharedDefaults = programmaticDefaults();
        assertThat(SHARED_VALIDATOR_FACTORY.matcher(sharedDefaults).find())
                .as("REQ-007 编程式默认门面必须是进程内绑定一次的 static final")
                .isTrue();

        Class<?> validatorType = Class.forName(VALIDATOR_TYPES.get(0));
        Object validator = validatorType
                .getConstructor(ValidationUtils.class).newInstance(recordingValidationUtils());
        Method validate = validatorType.getMethod("validate", Class.class);
        assertThatThrownBy(() -> validate.invoke(validator, UnannotatedContract.class))
                .as("关系规则保持原 typed code 与安全消息")
                .hasRootCauseInstanceOf(
                        Class.forName(EXCEPTION_PACKAGE + ".EgonRpcException").asSubclass(Throwable.class))
                .cause().hasMessage("RPC contract is missing @EgonRpcService");
        assertThat(validatedBeans)
                .as("原生约束必须先于协议关系检查")
                .containsExactly(UnannotatedContract.class);

        Object realValidator = validatorType.getConstructor(ValidationUtils.class)
                .newInstance(realValidationUtils());
        assertThatThrownBy(() -> validate.invoke(realValidator, (Object) null))
                .as("空契约必须由公共门面的原生存在规则拒绝")
                .rootCause().isInstanceOf(IllegalArgumentException.class)
                .hasMessage("target must not be null");
        assertThatThrownBy(() -> validate.invoke(validator, String.class))
                .as("非接口契约继续由关系规则拒绝且消息保持")
                .rootCause().isInstanceOf(CommonException.class)
                .hasMessage("RPC contract must be an interface");
        assertThat(validatedBeans)
                .as("关系规则失败前原生约束已经运行过")
                .containsExactly(UnannotatedContract.class, String.class);
    }

    // 算法第 5 行：本模块没有真实 S/T 语义对，框架状态映射器不得被机械转换。
    @Test
    void conversionsStayOnTheCommonContractWithoutFakeConverters() throws Exception {
        String sources = text(MAIN_SOURCES);

        assertThat(sources).as("Rule 5 禁止 BeanUtils 复制").doesNotContain("BeanUtils");
        assertThat(sources).as("本模块没有自有对象转换语义对").doesNotContain("BaseConverter");
        assertThat(sources).as("gRPC/Protobuf 由框架类型直接承载，不引入映射库").doesNotContain("org.mapstruct");
        assertThat(MAIN_SOURCES.resolve("top/egon/cola/component/rpc/contract/validation"))
                .as("REQ-005 不新增伪 converter 包").exists();
        assertThat(FRAMEWORK_MAPPER)
                .as("REQ-005 框架 ExceptionMapper 保持框架合同，不机械继承公共映射")
                .isEqualTo(LEGACY_EXCEPTION_PACKAGE + ".RpcStatusExceptionMapper");
        assertThat(read(MAIN_SOURCES.resolve(
                "top/egon/cola/component/rpc/exception/RpcStatusExceptionMapper.java")))
                .as("REQ-005 状态映射器仍返回原 typed 异常")
                .contains("public EgonRpcException map(StatusRuntimeException exception)")
                .contains("import " + EXCEPTION_PACKAGE + ".EgonRpcException;")
                .contains("import " + ERROR_CODE_TYPE + ";");
    }

    // 算法第 6 行：模块、相邻适配器、家庭消费者与 README 同一步更新，旧路径零残留。
    @Test
    void consumersAndDocsDropTheLegacyLocations() throws Exception {
        List<Path> roots = new ArrayList<>(List.of(GOVERNED_TEXT_ROOTS));
        roots.addAll(List.of(ADJACENT_MODULE_ROOTS));
        roots.addAll(List.of(FAMILY_TEXT_ROOTS));
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
        assertThat(packagePathMismatches(TIANSHU_SOURCES))
                .as("REQ-027 相邻适配器包声明必须与目录一致")
                .isEmpty();
        for (String packageName : List.of(ENUM_PACKAGE, EXCEPTION_PACKAGE)) {
            Path packageInfo = MAIN_SOURCES.resolve(packageName.replace('.', '/') + "/package-info.java");
            assertThat(packageInfo).as("REQ-027 新公共包必须记录职责").exists();
            assertThat(read(packageInfo)).contains("package " + packageName + ";");
        }

        assertThat(read(POM))
                .as("MC-DEP-001 rpc-starter 直接使用合同必须显式声明")
                .contains("egon-cola-component-common-core")
                .contains("spring-boot-starter-validation");
        assertThat(read(TIANSHU_POM))
                .as("MC-DEP-001 相邻适配器直接使用合同必须显式声明")
                .contains("egon-cola-component-common-core")
                .contains("spring-boot-starter-validation");

        String readme = read(README);
        assertThat(readme).as("README 只同步本 Step 落地的结构")
                .contains(EXCEPTION_PACKAGE)
                .contains(ENUM_PACKAGE)
                .contains(CANONICAL_FACADE_BEAN)
                .doesNotContain(LEGACY_EXCEPTION_PACKAGE + ".EgonRpc");
    }

    private static Map<String, List<String>> enumCodes() {
        Map<String, List<String>> codes = new LinkedHashMap<>();
        codes.put(RPC_PACKAGE + ".annotation.FailStrategy",
                List.of("INHERIT=0", "FAIL_OPEN=1", "FAIL_CLOSED=2", "LOCAL_FALLBACK=3"));
        codes.put(RPC_PACKAGE + ".annotation.LoadBalance",
                List.of("INHERIT=0", "ROUND_ROBIN=1", "SMOOTH_WEIGHTED_ROUND_ROBIN=2", "RANDOM=3",
                        "WEIGHTED_RANDOM=4", "CONSISTENT_HASH=5", "LEAST_IN_FLIGHT=6"));
        codes.put(ERROR_CODE_TYPE,
                List.of("RPC_INVALID_CONTRACT=0", "RPC_PROVIDER_START_FAILED=1", "RPC_REGISTRATION_FAILED=2",
                        "RPC_YUHENG_UNAVAILABLE=3", "RPC_YUHENG_AMBIGUOUS=4", "RPC_SERVICE_NOT_FOUND=5",
                        "RPC_METHOD_NOT_FOUND=6", "RPC_DEADLINE_EXCEEDED=7", "RPC_CANCELLED=8",
                        "RPC_PROVIDER_UNAVAILABLE=9", "RPC_RATE_LIMITED=10", "RPC_INVALID_REQUEST=11",
                        "RPC_PROVIDER_REJECTED=12", "RPC_INTERNAL=13"));
        codes.put(RPC_PACKAGE + ".consumer.gateway.RpcGatewayState",
                List.of("STARTING=0", "READY=1", "UNAVAILABLE=2", "AMBIGUOUS=3", "STOPPED=4"));
        codes.put(RPC_PACKAGE + ".consumer.invocation.RpcInvocationMode",
                List.of("BLOCKING=0", "ASYNC=1"));
        codes.put(RPC_PACKAGE + ".consumer.lifecycle.RpcConsumerRuntimeState",
                List.of("NEW=0", "STARTING=1", "READY=2", "DEGRADED=3", "DRAINING=4", "FAILED=5", "STOPPED=6"));
        codes.put(RPC_PACKAGE + ".consumer.reference.RpcReferenceMode", List.of("GATEWAY=0", "DIRECT=1"));
        codes.put(RPC_PACKAGE + ".context.invocation.RpcFailureStage",
                List.of("GATEWAY=0", "PROVIDER=1"));
        codes.put(RPC_PACKAGE + ".contract.descriptor.RpcType", List.of("UNARY=0"));
        codes.put(RPC_PACKAGE + ".provider.lifecycle.RpcProviderRuntimeState",
                List.of("NEW=0", "STARTING=1", "READY=2", "DEGRADED=3", "DRAINING=4", "FAILED=5", "STOPPED=6"));
        codes.put(RPC_PACKAGE + ".provider.registration.RpcLeaseOperationResult$Status",
                List.of("RENEWED=0", "DELETED=1", "NOT_FOUND=2", "LEASE_MISMATCH=3"));
        codes.put(RPC_PACKAGE + ".provider.registration.RpcProviderRegistrationMode",
                List.of("REQUIRED=0", "DISABLED=1"));
        codes.put(TIANSHU_PACKAGE + ".security.DdcRpcOperation",
                List.of("SDK_REGISTER=0", "SDK_HEARTBEAT=1", "SDK_OFFLINE=2", "CONFIG_PULL=3", "PUBLISH_ACK=4",
                        "REGISTRY_REGISTER=5", "REGISTRY_HEARTBEAT=6", "REGISTRY_DEREGISTER=7", "REGISTRY_READ=8",
                        "MANAGEMENT_CONFIG_READ=9", "MANAGEMENT_CONFIG_WRITE=10", "MANAGEMENT_PUBLISH=11",
                        "MANAGEMENT_TASK_READ=12", "MANAGEMENT_TASK_RETRY=13", "MANAGEMENT_INSTANCE_READ=14",
                        "MANAGEMENT_ADMISSION_REVOKE=15", "MANAGEMENT_SCOPE_READ=16", "MANAGEMENT_REGISTRY_READ=17",
                        "MANAGEMENT_CATALOG_READ=18"));
        return Map.copyOf(codes);
    }

    private static Path repo(String relative) {
        return REPO_ROOT.resolve(relative);
    }

    private static String simpleName(String type) {
        int lastDot = type.lastIndexOf('.');
        int lastPackage = type.lastIndexOf('.', lastDot - 1);
        return lastPackage < 0 ? type : type.substring(lastPackage + 1).replace('$', '.');
    }

    /** 源码里的 enum 声明只写自己的名字，嵌套 enum 也不带外层类型前缀。 */
    private static String enumSimpleName(String type) {
        String qualified = simpleName(type);
        int nested = qualified.lastIndexOf('.');
        return nested < 0 ? qualified : qualified.substring(nested + 1);
    }

    private static Class<?> componentException(String simpleName) {
        String type = EXCEPTION_PACKAGE + "." + simpleName;
        try {
            return Class.forName(type);
        } catch (ClassNotFoundException exception) {
            return fail("REQ-002 " + simpleName + " 必须位于 " + EXCEPTION_PACKAGE);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Enum<?> constant(Class<?> enumType, String name) {
        return Enum.valueOf((Class<? extends Enum>) enumType, name);
    }

    /** 固定编码与 message 都必须显式声明：message 取常量名，code 取文档化的字面量。 */
    @SuppressWarnings("unchecked")
    private static List<String> codeAssignments(Class<?> enumType) throws Exception {
        Method getCode = enumType.getMethod("getCode");
        Method getMessage = enumType.getMethod("getMessage");
        Enum<?>[] constants = (Enum<?>[]) enumType.getEnumConstants();
        List<String> rendered = new ArrayList<>();
        for (Enum<?> constant : constants) {
            assertThat(getMessage.invoke(constant))
                    .as("REQ-004 %s.%s 的 message 必须保持常量名", enumType.getSimpleName(), constant.name())
                    .isEqualTo(constant.name());
            rendered.add(constant.name() + "=" + getCode.invoke(constant));
        }
        return rendered;
    }

    /** 跨模块 enum 不在本模块类路径上，按源码字面量固定同一份编码合同。 */
    private static List<String> sourceCodeAssignments(Path root, String enumName) throws IOException {
        Pattern constant = Pattern.compile(
                "([A-Z][A-Z0-9_]*)\\s*\\(\\s*(\\d+)\\s*,\\s*\"([^\"]*)\"\\s*\\)");
        List<String> rendered = new ArrayList<>();
        for (Path file : sources(root)) {
            String body = Files.readString(file);
            int declaration = body.indexOf("enum " + enumName);
            if (declaration < 0) {
                continue;
            }
            int constants = body.indexOf('{', declaration);
            int end = body.indexOf(';', constants);
            assertThat(end).as("%s 的常量列表必须以分号结束", enumName).isPositive();
            Matcher matcher = constant.matcher(body.substring(constants, end));
            while (matcher.find()) {
                assertThat(matcher.group(3))
                        .as("REQ-004 %s.%s 的 message 必须保持常量名", enumName, matcher.group(1))
                        .isEqualTo(matcher.group(1));
                rendered.add(matcher.group(1) + "=" + matcher.group(2));
            }
            return rendered;
        }
        return fail("REQ-004 未找到 " + enumName + " 的源码声明");
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

    interface UnannotatedContract {
        String get(String request);
    }

    private static String programmaticDefaults() throws IOException {
        StringBuilder builder = new StringBuilder();
        for (String type : List.of(
                RPC_PACKAGE + ".consumer.direct.RpcDirectClientFactory",
                RPC_PACKAGE + ".consumer.proxy.EgonRpcReferenceBeanPostProcessor")) {
            builder.append(read(MAIN_SOURCES.resolve(type.replace('.', '/') + ".java"))).append('\n');
        }
        return builder.toString();
    }

    private static List<String> directoryNames(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.list(directory)) {
            return paths.map(path -> path.getFileName().toString()).sorted().toList();
        }
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
                    String line = lines.get(cursor).trim();
                    if (line.isEmpty() || line.startsWith("//") || line.startsWith("/*")
                            || line.startsWith("*")) {
                        continue;
                    }
                    Matcher constant = ENUM_CONSTANT.matcher(line);
                    if (!constant.find()) {
                        break;
                    }
                    if (!EXPLICIT_CODE.matcher(line).find()) {
                        violations.add(enumName + "." + constant.group(1) + " -> " + line);
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
                    .filter(path -> path.toString().replace('\\', '/').indexOf("/target/") < 0)
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
