package top.egon.cola.component.rag.contract;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.component.common.core.enums.EgonEnum;
import top.egon.cola.component.common.core.enums.ErrorStatus;
import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.rag.autoconfigure.RagEmbeddingModelProperties;
import top.egon.cola.component.rag.autoconfigure.RagProperties;
import top.egon.cola.component.rag.autoconfigure.RagRetrievalProperties;
import top.egon.cola.component.rag.chunk.RagChunkingStrategyEnum;
import top.egon.cola.component.rag.converter.RagChunkConverter;
import top.egon.cola.component.rag.model.RagRetrievedChunkBO;
import top.egon.cola.component.rag.storage.RagDocumentStorageTypeEnum;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
import static org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Step 9 合同：RAG 的自定义异常、稳定字符串 code、手写 enum、校验交接与转换必须落在公共合同上
 * （REQ-002、REQ-003、REQ-004、REQ-005、REQ-006、REQ-007、REQ-027）。
 *
 * <p>本文件只固定本 Step 列出的六条算法差异；向量库探测、切块与抽取行为由原回归证明保持。</p>
 */
class ComponentContractGovernanceTest {

    private static final Path MAIN_SOURCES = Path.of("src/main/java");

    private static final Path POM = Path.of("pom.xml");

    private static final String RAG_PACKAGE = "top.egon.cola.component.rag";

    private static final String EXCEPTION_PACKAGE = RAG_PACKAGE + ".common.exception";

    private static final String LEGACY_EXCEPTION_PACKAGE = RAG_PACKAGE + ".exception";

    private static final List<String> COMPONENT_EXCEPTIONS = List.of(
            "RagException", "RagChunkingException", "RagConfigurationException", "RagExtractionException",
            "RagExtractorConflictException", "RagExtractorMissingException", "RagModelNotRegisteredException",
            "RagStorageException", "RagValidationException", "RagVectorStoreException");

    /** 归一化前后都必须保持的稳定机器可读 code；旧 code() 只换成公共 getStatus()。 */
    private static final Map<String, String> STABLE_STATUSES = stableStatuses();

    /** 只有这五个异常历史上提供 (message, cause) 构造器，其余只接收已拼好的安全消息。 */
    private static final List<String> CAUSE_CAPABLE_EXCEPTIONS = List.of(
            "RagChunkingException", "RagConfigurationException", "RagExtractionException",
            "RagStorageException", "RagVectorStoreException");

    private static final List<String> CONVERTER_TYPES = List.of(RAG_PACKAGE + ".converter.RagChunkConverter");

    /** 算法第 6 行：旧 FQCN 在模块与家庭消费者中必须零残留。 */
    private static final List<String> RELOCATED_TYPES = COMPONENT_EXCEPTIONS.stream()
            .map(name -> LEGACY_EXCEPTION_PACKAGE + "." + name)
            .toList();

    private static final Path[] GOVERNED_TEXT_ROOTS = {
            MAIN_SOURCES,
            Path.of("src/test/java"),
            POM,
            Path.of("README.md"),
            Path.of("README.zh-CN.md")};

    /** Step 9 声明并额外修正的家庭消费者引用闭包（REQ-027）。 */
    private static final Path[] FAMILY_TEXT_ROOTS = {
            Path.of("../../egon-cola-archetypes/source-projects/egon-cola-source-agent"
                    + "/egon-cola-source-agent-application/src/main/java"),
            Path.of("../../egon-cola-archetypes/source-projects/egon-cola-source-agent"
                    + "/egon-cola-source-agent-application/src/test/java"),
            Path.of("../../egon-cola-archetypes/source-projects/egon-cola-source-agent"
                    + "/egon-cola-source-agent-infrastructure/src/main/java"),
            Path.of("../../egon-cola-archetypes/source-projects/egon-cola-source-agent"
                    + "/egon-cola-source-agent-infrastructure/src/test/java")};

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
    private static final Pattern CONVERTER_DECLARATION =
            Pattern.compile("\\binterface\\s+(\\w*Converter)\\b");

    private static Map<String, String> stableStatuses() {
        Map<String, String> statuses = new LinkedHashMap<>();
        statuses.put("RagChunkingException", "RAG_CHUNKING");
        statuses.put("RagConfigurationException", "RAG_CONFIGURATION");
        statuses.put("RagExtractionException", "RAG_EXTRACTION");
        statuses.put("RagExtractorConflictException", "RAG_EXTRACTOR_CONFLICT");
        statuses.put("RagExtractorMissingException", "RAG_EXTRACTOR_MISSING");
        statuses.put("RagModelNotRegisteredException", "RAG_MODEL_NOT_REGISTERED");
        statuses.put("RagStorageException", "RAG_STORAGE");
        statuses.put("RagValidationException", "RAG_VALIDATION");
        statuses.put("RagVectorStoreException", "RAG_VECTOR_STORE");
        return Map.copyOf(statuses);
    }

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

        Class<?> root = commonException("RagException");
        assertThat(root.getSuperclass())
                .as("REQ-002 技术异常根必须继承 CommonException")
                .isEqualTo(CommonException.class);
        List<String> wrongParent = new ArrayList<>();
        declared.forEach((name, packageName) -> {
            if (name.equals("RagException")) {
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
        assertThat(MAIN_SOURCES.resolve(LEGACY_EXCEPTION_PACKAGE.replace('.', '/')))
                .as("REQ-002 旧 exception 包必须整体消失")
                .doesNotExist();
    }

    // 算法第 2 行：旧 String code 进入公共 getStatus()，原 cause/安全消息/构造形态保持。
    @Test
    void stableStringCodesBecomeTheCommonStatusAndKeepCausesAndSafeMessages() throws Exception {
        Class<?> root = commonException("RagException");
        CommonException plain = (CommonException) root
                .getConstructor(String.class, String.class).newInstance("RAG_CUSTOM", "a stable safe message");
        assertThat(plain.getCode())
                .as("REQ-003 归一化后 code 仍是公共 int 合同")
                .isEqualTo(ResultCode.SYSTEM_ERROR.getCode());
        assertThat(plain.getStatus())
                .as("REQ-003 旧 String code 必须由公共 status 承载")
                .isEqualTo("RAG_CUSTOM");
        assertThat(plain.getMessage()).isEqualTo("a stable safe message");
        assertThat(plain.isRetryable()).isFalse();

        RuntimeException origin = new RuntimeException("origin");
        CommonException rooted = (CommonException) root
                .getConstructor(String.class, String.class, Throwable.class)
                .newInstance("RAG_CUSTOM", "a stable safe message", origin);
        assertThat(rooted.getCause()).as("REQ-003 原 cause 必须保持同一实例").isSameAs(origin);

        for (Map.Entry<String, String> entry : STABLE_STATUSES.entrySet()) {
            Class<?> type = commonException(entry.getKey());
            assertThat(type.getSuperclass()).isEqualTo(root);
            CommonException failure = (CommonException) type
                    .getConstructor(String.class).newInstance("a stable safe message");
            assertThat(failure.getStatus())
                    .as("REQ-003 %s 的 code 字面量不得改变", entry.getKey())
                    .isEqualTo(entry.getValue());
            assertThat(failure.getCode()).isEqualTo(ResultCode.SYSTEM_ERROR.getCode());
            assertThat(failure.getMessage()).isEqualTo("a stable safe message");
            assertThat(failure.isRetryable()).isFalse();
            assertThat(failure.getCause()).isNull();
            assertThat(safeMessage(failure))
                    .as("REQ-003 safeMessage() 调用合同保持")
                    .isEqualTo("a stable safe message");
        }

        for (Class<?> type : List.of(commonException("RagChunkingException"),
                commonException("RagConfigurationException"), commonException("RagExtractionException"),
                commonException("RagStorageException"), commonException("RagVectorStoreException"))) {
            assertThat(type.getConstructors())
                    .as("%s 保留 message 与 message+cause 两种构造形态", type.getSimpleName())
                    .hasSize(2);
            RuntimeException cause = new RuntimeException("cause");
            CommonException failure = (CommonException) type
                    .getConstructor(String.class, Throwable.class).newInstance("failed", cause);
            assertThat(failure.getCause()).isSameAs(cause);
            assertThat(failure.getMessage()).isEqualTo("failed");
        }
        for (String name : List.of("RagExtractorConflictException", "RagExtractorMissingException",
                "RagModelNotRegisteredException", "RagValidationException")) {
            assertThat(commonException(name).getConstructors())
                    .as("%s 只有单消息构造器", name)
                    .hasSize(1);
        }
        assertThat(CAUSE_CAPABLE_EXCEPTIONS).allMatch(COMPONENT_EXCEPTIONS::contains);

        List<String> shadowed = new ArrayList<>();
        for (String name : COMPONENT_EXCEPTIONS) {
            Class<?> type = commonException(name);
            for (Method method : type.getDeclaredMethods()) {
                if (Set.of("getCode", "getStatus", "isRetryable", "code").contains(method.getName())) {
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
                .as("REQ-003 不得遮蔽公共 int code/status/retryable，也不得保留旧 String code()")
                .isEmpty();
    }

    // 算法第 3 行：手写 enum 实现 EgonEnum，常量为显式字面量，既有名称与持久化表示不改。
    @Test
    void handwrittenEnumsImplementTheCommonEnumContract() throws Exception {
        List<String> headers = enumHeaders(MAIN_SOURCES);

        assertThat(headers).as("本模块手写 enum 清单").containsExactly(
                "RagChunkingStrategyEnum implements EgonEnum",
                "RagDocumentStorageTypeEnum implements EgonEnum");
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

        assertThat(RagChunkingStrategyEnum.values()).extracting(Enum::name)
                .as("REQ-004 常量集合与顺序保持（append-only）")
                .containsExactly("TOKEN", "MARKDOWN_HEADING", "RECURSIVE");
        assertThat(codes(RagChunkingStrategyEnum.class)).containsExactly(0, 1, 2);
        assertThat(messages(RagChunkingStrategyEnum.values()))
                .containsExactly("TOKEN", "MARKDOWN_HEADING", "RECURSIVE");
        assertThat(RagDocumentStorageTypeEnum.values()).extracting(Enum::name).containsExactly("LOCAL");
        assertThat(codes(RagDocumentStorageTypeEnum.class)).containsExactly(0);
        assertThat(messages(RagDocumentStorageTypeEnum.values())).containsExactly("LOCAL");

        // 原 wire/name 语义不变：配置仍按常量名解析，工厂仍按枚举身份分派。
        assertThat(RagChunkingStrategyEnum.valueOf("TOKEN")).isSameAs(RagChunkingStrategyEnum.TOKEN);
        assertThat(RagDocumentStorageTypeEnum.valueOf("LOCAL"))
                .isSameAs(RagDocumentStorageTypeEnum.LOCAL);
        String factory = read(MAIN_SOURCES.resolve(
                "top/egon/cola/component/rag/chunk/RagChunkingStrategyFactory.java"));
        assertThat(factory).contains("Map<RagChunkingStrategyEnum, RagChunkingStrategy> registry");
    }

    // 算法第 4 行：本模块没有自有 validator，层间交接继续走原生 Jakarta 约束且零业务副作用。
    @Test
    void layerHandoffsStayOnNativeConstraintsWithoutRenamedValidators() throws Exception {
        assertThat(declaredTypes(VALIDATOR_DECLARATION))
                .as("REQ-007 本模块不得存在仅改名绕过公共合同的 validator")
                .isEmpty();

        String autoConfiguration = read(MAIN_SOURCES.resolve(
                "top/egon/cola/component/rag/autoconfigure/RagAutoConfiguration.java"));
        assertThat(autoConfiguration)
                .as("REQ-007 绑定后的配置必须按原生约束整体校验后再装配")
                .contains("validator.validate(properties)")
                .contains("throw new RagConfigurationException(\"invalid rag configuration: \"");
        assertThat(countOccurrences(autoConfiguration, "validator.validate("))
                .as("配置校验只有一个入口").isEqualTo(1);

        String sources = text(MAIN_SOURCES);
        assertThat(sources)
                .as("REQ-007 无自有 validator 时不得私造校验设施")
                .doesNotContain("buildDefaultValidatorFactory")
                .doesNotContain("new ValidationUtils(");
        assertThat(sources)
                .as("输入失败不得触发 DAO/缓存/MQ 副作用")
                .doesNotContain("JdbcTemplate")
                .doesNotContain("RedisTemplate")
                .doesNotContain("RabbitTemplate")
                .doesNotContain("KafkaTemplate");

        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        assertThat(validator.validate(validProperties()))
                .as("规范化后的默认载体满足声明式原生约束")
                .isEmpty();

        ThrowingCallable oversized = () -> new RagRetrievalProperties(0, 300);
        Throwable topKFailure = catchThrowable(oversized);
        assertThat(topKFailure).as("范围规则仍由载体自身拒绝").isInstanceOf(CommonException.class);
        assertThat(((CommonException) topKFailure).getStatus())
                .as("REQ-003 拒绝原因通过公共 status 暴露")
                .isEqualTo("RAG_CONFIGURATION");
        assertThatThrownBy(() -> new RagRetrievalProperties(9, 4))
                .isInstanceOf(CommonException.class)
                .hasMessage("default-top-k must not exceed max-top-k");
    }

    // 算法第 5 行：转换只服务真实 S/T 语义对，继续由 MapStruct 生成并保持双向合同。
    @Test
    void conversionsStayOnTheCommonConverterContract() throws Exception {
        assertThat(declaredTypes(CONVERTER_DECLARATION))
                .as("本模块 converter 清单")
                .containsExactlyElementsOf(CONVERTER_TYPES);
        assertThat(BaseConverter.class.isAssignableFrom(RagChunkConverter.class))
                .as("REQ-005 真实 S/T 对必须继承公共 BaseConverter").isTrue();
        assertThat(List.of(RagChunkConverter.class.getMethods()))
                .as("REQ-005 双向合同保持")
                .extracting(Method::getName)
                .contains("toTarget", "toSource");
        assertThat(Class.forName(RAG_PACKAGE + ".converter.RagChunkConverterImpl"))
                .as("REQ-005 映射由 MapStruct 生成而非手写复制")
                .isAssignableTo(RagChunkConverter.class);
        assertThat(text(MAIN_SOURCES)).as("Rule 5 禁止 BeanUtils 复制").doesNotContain("BeanUtils");

        RagRetrievedChunkBO chunk = new RagRetrievedChunkBO("doc-1:7", "kb-1", "doc-1", 7, "chunk body", 0.83,
                "openai-small", Map.of("source", "manual"));
        Document document = RagChunkConverter.INSTANCE.toTarget(chunk);
        assertThat(document.getId()).isEqualTo("doc-1:7");
        assertThat(document.getText()).isEqualTo("chunk body");
        RagRetrievedChunkBO restored = RagChunkConverter.INSTANCE.toSource(document);
        assertThat(restored)
                .extracting(RagRetrievedChunkBO::chunkId, RagRetrievedChunkBO::content,
                        RagRetrievedChunkBO::collectionId, RagRetrievedChunkBO::documentId,
                        RagRetrievedChunkBO::chunkIndex, RagRetrievedChunkBO::logicalModelName,
                        RagRetrievedChunkBO::attributes)
                .containsExactly("doc-1:7", "chunk body", "kb-1", "doc-1", 7, "openai-small",
                        Map.of("source", "manual"));
    }

    // 算法第 6 行：模块与家庭消费者、反射名称与 README 同一步更新，旧路径零残留。
    @Test
    void consumersReflectionNamesAndDocsDropTheLegacyLocations() throws Exception {
        List<Path> roots = new ArrayList<>(List.of(GOVERNED_TEXT_ROOTS));
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
                assertThat(content)
                        .as("%s 不应引用旧异常包 %s", file, LEGACY_EXCEPTION_PACKAGE)
                        .doesNotContain(LEGACY_EXCEPTION_PACKAGE + ";")
                        .doesNotContain(LEGACY_EXCEPTION_PACKAGE + ".");
            }
        }

        assertThat(packagePathMismatches(MAIN_SOURCES))
                .as("REQ-027 包声明必须与目录一致，且不重新引入旧 exception 包")
                .isEmpty();
        Path packageInfo = MAIN_SOURCES.resolve(
                "top/egon/cola/component/rag/common/exception/package-info.java");
        assertThat(packageInfo).as("REQ-027 所属包必须记录职责").exists();
        assertThat(read(packageInfo)).contains("package " + EXCEPTION_PACKAGE + ";");
        assertThat(MAIN_SOURCES.resolve("top/egon/cola/component/rag/exception/package-info.java"))
                .as("File 30 旧包职责文件必须删除")
                .doesNotExist();

        String pom = read(POM);
        assertThat(pom).as("MC-DEP-001 直接使用合同必须显式声明")
                .contains("egon-cola-component-common-core")
                .contains("spring-boot-starter-validation");
        String readme = read(Path.of("README.md"));
        assertThat(readme).as("File 67 只同步本 Step 落地的结构")
                .contains(EXCEPTION_PACKAGE)
                .contains("getStatus()");
    }

    private static RagProperties validProperties() {
        return new RagProperties(true, 1536, "hostVectorStore",
                Map.of("openai-small", new RagEmbeddingModelProperties("ragEmbeddingModel")),
                null, null, null, null);
    }

    private static Throwable catchThrowable(ThrowingCallable callable) {
        try {
            callable.call();
        } catch (Throwable failure) {
            return failure;
        }
        return fail("载体必须拒绝越界配置");
    }

    private static Class<?> commonException(String simpleName) {
        String type = EXCEPTION_PACKAGE + "." + simpleName;
        try {
            return Class.forName(type);
        } catch (ClassNotFoundException exception) {
            return fail("REQ-002 " + simpleName + " 必须位于 " + EXCEPTION_PACKAGE);
        }
    }

    private static int[] codes(Class<? extends Enum<?>> enumType) {
        Enum<?>[] constants = enumType.getEnumConstants();
        int[] values = new int[constants.length];
        for (int index = 0; index < constants.length; index++) {
            values[index] = ((EgonEnum) constants[index]).getCode();
        }
        return values;
    }

    /** 旧 String code 消费者的替换证明：安全消息继续可用，且不新增诊断字段。 */
    private static String safeMessage(CommonException failure) {
        try {
            Method getter = failure.getClass().getMethod("safeMessage");
            return String.valueOf(getter.invoke(failure));
        } catch (ReflectiveOperationException missing) {
            return fail("REQ-003 " + failure.getClass().getName() + " 必须保留 safeMessage()");
        }
    }

    /** 常量类是 final 的，因此 getter 只能按公共合同反射读取，保证本文件在 RED 阶段仍可编译。 */
    private static List<String> messages(Enum<?>[] constants) {
        List<String> messages = new ArrayList<>();
        for (Enum<?> constant : constants) {
            messages.add(String.valueOf(invoke(constant, "getMessage")));
        }
        return messages;
    }

    private static Object invoke(Enum<?> target, String getter) {
        try {
            Method method = target.getClass().getMethod(getter);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (ReflectiveOperationException missing) {
            return fail("REQ-004 " + target.getClass().getName() + " 必须提供 " + getter + "()");
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

    private static List<String> declaredTypes(Pattern declaration) throws IOException {
        List<String> declared = new ArrayList<>();
        for (Path file : sources(MAIN_SOURCES)) {
            String packageName = "";
            for (String line : Files.readAllLines(file)) {
                Matcher packageMatcher = PACKAGE.matcher(line);
                if (packageMatcher.find()) {
                    packageName = packageMatcher.group(1);
                    continue;
                }
                Matcher matcher = declaration.matcher(line);
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
                    if (line.isBlank() || line.trim().startsWith("*") || line.trim().startsWith("/*")) {
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
