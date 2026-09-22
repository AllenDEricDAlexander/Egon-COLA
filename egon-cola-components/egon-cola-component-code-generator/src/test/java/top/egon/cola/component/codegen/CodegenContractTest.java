package top.egon.cola.component.codegen;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import top.egon.cola.component.codegen.model.CodegenConfigBO;
import top.egon.cola.component.codegen.model.CodegenPlanBO;
import top.egon.cola.component.codegen.model.CodegenProfileEnum;
import top.egon.cola.component.codegen.validation.CodegenConfigValidator;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodegenContractTest {

    private final CodegenConfigValidator validator = new CodegenConfigValidator(
            new ValidationUtils(Validation.buildDefaultValidatorFactory().getValidator()));

    @Test
    void missingOutputRootIsRejectedWithoutCreatingIt() {
        Path outputRoot = Path.of("target", "codegen-contract-missing-root");
        CodegenConfigBO config = config("web", "repo");
        config.setOutputRoot(null);
        assertInvalid(config, CodegenConfigValidator.CONFIG_REQUIRED);
        assertFalse(Files.exists(outputRoot));
    }

    @Test
    void serviceControllerIsUnsupported() {
        assertInvalid(config("service", "controller"), CodegenConfigValidator.UNSUPPORTED_ARTIFACT);
    }

    @Test
    void webRepositoryWithExistingTypesIsValidAndWritesNothing() {
        Path outputRoot = Path.of("target", "codegen-contract-valid-root");
        CodegenConfigBO config = config("web", "repo");
        config.setOutputRoot(outputRoot.toString());
        config.setExistingTypes(new LinkedHashMap<>(Map.of(
                "po", "com.example.order.infrastructure.order.po.OrderPO",
                "dao", "com.example.order.infrastructure.order.dao.OrderDAO")));
        assertValid(config);
        assertNoTargetWrites(outputRoot);
    }

    @Test
    void duplicateArtifactsAreRejected() {
        CodegenConfigBO config = config("web", "repo");
        config.setArtifacts(new ArrayList<>(List.of("repo", "repo")));
        config.setExistingTypes(new LinkedHashMap<>(Map.of(
                "po", "com.example.order.infrastructure.order.po.OrderPO",
                "dao", "com.example.order.infrastructure.order.dao.OrderDAO")));
        assertInvalid(config, CodegenConfigValidator.DUPLICATE_PATH);
    }

    @Test
    void unknownArtifactAndAgentProfileAreRejected() {
        assertInvalid(config("light", "graph"), CodegenConfigValidator.UNSUPPORTED_ARTIFACT);
        assertThrows(JsonMappingException.class,
                () -> mapper().readValue("{\"projectType\":\"agent\"}", CodegenConfigBO.class));
        assertThrows(IllegalArgumentException.class, () -> CodegenProfileEnum.parse("WEB"));
        assertThrows(IllegalArgumentException.class, () -> CodegenProfileEnum.parse("traditional"));
    }

    @Test
    void unknownJsonPropertyFailsAndPlanRoundTripIsStable() throws Exception {
        assertThrows(JsonMappingException.class,
                () -> mapper().readValue("{\"configVersion\":1,\"unexpected\":true}", CodegenConfigBO.class));

        CodegenPlanBO plan = CodegenPlanBO.builder()
                .formatVersion(CodegenPlanBO.FORMAT_VERSION)
                .planId("abc")
                .profile(CodegenProfileEnum.WEB)
                .outputRootBinding("order-service")
                .inputFingerprint("in")
                .configFingerprint("cfg")
                .templateSetVersion("1")
                .componentFingerprint("cmp")
                .files(List.of(CodegenPlanBO.FileChangeBO.builder()
                        .actionId("a1")
                        .path("src/OrderPO.java")
                        .artifact("po")
                        .table("orders")
                        .operation("ADD")
                        .expectedDiskHashPresent(false)
                        .previousGeneratedHashPresent(false)
                        .candidateHash("hash")
                        .candidateHashPresent(true)
                        .candidatePath("candidates/OrderPO.java")
                        .build()))
                .schemaChanges(List.of())
                .pendingImpacts(List.of())
                .diagnostics(List.of())
                .artifactStates(List.of())
                .latestObservedSchema("schema")
                .build();
        ObjectMapper mapper = mapper();
        String first = mapper.writeValueAsString(plan);
        CodegenPlanBO restored = mapper.readValue(first, CodegenPlanBO.class);
        String second = mapper.writeValueAsString(restored);
        assertEquals(first, second);
        assertEquals(plan, restored);
        assertTrue(restored.supportedFormat());
        restored.setFormatVersion(2);
        assertFalse(restored.supportedFormat());
    }

    @Test
    void approvedExternalCoordinateIsOnlyFreeMarker() throws Exception {
        Path modulePom = Path.of("pom.xml").toAbsolutePath().normalize();
        Document document = parse(modulePom);
        Set<String> external = new TreeSet<>();
        NodeList dependencies = document.getElementsByTagName("dependency");
        for (int index = 0; index < dependencies.getLength(); index++) {
            Element dependency = (Element) dependencies.item(index);
            String scope = text(dependency, "scope");
            if ("test".equals(scope) || "provided".equals(scope)) {
                continue;
            }
            String groupId = text(dependency, "groupId");
            String artifactId = text(dependency, "artifactId");
            if (!"top.egon".equals(groupId)) {
                external.add(groupId + ":" + artifactId);
            }
        }
        assertEquals(Set.of("org.freemarker:freemarker"), external);

        Document parent = parse(modulePom.getParent().resolve("../pom.xml").normalize());
        assertEquals("2.3.35", property(parent, "freemarker.version"));
        assertTrue(managed(parent, "org.freemarker", "freemarker"));
    }

    private static void assertInvalid(CodegenConfigBO config, String code) {
        List<CodegenPlanBO.DiagnosticBO> diagnostics = new CodegenContractTest().validator.validate(config);
        assertTrue(diagnostics.stream().anyMatch(diagnostic -> code.equals(diagnostic.getCode())),
                () -> "expected " + code + " but was " + diagnostics);
    }

    private void assertValid(CodegenConfigBO config) {
        List<CodegenPlanBO.DiagnosticBO> diagnostics = validator.validate(config);
        assertTrue(diagnostics.isEmpty(), () -> "expected no diagnostics but was " + diagnostics);
    }

    private static void assertNoTargetWrites(Path outputRoot) {
        assertFalse(Files.exists(outputRoot));
    }

    private static CodegenConfigBO config(String profile, String artifact) {
        return CodegenConfigBO.builder()
                .configVersion(1)
                .profile(CodegenProfileEnum.parse(profile))
                .outputRoot("target/codegen-contract-output")
                .basePackage("com.example.order")
                .domain("order")
                .input(CodegenConfigBO.InputBO.builder()
                        .mode("schema")
                        .schemaFiles(new ArrayList<>(List.of("ddl/schema.sql")))
                        .build())
                .roots(new LinkedHashMap<>(Map.of("infrastructure", "order-service-infrastructure")))
                .tables(new ArrayList<>(List.of("orders")))
                .artifacts(new ArrayList<>(List.of(artifact)))
                .build();
    }

    private static ObjectMapper mapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        mapper.findAndRegisterModules();
        return mapper;
    }

    private static Document parse(Path path) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setNamespaceAware(false);
        return factory.newDocumentBuilder().parse(path.toFile());
    }

    private static String text(Element element, String tag) {
        NodeList nodes = element.getElementsByTagName(tag);
        if (nodes.getLength() == 0) {
            return "";
        }
        return nodes.item(0).getTextContent().trim();
    }

    private static String property(Document document, String name) {
        NodeList properties = document.getElementsByTagName("properties");
        Element propertiesElement = (Element) properties.item(0);
        return text(propertiesElement, name);
    }

    private static boolean managed(Document document, String groupId, String artifactId) {
        NodeList dependencies = document.getElementsByTagName("dependency");
        for (int index = 0; index < dependencies.getLength(); index++) {
            Node node = dependencies.item(index);
            if (!(node instanceof Element dependency)) {
                continue;
            }
            if (groupId.equals(text(dependency, "groupId")) && artifactId.equals(text(dependency, "artifactId"))) {
                return true;
            }
        }
        return false;
    }
}
