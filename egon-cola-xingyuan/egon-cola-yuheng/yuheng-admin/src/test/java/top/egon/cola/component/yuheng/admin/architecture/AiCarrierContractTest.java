package top.egon.cola.component.yuheng.admin.architecture;

import com.fasterxml.jackson.databind.DeserializationFeature;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import top.egon.cola.component.common.core.enums.EgonEnum;
import top.egon.cola.component.yuheng.admin.config.GatewayAiJsonConfiguration;
import top.egon.cola.component.yuheng.admin.knowledge.domain.dto.KnowledgeBaseCommandDTO;
import top.egon.cola.component.yuheng.admin.llm.domain.dto.LlmModelCommandDTO;
import top.egon.cola.component.yuheng.admin.llm.domain.enums.LlmModelKindEnum;
import top.egon.cola.component.yuheng.admin.llm.domain.enums.LlmProtocolEnum;
import top.egon.cola.component.yuheng.admin.shared.domain.validation.ExecuteGroup;
import top.egon.cola.component.yuheng.admin.wiki.domain.dto.WikiTransitionCommandDTO;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fixes the Step 2 carrier contract: ordinary POJOs stay classes, every AI
 * enum keeps a stable wire snapshot on a tens {@code code} interval, and the
 * group plus custom constraints reject inconsistent requests.
 */
class AiCarrierContractTest {

    private static final Pattern ENUM_CONSTANT = Pattern.compile(
            "^ {4}([A-Z][A-Z0-9_]*)\\(([^)]*)\\)\\s*[;,]", Pattern.MULTILINE);

    private static final Pattern ENUM_VALUE_FIELD = Pattern.compile(
            "@EnumValue\\s+private final String (\\w+);");

    /**
     * Prior public contract of the eight legacy admin enums plus the six
     * cross-process AI enums, recorded as {@code CONSTANT/code/wireValue}.
     */
    private static final Map<String, List<String>> FROZEN_WIRE = frozenWire();

    /**
     * The admin copy and the executable copy declare the same constants, codes
     * and wire values independently, because the two processes share only the
     * persisted column and the HTTP/MCP payload.
     */
    private static final Set<String> SHARED_CROSS_PROCESS = Set.of(
            "LlmProtocolEnum", "LlmCapabilityEnum", "LlmModelKindEnum",
            "LlmDeploymentEnum", "McpPersistentTaskStateEnum",
            "McpPersistentApprovalStatusEnum");

    private static ValidatorFactory validatorFactory;

    private static Validator validator;

    @BeforeAll
    static void openValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        if (validatorFactory != null) {
            validatorFactory.close();
        }
    }

    @Test
    void aiCarriersAreOrdinaryClassesAndEveryEnumDeclaresOneWireField()
            throws IOException {
        List<String> records = new ArrayList<>();
        List<String> enumGaps = new ArrayList<>();
        for (Path source : aiCarrierSources()) {
            String text = stripComments(Files.readString(source));
            String name = sourceNameWithoutExtension(source);
            if (name.endsWith("Enum.java") || name.endsWith("Status.java")) {
                if (!ENUM_VALUE_FIELD.matcher(text).find()) {
                    enumGaps.add(name + " has no @EnumValue String field");
                }
                if (!text.contains("@JsonValue")) {
                    enumGaps.add(name + " has no @JsonValue contract");
                }
                if (!text.contains("@JsonCreator")) {
                    enumGaps.add(name + " has no @JsonCreator rejection");
                }
                continue;
            }
            if (text.matches("(?s).*\\b(public|static)\\s+record\\s+\\w+.*")) {
                records.add(source.toString());
            }
            if (name.endsWith("DTO.java") || name.endsWith("VO.java")) {
                for (String annotation : List.of("@Data", "@NoArgsConstructor",
                        "@AllArgsConstructor", "@Accessors(chain = true)")) {
                    if (!text.contains(annotation)) {
                        enumGaps.add(name + " misses " + annotation);
                    }
                }
            }
        }
        assertTrue(records.isEmpty(),
                "Ordinary business carriers must be classes, not records: "
                        + records);
        assertTrue(enumGaps.isEmpty(), "Carrier baseline gaps: " + enumGaps);
    }

    @Test
    void enumCodesAndWireValuesMatchTheFrozenSnapshot() throws Exception {
        assertEquals(14, FROZEN_WIRE.size(), "frozen enum set is complete");
        for (Map.Entry<String, List<String>> entry : FROZEN_WIRE.entrySet()) {
            Class<?> enumType = Class.forName(entry.getKey());
            assertTrue(EgonEnum.class.isAssignableFrom(enumType),
                    enumType + " must implement EgonEnum");
            Object[] constants = enumType.getEnumConstants();
            List<String> actual = new ArrayList<>();
            for (Object constant : constants) {
                Enum<?> value = (Enum<?>) constant;
                actual.add(value.name() + "/" + ((EgonEnum) value).getCode()
                        + "/" + wireValueOf(enumType, value));
            }
            assertEquals(entry.getValue(), actual,
                    "Wire snapshot drifted for " + enumType);
        }
    }

    @Test
    void ordinalIsNeverTheBusinessCodeAndUnknownWireValuesAreRejected()
            throws Exception {
        for (String enumName : FROZEN_WIRE.keySet()) {
            Class<?> enumType = Class.forName(enumName);
            for (Object constant : enumType.getEnumConstants()) {
                Enum<?> value = (Enum<?>) constant;
                assertNotEquals(value.ordinal(),
                        ((EgonEnum) value).getCode(),
                        value + " must not expose its ordinal as code");
            }
            Method creator = Arrays.stream(enumType.getMethods())
                    .filter(method -> method.isAnnotationPresent(
                            com.fasterxml.jackson.annotation.JsonCreator.class))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            enumName + " has no @JsonCreator"));
            Throwable rejected = assertThrows(
                    java.lang.reflect.InvocationTargetException.class,
                    () -> creator.invoke(null, "__unsupported__"),
                    enumName + " must reject an unknown wire value").getCause();
            assertEquals(IllegalArgumentException.class, rejected.getClass(),
                    enumName + " must fail closed with IllegalArgumentException");
            assertThrows(java.lang.reflect.InvocationTargetException.class,
                    () -> creator.invoke(null, (Object) null),
                    enumName + " must reject a null wire value");
        }
    }

    @Test
    void crossProcessEnumDuplicatesDeclareIdenticalWireContracts()
            throws IOException {
        Map<String, List<String>> admin = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : FROZEN_WIRE.entrySet()) {
            if (SHARED_CROSS_PROCESS.contains(simpleName(entry.getKey()))) {
                admin.put(simpleName(entry.getKey()), entry.getValue());
            }
        }
        assertEquals(SHARED_CROSS_PROCESS.size(), admin.size(),
                "every shared enum has an admin snapshot entry");
        List<String> drift = new ArrayList<>();
        for (Path gatewayDir : List.of(
                reactorSibling("yuheng-llm-gateway",
                        "top/egon/cola/component/yuheng/llm/proxy/domain/enums"),
                reactorSibling("yuheng-mcp-gateway",
                        "top/egon/cola/component/yuheng/mcp/engine/mcp/domain/enums"))) {
            List<Path> sources;
            try (Stream<Path> paths = Files.list(gatewayDir)) {
                sources = paths.filter(path -> path.toString()
                        .endsWith(".java")).sorted().toList();
            }
            for (Path source : sources) {
                String name = sourceNameWithoutExtension(source);
                List<String> declared = parseWireSnapshot(source);
                if (declared.isEmpty()) {
                    drift.add(name + " declares no wire contract");
                } else if (!admin.containsKey(name)) {
                    drift.add(name + " has no admin counterpart declared");
                } else if (!admin.get(name).equals(declared)) {
                    drift.add(name + " gateway=" + declared + " admin="
                            + admin.get(name));
                }
            }
        }
        assertTrue(drift.isEmpty(),
                "Cross-process wire drift: " + String.join(" | ", drift));
    }

    @Test
    void llmModelValidatorRejectsInconsistentEmbeddingCarriers() {
        Set<String> dimensionsPaths = violatedPaths(
                modelCommand(LlmModelKindEnum.CHAT, 1536, null));
        assertTrue(dimensionsPaths.contains("dimensions"),
                "CHAT must reject a carried embedding dimension: "
                        + dimensionsPaths);

        Set<String> spacePaths = violatedPaths(
                modelCommand(LlmModelKindEnum.EMBEDDING, 1536, null));
        assertTrue(spacePaths.contains("embeddingSpaceId"),
                "EMBEDDING must require an immutable space identity: "
                        + spacePaths);

        Set<String> strayPaths = violatedPaths(
                modelCommand(LlmModelKindEnum.EMBEDDING, null, "space-1"));
        assertTrue(strayPaths.contains("dimensions"),
                "EMBEDDING must reject a missing dimension: " + strayPaths);

        LlmModelCommandDTO consistent = modelCommand(
                LlmModelKindEnum.CHAT, null, null);
        Set<String> consistentPaths = violatedPaths(consistent);
        assertFalse(consistentPaths.contains("dimensions"),
                "Consistent CHAT carrier must not be rejected: "
                        + consistentPaths);
        assertFalse(consistentPaths.contains("embeddingSpaceId"),
                "Consistent CHAT carrier must not be rejected: "
                        + consistentPaths);
        assertEquals("chat-primary", consistent.getKey(),
                "Validation must not mutate the rejected or accepted request");
    }

    @Test
    void wikiTransitionConstraintsFireOnlyWithinExecuteGroup()
            throws Exception {
        Set<String> defaultPaths = violatedPaths(
                new WikiTransitionCommandDTO());
        assertTrue(defaultPaths.isEmpty(),
                "The reused transition carrier must stay silent outside its"
                        + " execution group: " + defaultPaths);

        Set<String> executePaths = violatedPaths(
                new WikiTransitionCommandDTO(), ExecuteGroup.class);
        assertTrue(executePaths.contains("event"),
                "ExecuteGroup must reject a transition without an event: "
                        + executePaths);
        assertTrue(executePaths.contains("expectedPageRevision"),
                "ExecuteGroup must reject a missing page CAS revision: "
                        + executePaths);

        assertGroupsDeclaredOnTransitionCarrier();
    }

    @Test
    void aiCarrierJsonBindingRejectsUnknownPropertiesWithoutTouchingGlobalMapper()
            throws Exception {
        MappingJackson2HttpMessageConverter converter =
                new MappingJackson2HttpMessageConverter();
        List<HttpMessageConverter<?>> converters = new ArrayList<>();
        converters.add(converter);
        new GatewayAiJsonConfiguration().extendMessageConverters(converters);

        assertFalse(converter.getObjectMapper()
                        .isEnabled(DeserializationFeature
                                .FAIL_ON_UNKNOWN_PROPERTIES),
                "The shared Spring Boot ObjectMapper must stay untouched");

        String smuggled = "{\"key\":\"chat-primary\",\"actorType\":\"USER\","
                + "\"reviewStatus\":\"APPROVED\"}";
        assertThrows(org.springframework.http.converter
                        .HttpMessageNotReadableException.class,
                () -> converter.read(KnowledgeBaseCommandDTO.class,
                        json(smuggled)),
                "A server-derived field must not bind into an AI carrier");

        LlmModelCommandDTO bound = (LlmModelCommandDTO) converter.read(
                LlmModelCommandDTO.class,
                json("{\"key\":\"chat-primary\",\"kind\":\""
                        + LlmModelKindEnum.CHAT.wireValue()
                        + "\",\"protocols\":[\""
                        + LlmProtocolEnum.OPENAI_CHAT.wireValue() + "\"]}"));
        assertEquals(LlmModelKindEnum.CHAT, bound.getKind(),
                "The persisted wire string is the JSON contract");
        assertThrows(org.springframework.http.converter
                        .HttpMessageNotReadableException.class,
                () -> converter.read(LlmModelCommandDTO.class,
                        json("{\"key\":\"x\",\"kind\":\"CHAT\","
                                + "\"protocols\":[\"OPENAI_CHAT\"],"
                                + "\"expectedRevision\":\"not-a-number\"}")));
    }

    private static MockHttpInputMessage json(String body) {
        MockHttpInputMessage message = new MockHttpInputMessage(
                body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        message.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return message;
    }

    private static void assertGroupsDeclaredOnTransitionCarrier()
            throws Exception {
        assertTrue(Arrays.stream(WikiTransitionCommandDTO.class
                        .getAnnotations())
                .anyMatch(annotation -> annotation.annotationType()
                        .isAnnotationPresent(Constraint.class)
                        && groupsOf(annotation).contains(ExecuteGroup.class)),
                "The transition carrier class constraint must belong to"
                        + " ExecuteGroup");
        Field eventField = WikiTransitionCommandDTO.class
                .getDeclaredField("event");
        assertTrue(Arrays.stream(eventField.getAnnotations())
                        .anyMatch(annotation -> annotation.annotationType()
                                .isAnnotationPresent(Constraint.class)
                                && groupsOf(annotation).contains(ExecuteGroup.class)),
                "The transition event field must be validated within"
                        + " ExecuteGroup");
    }

    private static List<Class<?>> groupsOf(
            java.lang.annotation.Annotation annotation) {
        try {
            return Arrays.asList((Class<?>[]) annotation.annotationType()
                    .getMethod("groups").invoke(annotation));
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Cannot read groups of "
                    + annotation, failure);
        }
    }

    private static Set<String> violatedPaths(Object carrier,
            Class<?>... groups) {
        Set<String> paths = new LinkedHashSet<>();
        var violations = groups.length == 0
                ? validator.validate(carrier)
                : validator.validate(carrier, groups);
        for (ConstraintViolation<?> violation : violations) {
            paths.add(violation.getPropertyPath().toString());
        }
        return paths;
    }

    private static LlmModelCommandDTO modelCommand(
            LlmModelKindEnum kind, Integer dimensions, String spaceId) {
        LlmModelCommandDTO command = new LlmModelCommandDTO();
        command.setKey("chat-primary");
        command.setName("Chat primary");
        command.setKind(kind);
        command.setProtocols(List.of(LlmProtocolEnum.OPENAI_CHAT));
        command.setEnabled(Boolean.TRUE);
        command.setDimensions(dimensions);
        command.setEmbeddingSpaceId(spaceId);
        command.setAllowedSubjects(List.of("service://caller"));
        command.setExpectedRevision(0L);
        return command;
    }

    private static String wireValueOf(Class<?> enumType, Enum<?> value)
            throws Exception {
        Method jsonValueMethod = Arrays.stream(enumType.getMethods())
                .filter(method -> method.isAnnotationPresent(
                        com.fasterxml.jackson.annotation.JsonValue.class)
                        && method.getParameterCount() == 0)
                .findFirst()
                .orElse(null);
        if (jsonValueMethod != null) {
            return String.valueOf(jsonValueMethod.invoke(value));
        }
        for (Field field : enumType.getDeclaredFields()) {
            if (field.isAnnotationPresent(
                    com.fasterxml.jackson.annotation.JsonValue.class)) {
                field.setAccessible(true);
                return String.valueOf(field.get(value));
            }
        }
        throw new AssertionError(enumType + " has no @JsonValue contract");
    }

    private static List<Path> aiCarrierSources() throws IOException {
        List<Path> sources = new ArrayList<>();
        for (String fragment : List.of("llm", "knowledge", "wiki")) {
            collect(sources, adminPackage(fragment).resolve("domain"));
        }
        collect(sources, adminPackage("mcp").resolve("domain/enums"));
        collect(sources, adminPackage("catalog").resolve("domain/enums"));
        collect(sources, adminPackage("openapi").resolve("domain/enums"));
        collect(sources, adminPackage("release").resolve("domain/enums"));
        collect(sources, adminPackage("observability").resolve("domain/enums"));
        collect(sources, adminPackage("shared").resolve("domain/validation"));
        collect(sources, Path.of("src/main/java/top/egon/cola/component"
                + "/yuheng/admin/config"));
        collect(sources, reactorSibling("yuheng-llm-gateway",
                "top/egon/cola/component/yuheng/llm/proxy/domain/enums"));
        collect(sources, reactorSibling("yuheng-mcp-gateway",
                "top/egon/cola/component/yuheng/mcp/engine/mcp/domain/enums"));
        assertFalse(sources.isEmpty(), "Step 2 carriers must be discoverable");
        return sources;
    }

    private static void collect(List<Path> target, Path directory)
            throws IOException {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .forEach(target::add);
        }
    }

    private static List<String> parseWireSnapshot(Path source)
            throws IOException {
        String text = stripComments(Files.readString(source));
        Matcher wireField = ENUM_VALUE_FIELD.matcher(text);
        if (!wireField.find()) {
            return List.of();
        }
        String wireName = wireField.group(1);
        Pattern constructor = Pattern.compile(
                "\\b" + Pattern.quote(fileName(source).replace(".java", ""))
                        + "\\(([^)]*)\\)\\s*\\{");
        Matcher matcher = constructor.matcher(text);
        if (!matcher.find()) {
            return List.of();
        }
        List<String> parameters = new ArrayList<>();
        for (String parameter : matcher.group(1).split(",")) {
            String[] tokens = parameter.trim().split("\\s+");
            parameters.add(tokens[tokens.length - 1]);
        }
        String body = text.substring(matcher.start());
        Map<String, String> assignments = new LinkedHashMap<>();
        Matcher assignment = Pattern
                .compile("this\\.(\\w+) = (\\w+);").matcher(body);
        while (assignment.find()) {
            assignments.put(assignment.group(1), assignment.group(2));
        }
        List<String> snapshot = new ArrayList<>();
        Matcher constant = ENUM_CONSTANT.matcher(text);
        while (constant.find()) {
            List<String> arguments = splitArguments(constant.group(2));
            Map<String, String> bound = new LinkedHashMap<>();
            for (int index = 0; index < parameters.size()
                    && index < arguments.size(); index++) {
                bound.put(assignments.getOrDefault(parameters.get(index),
                        parameters.get(index)), arguments.get(index));
            }
            snapshot.add(constant.group(1) + "/"
                    + Integer.parseInt(bound.get("code")) + "/"
                    + bound.get(wireName).replace("\"", ""));
        }
        return snapshot;
    }

    private static List<String> splitArguments(String raw) {
        List<String> arguments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (char character : raw.toCharArray()) {
            if (character == '"') {
                quoted = !quoted;
                current.append(character);
            } else if (character == ',' && !quoted) {
                arguments.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        if (!current.toString().isBlank()) {
            arguments.add(current.toString().trim());
        }
        return arguments;
    }

    private static String stripComments(String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", "")
                .replaceAll("//[^\n]*", "");
    }

    private static String fileName(Path path) {
        return path.getFileName().toString();
    }

    private static String sourceNameWithoutExtension(Path path) {
        return fileName(path).replace(".java", "");
    }

    private static String simpleName(String fqcn) {
        return fqcn.substring(fqcn.lastIndexOf('.') + 1);
    }

    private static Path adminPackage(String fragment) {
        return Path.of("src/main/java/top/egon/cola/component/yuheng/admin")
                .resolve(fragment);
    }

    private static Path reactorSibling(String module, String packagedir) {
        return Path.of("..").resolve(module).resolve("src/main/java")
                .resolve(packagedir).normalize();
    }

    private static Map<String, List<String>> frozenWire() {
        Map<String, List<String>> frozen = new LinkedHashMap<>();
        frozen.put("top.egon.cola.component.yuheng.admin.catalog.domain"
                + ".enums.GatewayCatalogProtocolEnum", List.of(
                "HTTP/10/HTTP", "RPC/20/RPC"));
        frozen.put("top.egon.cola.component.yuheng.admin.mcp.domain.enums"
                + ".McpCapabilityKindEnum", List.of(
                "RESOURCE/10/RESOURCE", "RESOURCE_TEMPLATE/20/RESOURCE_TEMPLATE",
                "PROMPT/30/PROMPT", "TASK_POLICY/40/TASK_POLICY",
                "APP_BINDING/50/APP_BINDING"));
        frozen.put("top.egon.cola.component.yuheng.admin.observability.domain"
                + ".enums.GatewayCallEventConsumeResultEnum", List.of(
                "PROJECTED/10/PROJECTED", "DUPLICATE/20/DUPLICATE",
                "POISON_RECORDED/30/POISON_RECORDED"));
        frozen.put("top.egon.cola.component.yuheng.admin.openapi.domain.enums"
                + ".GatewayOpenApiSyncStateEnum", List.of(
                "DISCOVERED/10/DISCOVERED", "FETCHING/20/FETCHING",
                "VALIDATING/30/VALIDATING", "INVALID/40/INVALID",
                "INCONSISTENT_BUILD/50/INCONSISTENT_BUILD",
                "INGESTING/60/INGESTING", "VALID/70/VALID",
                "INGEST_FAILED/80/INGEST_FAILED",
                "FETCH_FAILED/90/FETCH_FAILED", "STALE/100/STALE"));
        frozen.put("top.egon.cola.component.yuheng.admin.release.domain.enums"
                + ".GatewayPublicationPhaseEnum", List.of(
                "CHUNK/10/CHUNK", "ACTIVATION/20/ACTIVATION"));
        frozen.put("top.egon.cola.component.yuheng.admin.release.domain.enums"
                + ".GatewayPublicationStatusEnum", List.of(
                "PLANNED/10/PLANNED", "RESOLVED/20/RESOLVED",
                "SUBMITTED/30/SUBMITTED", "SUCCESS/40/SUCCESS",
                "FAILED/50/FAILED", "PARTIAL_SUCCESS/60/PARTIAL_SUCCESS",
                "TIMEOUT/70/TIMEOUT", "UNKNOWN/80/UNKNOWN"));
        frozen.put("top.egon.cola.component.yuheng.admin.release.domain.enums"
                + ".GatewayReleaseStatus", List.of(
                "CREATED/10/CREATED", "VALIDATING/20/VALIDATING",
                "READY/30/READY", "PUBLISHING/40/PUBLISHING",
                "SUCCESS/50/SUCCESS", "FAILED/60/FAILED",
                "TIMEOUT/70/TIMEOUT", "UNKNOWN/80/UNKNOWN",
                "SUPERSEDED/90/SUPERSEDED"));
        frozen.put("top.egon.cola.component.yuheng.admin.shared.domain.enums"
                + ".AdminActorTypeEnum", List.of(
                "USER/10/USER", "SERVICE/20/SERVICE"));
        frozen.put("top.egon.cola.component.yuheng.admin.llm.domain.enums"
                + ".LlmProtocolEnum", List.of(
                "OPENAI_CHAT/10/OPENAI_CHAT",
                "OPENAI_EMBEDDING/20/OPENAI_EMBEDDING",
                "OPENAI_RESPONSES/30/OPENAI_RESPONSES",
                "ANTHROPIC_MESSAGES/40/ANTHROPIC_MESSAGES"));
        frozen.put("top.egon.cola.component.yuheng.admin.llm.domain.enums"
                + ".LlmCapabilityEnum", List.of(
                "TEXT/10/TEXT", "FUNCTION_TOOLS/20/FUNCTION_TOOLS",
                "STRUCTURED_OUTPUT/30/STRUCTURED_OUTPUT", "VISION/40/VISION",
                "REASONING/50/REASONING"));
        frozen.put("top.egon.cola.component.yuheng.admin.llm.domain.enums"
                + ".LlmModelKindEnum", List.of(
                "CHAT/10/CHAT", "EMBEDDING/20/EMBEDDING"));
        frozen.put("top.egon.cola.component.yuheng.admin.llm.domain.enums"
                + ".LlmDeploymentEnum", List.of(
                "LOCAL/10/LOCAL", "CLOUD/20/CLOUD"));
        frozen.put("top.egon.cola.component.yuheng.admin.mcp.domain.enums"
                + ".McpPersistentTaskStateEnum", List.of(
                "WORKING/10/WORKING", "INPUT_REQUIRED/20/INPUT_REQUIRED",
                "COMPLETED/30/COMPLETED", "FAILED/40/FAILED",
                "CANCELLED/50/CANCELLED"));
        frozen.put("top.egon.cola.component.yuheng.admin.mcp.domain.enums"
                + ".McpPersistentApprovalStatusEnum", List.of(
                "PENDING/10/PENDING", "CONSUMED/20/CONSUMED",
                "EXPIRED/30/EXPIRED", "REVOKED/40/REVOKED"));
        return Collections.unmodifiableMap(frozen);
    }
}
