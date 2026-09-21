package top.egon.cola.archetype.source.agent.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Generated-project contract for the Agent archetype layering and common component rules.
 *
 * <p>Every assertion is a source-shape check, so the contract is observable before the Spring context
 * exists; the runtime behaviour behind each rule is covered by the family fixtures. Nothing here
 * imports a moved type: the assertions read source text, which keeps a failure a statement about the
 * tree rather than a compilation error.</p>
 */
class ArchetypeContractConvergenceTest {

    private static final String ROOT = "top.egon.cola.archetype.source.agent";

    private static final List<String> MODULES = List.of(
            "common", "domain", "application", "infrastructure", "adapter", "starter");

    private static final Path MAIN = Path.of("src/main/java");

    private static final Path TEST = Path.of("src/test/java");

    /**
     * The retired roles of this family. {@code Gateway} is the shape the domain used to call an
     * external capability; {@code repo.dao}/{@code repo.po} nested the persistence roles under the
     * Repository instead of making them siblings.
     */
    private static final List<String> RETIRED = List.of(
            "Gateway", "CachePort", "EventPublisher", "Assembler", "IdempotencyAdapter", "repo.dao",
            "repo.po", "repo.converter", ROOT + ".domain.knowledge.gateway", ROOT + ".domain.research.gateway",
            ROOT + ".application.knowledge.command", ROOT + ".application.research.command",
            ROOT + ".application.knowledge.exception", ROOT + ".application.research.exception",
            ROOT + ".adapter.knowledge.dto", ROOT + ".adapter.knowledge.vo", ROOT + ".adapter.knowledge.converter",
            ROOT + ".adapter.research.dto", ROOT + ".adapter.research.vo", ROOT + ".adapter.research.converter");

    @Test
    void domainOwnsEveryCapabilityAndStaysFrameworkFree() throws IOException {
        String all = joinMain();
        for (String forbidden : RETIRED) {
            assertFalse(all.contains(forbidden), "retired shape must be gone: " + forbidden);
        }

        for (String obsolete : List.of("knowledge/gateway", "research/gateway")) {
            assertFalse(Files.exists(sources("domain").resolve(obsolete)),
                    "obsolete domain package survived: " + obsolete);
        }

        // A capability the domain needs is a domain Service, whatever sits behind it.
        assertExists("domain", "knowledge/service/KnowledgeAnswerService.java");
        assertExists("domain", "knowledge/service/KnowledgeVectorService.java");
        assertExists("domain", "research/service/DeepResearchAgentService.java");
        assertExists("domain", "knowledge/service/KnowledgeAnswerRunService.java");
        assertExists("domain", "knowledge/service/KnowledgeQaEventObserverService.java");
        assertExists("domain", "research/service/DeepResearchRunService.java");
        assertExists("domain", "research/service/DeepResearchEventObserverService.java");
        assertExists("domain", "knowledge/repository/KnowledgeBaseRepository.java");
        assertExists("domain", "knowledge/repository/KnowledgeDocumentRepository.java");

        for (Path path : sourcesOf("domain")) {
            String text = read(path);
            assertFalse(text.contains(ROOT + ".infrastructure."),
                    "domain must not depend on infrastructure: " + path);
            assertFalse(text.contains(ROOT + ".application."),
                    "domain must not depend on application: " + path);
            assertFalse(text.contains(ROOT + ".adapter."), "domain must not depend on adapter: " + path);
            assertFalse(text.contains("import org.springframework."), "domain must stay framework-free: " + path);
        }
    }

    @Test
    void everyDomainServiceIsImplementedByOneNamedInfrastructureBean() throws IOException {
        for (String path : List.of("knowledge/service/impl/KnowledgeAnswerChatModelServiceImpl.java",
                "knowledge/service/impl/RagKnowledgeVectorServiceImpl.java",
                "research/service/impl/AgentFlowDeepResearchAgentServiceImpl.java")) {
            assertExists("infrastructure", path);
        }
        for (String obsolete : List.of("knowledge/gateway", "research/gateway")) {
            assertFalse(Files.exists(sources("infrastructure").resolve(obsolete)),
                    "obsolete infrastructure package survived: " + obsolete);
        }

        String answer = read("infrastructure", "knowledge/service/impl/KnowledgeAnswerChatModelServiceImpl.java");
        String vector = read("infrastructure", "knowledge/service/impl/RagKnowledgeVectorServiceImpl.java");
        String agent = read("infrastructure", "research/service/impl/AgentFlowDeepResearchAgentServiceImpl.java");

        assertTrue(answer.contains("implements KnowledgeAnswerService"));
        assertTrue(vector.contains("implements KnowledgeVectorService"));
        assertTrue(agent.contains("implements DeepResearchAgentService"));
        assertTrue(answer.contains("@Service(\"knowledgeAnswerService\")"));
        assertTrue(vector.contains("@Service(\"knowledgeVectorService\")"));
        assertTrue(agent.contains("@Service(\"deepResearchAgentService\")"));
        for (String text : List.of(answer, vector, agent)) {
            assertTrue(text.contains("@RequiredArgsConstructor"));
            assertTrue(text.contains("@Slf4j"), "a Spring-managed business class logs through @Slf4j");
            assertTrue(text.contains("@Qualifier("), "every injected collaborator is named");
        }
        // The vendor boundary stays where it was: the model stream and the vector store are only reachable here.
        assertTrue(answer.contains("ChatModel"));
        assertTrue(vector.contains("VectorStore"));
        assertTrue(agent.contains("AgentFlowService"));

        // The persistence roles stay beside the Repository, which keeps its own package.
        for (String path : List.of("knowledge/service/KnowledgeBaseRepositoryImpl.java",
                "knowledge/service/KnowledgeDocumentRepositoryImpl.java")) {
            String text = read("infrastructure", path);
            assertTrue(matches(text, "@Repository\\(\"\\w+Repository\"\\)"),
                    "a Spring-managed Repository needs an explicit bean name: " + path);
            assertTrue(text.contains("@Qualifier(\""), "the DAO and converter are named, not typed: " + path);
        }
    }

    @Test
    void boundaryCarriersLiveInThePojoDirectories() throws IOException {
        for (String path : List.of(
                "adapter:knowledge/pojo/dto/AskKnowledgeBaseRequest.java",
                "adapter:knowledge/pojo/dto/CreateKnowledgeBaseRequest.java",
                "adapter:knowledge/pojo/dto/RetrieveKnowledgeRequest.java",
                "adapter:knowledge/pojo/dto/UpdateKnowledgeBaseRequest.java",
                "adapter:knowledge/pojo/vo/KnowledgeBaseVO.java",
                "adapter:knowledge/pojo/vo/KnowledgeDocumentVO.java",
                "adapter:knowledge/pojo/vo/KnowledgeQaEventVO.java",
                "adapter:knowledge/pojo/vo/KnowledgeQaReferenceVO.java",
                "adapter:knowledge/pojo/vo/KnowledgeRetrievalVO.java",
                "adapter:knowledge/pojo/vo/KnowledgeRetrievedChunkVO.java",
                "adapter:research/pojo/dto/StartDeepResearchRequest.java",
                "adapter:research/pojo/vo/DeepResearchEventVO.java",
                "application:knowledge/pojo/command/AskKnowledgeBaseCommand.java",
                "application:knowledge/pojo/command/CreateKnowledgeBaseCommand.java",
                "application:knowledge/pojo/command/RetrieveKnowledgeCommand.java",
                "application:knowledge/pojo/command/UpdateKnowledgeBaseCommand.java",
                "application:knowledge/pojo/command/UploadKnowledgeDocumentCommand.java",
                "application:research/pojo/command/StartDeepResearchCommand.java")) {
            int separator = path.indexOf(':');
            assertExists(path.substring(0, separator), path.substring(separator + 1));
        }

        for (String obsolete : List.of(
                "adapter:knowledge/dto", "adapter:knowledge/vo", "adapter:knowledge/converter",
                "adapter:research/dto", "adapter:research/vo", "adapter:research/converter",
                "application:knowledge/command", "application:knowledge/exception",
                "application:research/command", "application:research/exception")) {
            int separator = obsolete.indexOf(':');
            assertFalse(Files.exists(sources(obsolete.substring(0, separator))
                    .resolve(obsolete.substring(separator + 1))), "obsolete carrier package: " + obsolete);
        }

        List<Path> carriers = mainSources().stream()
                .filter(path -> path.toString().contains("/pojo/"))
                .filter(path -> !path.getFileName().toString().endsWith("Converter.java"))
                .filter(path -> !path.getFileName().toString().endsWith("Convertor.java"))
                .filter(path -> !path.getFileName().toString().equals("package-info.java"))
                .toList();
        assertEquals(18, carriers.size(), "every reviewed inbound or outbound carrier");
        for (Path path : carriers) {
            assertTrue(read(path).contains("implements BasePojo"),
                    "hand-written carrier must implement the common carrier contract: " + path);
        }
    }

    @Test
    void persistenceRolesStaySiblingsOfTheRepository() throws IOException {
        for (String path : List.of("knowledge/dao/KnowledgeBaseDAO.java", "knowledge/dao/KnowledgeDocumentDAO.java",
                "knowledge/po/KnowledgeBasePO.java", "knowledge/po/KnowledgeDocumentPO.java",
                "knowledge/po/JsonbStringTypeHandler.java", "knowledge/converter/KnowledgeBasePOConverter.java",
                "knowledge/converter/KnowledgeDocumentPOConverter.java")) {
            assertExists("infrastructure", path);
        }
        for (String obsolete : List.of("knowledge/repo/dao", "knowledge/repo/po", "knowledge/repo/converter")) {
            assertFalse(Files.exists(sources("infrastructure").resolve(obsolete)),
                    "obsolete persistence package survived: " + obsolete);
        }
        for (Path path : sources(sources("infrastructure").resolve("knowledge/repo"))) {
            String name = path.getFileName().toString();
            assertTrue(name.endsWith("Repository.java") || name.equals("package-info.java"),
                    "repo holds only the technical Repository: " + path);
        }

        for (String xml : List.of("mybatis/mapper/knowledge/KnowledgeBaseDAO.xml",
                "mybatis/mapper/knowledge/KnowledgeDocumentDAO.xml")) {
            String text = resource("infrastructure", xml);
            assertTrue(text.contains(ROOT + ".infrastructure.knowledge.dao."), xml);
            assertTrue(text.contains(ROOT + ".infrastructure.knowledge.po."), xml);
            assertFalse(text.contains("repo.dao"), xml);
            assertFalse(text.contains("repo.po"), xml);
            // Only the packages moved; the tenant, soft-delete and optimistic-lock semantics did not.
            assertTrue(text.contains("deleted_at IS NULL"), xml);
            assertTrue(text.contains("MP_OPTLOCK_VERSION_ORIGINAL"), xml);
            assertTrue(text.contains("tenant_id"), xml);
            assertTrue(text.contains("selectActiveById"), xml);
            assertTrue(text.contains("deleteVersionedById"), xml);
            assertFalse(text.contains("${"), "no string interpolation may enter the mapper SQL: " + xml);
        }

        String application = read("starter", "DeepResearchApplication.java");
        assertTrue(application.contains(ROOT + ".infrastructure.knowledge.dao"));
        assertFalse(application.contains("repo.dao"));

        // The write-guard keys are per statement id, so they follow the DAO move across every profile.
        String guarded = ROOT + ".infrastructure.knowledge.dao.KnowledgeDocumentDAO.softDeleteByKnowledgeBaseId";
        for (String yml : List.of("application.yml", "application-dev.yml", "application-prod.yml",
                "application-test.yml")) {
            String text = resource("starter", yml);
            assertTrue(text.contains(guarded), "the bulk-delete guard is missing from " + yml);
            assertFalse(text.contains(ROOT + ".infrastructure.knowledge.repo.dao"),
                    "the guard still names the moved package in " + yml);
        }
    }

    @Test
    void customExceptionsLiveInCommonOnTheCommonHierarchy() throws IOException {
        assertExists("common", "exception/KnowledgeApplicationException.java");
        assertExists("common", "exception/DeepResearchApplicationException.java");

        String knowledge = read("common", "exception/KnowledgeApplicationException.java");
        String research = read("common", "exception/DeepResearchApplicationException.java");
        for (String text : List.of(knowledge, research)) {
            assertTrue(text.contains("extends BusinessException"),
                    "an expected rejection is a business failure on the common hierarchy");
            assertFalse(text.contains("extends RuntimeException"),
                    "the family must not keep a private exception root: " + text);
            assertTrue(text.contains("getStatus()"),
                    "the stable String code stays published through the common status getter");
            assertTrue(text.contains("serialVersionUID"));
        }
        assertTrue(knowledge.contains("KnowledgeErrorCodeEnum code()"),
                "the typed accessor keeps the original vocabulary instead of a String");
        assertTrue(research.contains("ResearchErrorCodeEnum code()"));
        assertTrue(knowledge.contains("onField("), "the field-named rejection stays available");
        assertTrue(knowledge.contains("traceId()") && research.contains("traceId()"));
        assertTrue(knowledge.contains("fieldErrors()") && research.contains("fieldErrors()"));
    }

    @Test
    void enumsCarryExplicitCodesAndKeepTheirWireNames() throws IOException {
        for (String path : List.of("knowledge/model/ChunkingStrategyEnum.java",
                "knowledge/model/DocumentIngestStatusEnum.java", "knowledge/model/KnowledgeBaseStatusEnum.java",
                "knowledge/model/KnowledgeQaEventTypeEnum.java", "research/model/ReportLanguageEnum.java",
                "research/model/ResearchEventTypeEnum.java", "research/model/ResearchStageEnum.java")) {
            assertTrue(read("domain", path).contains("implements EgonEnum"),
                    "a domain enum exposes the common code/message contract: " + path);
        }
        for (String path : List.of("error/KnowledgeErrorCodeEnum.java", "error/ResearchErrorCodeEnum.java")) {
            String text = read("common", path);
            assertTrue(text.contains("implements ErrorStatus"),
                    "an error vocabulary exposes the common status contract: " + path);
            assertTrue(text.contains("public String getStatus()"),
                    "the String wire code is published through the common getter: " + path);
            assertTrue(text.contains("return name();"),
                    "the String wire code stays the constant name: " + path);
        }

        assertCodes("domain", "knowledge/model/ChunkingStrategyEnum.java", "TOKEN", "MARKDOWN_HEADING", "RECURSIVE");
        assertCodes("domain", "knowledge/model/DocumentIngestStatusEnum.java",
                "PENDING", "PROCESSING", "SUCCEEDED", "FAILED", "DEAD");
        assertCodes("domain", "knowledge/model/KnowledgeBaseStatusEnum.java", "ACTIVE", "DELETED");
        assertCodes("domain", "knowledge/model/KnowledgeQaEventTypeEnum.java",
                "STARTED", "PROGRESS", "COMPLETED", "FAILED");
        assertCodes("domain", "research/model/ReportLanguageEnum.java", "ZH_CN", "EN_US");
        assertCodes("domain", "research/model/ResearchEventTypeEnum.java", "STARTED", "PROGRESS", "COMPLETED", "FAILED");
        assertCodes("domain", "research/model/ResearchStageEnum.java", "PLANNING", "EVIDENCE_RESEARCH",
                "COUNTERPOINT_RESEARCH", "FRESHNESS_RESEARCH", "SYNTHESIS", "COMPLETED", "FAILED");
        assertCodes("common", "error/KnowledgeErrorCodeEnum.java", "KNOWLEDGE_VALIDATION_ERROR",
                "KNOWLEDGE_NOT_ACCEPTABLE", "KNOWLEDGE_BASE_NOT_FOUND", "KNOWLEDGE_BASE_CODE_CONFLICT",
                "KNOWLEDGE_BASE_BUSY", "KNOWLEDGE_DOCUMENT_NOT_FOUND", "KNOWLEDGE_DOCUMENT_BUSY",
                "KNOWLEDGE_CONTENT_MISSING", "KNOWLEDGE_IMMUTABLE_FIELD", "KNOWLEDGE_MODEL_NOT_REGISTERED",
                "KNOWLEDGE_EXTRACTOR_MISSING", "KNOWLEDGE_FILE_TOO_LARGE", "KNOWLEDGE_EMBEDDING_FAILED",
                "KNOWLEDGE_CAPACITY_EXHAUSTED", "KNOWLEDGE_DEPENDENCY_UNAVAILABLE", "KNOWLEDGE_INTERNAL_ERROR");
        assertCodes("common", "error/ResearchErrorCodeEnum.java", "RESEARCH_VALIDATION_ERROR", "RESEARCH_UNAUTHORIZED",
                "RESEARCH_NOT_ACCEPTABLE", "RESEARCH_UNSUPPORTED_MEDIA_TYPE", "RESEARCH_CAPACITY_EXHAUSTED",
                "RESEARCH_DEPENDENCY_UNAVAILABLE", "RESEARCH_INTERNAL_ERROR", "RESEARCH_TIMEOUT");

        String ingest = read("domain", "knowledge/model/DocumentIngestStatusEnum.java");
        assertTrue(ingest.contains("isTerminal()") && ingest.contains("canTransitionTo("),
                "the ingest state machine keeps its own transitions");
        assertTrue(read("domain", "research/model/ReportLanguageEnum.java").contains("wireValue()"),
                "the report language keeps the String value the API accepts");
        String all = joinMain();
        assertFalse(all.contains(".ordinal()"), "an enum code is declared, never derived from the ordinal");
    }

    @Test
    void convertersStayOnTheCommonMappingContract() throws IOException {
        List<Path> converters = mainSources().stream()
                .filter(path -> path.getFileName().toString().endsWith("Converter.java"))
                .toList();
        assertEquals(12, converters.size(), "the reviewed converter surface");
        for (Path path : converters) {
            String text = read(path);
            assertTrue(text.contains("@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)"),
                    "an unmapped property must fail the build: " + path);
            assertTrue(text.contains("BaseConverter<") || text.contains("BaseForwardConverter<"),
                    "a converter implements the common mapping contract: " + path);
            assertTrue(text.contains("interface"), "a MapStruct converter is an interface: " + path);
        }
        // Every adapter and application converter sits in the pojo directory of its own context.
        for (Path path : converters) {
            String text = path.toString().replace('\\', '/');
            if (text.contains("/adapter/")) {
                assertTrue(text.contains("/pojo/convertor/"),
                        "a boundary converter sits with the carriers it maps: " + path);
            }
        }
        // Persistence converters are reached through the exposed instance, which keeps one mapping owner.
        for (String[] pair : List.of(
                new String[] {"knowledge/converter/KnowledgeBasePOConverter.java",
                        "knowledge/service/KnowledgeBaseRepositoryImpl.java"},
                new String[] {"knowledge/converter/KnowledgeDocumentPOConverter.java",
                        "knowledge/service/KnowledgeDocumentRepositoryImpl.java"})) {
            assertTrue(read("infrastructure", pair[0]).contains("Mappers.getMapper("), pair[0]);
            assertTrue(read("infrastructure", pair[1]).contains("POConverter.INSTANCE"),
                    pair[1] + " must map through the single converter owner");
        }
    }

    @Test
    void useCasesReachEveryCollaboratorByName() throws IOException {
        String qa = read("application", "knowledge/manage/impl/KnowledgeQaManageImpl.java");
        String removal = read("application", "knowledge/service/KnowledgeRemovalService.java");
        String research = read("application", "research/manage/impl/DeepResearchManageImpl.java");

        assertTrue(qa.contains("@Qualifier(\"knowledgeAnswerService\")"));
        assertTrue(qa.contains("@Qualifier(\"knowledgeVectorService\")"));
        assertTrue(removal.contains("@Qualifier(\"knowledgeVectorService\")"));
        assertTrue(research.contains("@Qualifier(\"deepResearchAgentService\")"));

        for (String path : List.of("knowledge/manage/impl/KnowledgeBaseManageImpl.java",
                "knowledge/manage/impl/KnowledgeDocumentManageImpl.java",
                "knowledge/manage/impl/KnowledgeQaManageImpl.java",
                "research/manage/impl/DeepResearchManageImpl.java")) {
            String text = read("application", path);
            assertTrue(matches(text, "@Service\\(\"\\w+Manage\"\\)"),
                    "a Spring-managed use case needs an explicit bean name: " + path);
            assertTrue(text.contains("@RequiredArgsConstructor"), "injection is declared by the constructor: " + path);
            assertTrue(text.contains("@Slf4j"), "business classes log through @Slf4j: " + path);
            assertTrue(text.contains("@Qualifier("), "every collaborator is injected by name: " + path);
        }

        String validation = read("application", "knowledge/manage/impl/KnowledgeBaseManageImpl.java");
        assertTrue(validation.contains("@Qualifier(\"agentValidationUtils\")"),
                "the hand-off into the domain is validated through the common validator bean");

        // Only the infrastructure module may see a persistence role.
        String adapterAndApplication = join(sourcesOf("adapter")) + join(sourcesOf("application"));
        for (String forbidden : List.of(ROOT + ".infrastructure.", "DAO", "POConverter")) {
            assertFalse(adapterAndApplication.contains(forbidden),
                    "the use-case layers must not reach the persistence stack: " + forbidden);
        }
    }

    @Test
    void obsoletePackagesDoNotSurviveAndTestSurfaceCoversTheMovedContracts() throws IOException {
        for (String obsolete : List.of(
                "adapter:knowledge/dto", "adapter:knowledge/vo", "adapter:knowledge/converter",
                "adapter:research/dto", "adapter:research/vo", "adapter:research/converter",
                "application:knowledge/command", "application:knowledge/exception",
                "application:research/command", "application:research/exception",
                "domain:knowledge/gateway", "domain:research/gateway",
                "infrastructure:knowledge/gateway", "infrastructure:research/gateway",
                "infrastructure:knowledge/repo/dao", "infrastructure:knowledge/repo/po",
                "infrastructure:knowledge/repo/converter")) {
            int separator = obsolete.indexOf(':');
            assertFalse(Files.exists(sources(obsolete.substring(0, separator))
                    .resolve(obsolete.substring(separator + 1))), "obsolete path survived: " + obsolete);
        }

        // The fixtures name a retired type only to forbid it, so a bare substring would be a false alarm.
        List<Path> fixtures = new ArrayList<>();
        for (String module : MODULES) {
            Path root = testSources(module);
            if (Files.isDirectory(root)) {
                fixtures.addAll(sources(root).stream()
                        .filter(path -> !path.getFileName().toString().equals("ArchetypeContractConvergenceTest.java"))
                        .toList());
            }
        }
        String testSurface = join(fixtures);
        for (String retired : List.of("KnowledgeAnswerGateway", "KnowledgeVectorGateway", "DeepResearchAgentGateway",
                "KnowledgeAnswerChatModelGateway", "RagKnowledgeVectorGateway", "AgentFlowDeepResearchAgentGateway",
                "repo.dao", "repo.po", "repo.converter", "application.knowledge.command",
                "application.research.command", "adapter.knowledge.dto", "adapter.knowledge.vo",
                "adapter.research.dto", "adapter.research.vo", "application.knowledge.exception",
                "application.research.exception")) {
            assertFalse(matches(testSurface, "\\b" + retired.replaceAll("\\.", "\\\\.") + "\\b"),
                    "the test surface still names the retired contract: " + retired);
        }
        // The fixtures that bind by type must follow the move; the controller fixtures bind their
        // inbound and outbound knowledge carriers as JSON documents, so `boundaryCarriersLiveInThe
        // PojoDirectories` above is what pins those, not an import.
        for (String moved : List.of("top.egon.cola.archetype.source.agent.application.knowledge.pojo.command",
                "top.egon.cola.archetype.source.agent.common.exception",
                "top.egon.cola.archetype.source.agent.domain.knowledge.service",
                "top.egon.cola.archetype.source.agent.infrastructure.knowledge.dao")) {
            assertTrue(testSurface.contains(moved),
                    "the family fixtures must exercise the moved contract: " + moved);
        }
    }

    @Test
    void definitionResourcesFollowTheMovedTree() throws IOException {
        Path definition = Path.of("..", "..", "..", "definitions", "egon-cola-archetype-agent");
        // A generated project has no sibling definitions directory; the source repository owns this check.
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.isDirectory(definition),
                "the definition tree is only reachable from the archetype source repository");
        List<Path> resources;
        try (Stream<Path> paths = Files.walk(definition)) {
            resources = paths.filter(Files::isRegularFile)
                    .filter(path -> !path.toString().contains("/target/"))
                    .filter(path -> {
                        String name = path.getFileName().toString();
                        return name.endsWith(".xml") || name.endsWith(".properties")
                                || name.endsWith(".groovy") || name.endsWith(".md");
                    })
                    .toList();
        }
        assertFalse(resources.isEmpty(), "definition resources must be discoverable");
        List<Path> documented = new ArrayList<>(resources);
        documented.addAll(List.of(Path.of("..", "README.md"), Path.of("..", "README.zh-CN.md")));
        for (Path path : documented) {
            // The generated-project verifier names the retired paths on purpose to forbid them.
            if (path.getFileName().toString().equals("verify.groovy")) {
                continue;
            }
            String text = read(path);
            for (String stale : List.of(ROOT + ".application.knowledge.command", ROOT + ".application.research.command",
                    ROOT + ".adapter.knowledge.dto", ROOT + ".adapter.knowledge.vo",
                    ROOT + ".adapter.knowledge.converter", ROOT + ".application.knowledge.exception",
                    ROOT + ".application.research.exception", ROOT + ".domain.knowledge.gateway",
                    ROOT + ".domain.research.gateway", ROOT + ".infrastructure.knowledge.gateway",
                    ROOT + ".infrastructure.knowledge.repo.dao", ROOT + ".infrastructure.knowledge.repo.po",
                    "application/knowledge/command", "application/research/command",
                    "adapter/knowledge/dto", "adapter/knowledge/vo", "domain/knowledge/gateway",
                    "infrastructure/knowledge/gateway", "infrastructure/knowledge/repo/dao",
                    "infrastructure/knowledge/repo/po", "infrastructure/research/gateway",
                    "KnowledgeAnswerGateway", "KnowledgeVectorGateway", "DeepResearchAgentGateway",
                    "KnowledgeAnswerChatModelGateway", "RagKnowledgeVectorGateway",
                    "AgentFlowDeepResearchAgentGateway")) {
                assertFalse(text.contains(stale),
                        "definition resource still publishes the retired contract: " + stale + " in " + path);
            }
        }

        // The verifier must expect the moved tree, which is what keeps a generated project honest.
        String verifier = read(definition.resolve("src/test/resources/projects/basic/verify.groovy"));
        assertTrue(verifier.contains("infrastructure/research/service/impl/AgentFlowDeepResearchAgentServiceImpl.java"),
                "the verifier must expect the domain service implementation behind the Service boundary");
        assertTrue(verifier.contains("adapter/research/pojo/dto/StartDeepResearchRequest.java"),
                "the verifier must expect the inbound carrier in the pojo directory");
        assertTrue(verifier.contains("adapter/research/pojo/vo/DeepResearchEventVO.java"),
                "the verifier must expect the outbound carrier in the pojo directory");
    }

    private static void assertCodes(String module, String relative, String... constants) {
        String text = read(module, relative);
        for (int index = 0; index < constants.length; index++) {
            String constant = constants[index];
            assertTrue(matches(text, "\n\\s+" + constant + "\\(" + index + ", "),
                    "explicit code drift for " + constant + " in " + relative);
        }
    }

    private static String joinMain() throws IOException {
        List<Path> all = new ArrayList<>();
        for (String module : MODULES) {
            all.addAll(sourcesOf(module));
        }
        return join(all);
    }

    private static List<Path> mainSources() throws IOException {
        List<Path> all = new ArrayList<>();
        for (String module : MODULES) {
            all.addAll(sourcesOf(module));
        }
        return all;
    }

    private static List<Path> sourcesOf(String module) throws IOException {
        Path root = sources(module);
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        return sources(root);
    }

    private static Path sources(String module) {
        return Path.of("..").resolve("egon-cola-source-agent-" + module).resolve(MAIN)
                .resolve(ROOT.replace('.', '/')).resolve(module);
    }

    private static Path testSources(String module) {
        return Path.of("..").resolve("egon-cola-source-agent-" + module).resolve(TEST)
                .resolve(ROOT.replace('.', '/'));
    }

    private static String read(String module, String relative) {
        return read(sources(module).resolve(relative));
    }

    private static String resource(String module, String relative) {
        return read(Path.of("..", "egon-cola-source-agent-" + module, "src/main/resources").resolve(relative));
    }

    private static void assertExists(String module, String relative) {
        Path path = sources(module).resolve(relative);
        assertTrue(Files.exists(path), "expected contract path is missing: " + path);
    }

    private static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new AssertionError("cannot read contract path " + path, exception);
        }
    }

    private static List<Path> sources(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(path -> path.toString().endsWith(".java")).sorted().toList();
        }
    }

    private static String join(List<Path> paths) {
        StringBuilder builder = new StringBuilder();
        for (Path path : paths) {
            builder.append(read(path));
        }
        return builder.toString();
    }

    private static boolean matches(String text, String regex) {
        return java.util.regex.Pattern.compile(regex).matcher(text).find();
    }
}
