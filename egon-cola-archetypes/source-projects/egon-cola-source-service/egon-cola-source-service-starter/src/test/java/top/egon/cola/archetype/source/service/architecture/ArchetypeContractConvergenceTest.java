package top.egon.cola.archetype.source.service.architecture;

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
 * Generated-project contract for the Service archetype layering and common component rules.
 *
 * <p>Every assertion is a source-shape check so the contract is observable before the Spring
 * context exists; the runtime behaviour behind each rule is covered by the family fixtures.</p>
 */
class ArchetypeContractConvergenceTest {

    private static final String ROOT = "top.egon.cola.archetype.source.service";

    private static final List<String> MODULES = List.of(
            "common", "facade", "domain", "application", "infrastructure", "adapter", "starter");

    private static final Path MAIN = Path.of("src/main/java");

    private static final Path TEST = Path.of("src/test/java");

    @Test
    void domainOnlyExposesServicesAndKeepsPortsOutOfDomain() throws IOException {
        String all = join(mainSources());

        for (String forbidden : List.of(
                "OrganizationDirectoryPort", "CourseEventPublisher", "ExamEventPublisher",
                "EventPublisher", "CachePort", "QueryGateway", "Gateway", "Assembler")) {
            assertFalse(all.contains(forbidden), "domain port capability must be a domain Service: " + forbidden);
        }
        assertFalse(Files.exists(sources("domain").resolve("client")), "domain.client must be gone");
        assertFalse(Files.exists(sources("domain").resolve("course/event")));
        assertFalse(Files.exists(sources("domain").resolve("exam/event")));

        assertExists("domain", "course/service/OrganizationDirectoryService.java");
        assertExists("domain", "course/service/CourseEventService.java");
        assertExists("domain", "exam/service/ExamEventService.java");
        assertExists("domain", "course/vos/OrganizationUserBO.java");
        assertExists("domain", "course/vos/OrganizationSchoolClassBO.java");

        for (Path path : sourcesOf("domain")) {
            String text = read(path);
            assertFalse(text.contains(ROOT + ".infrastructure."),
                    "domain must not depend on infrastructure: " + path);
            assertFalse(text.contains(ROOT + ".application."),
                    "domain must not depend on application: " + path);
            assertFalse(text.contains("import org.springframework."),
                    "domain must stay framework-free; infrastructure publishes the validator beans: " + path);
        }
    }

    @Test
    void externalClientsLiveInInfrastructureClientAndImpl() throws IOException {
        assertExists("infrastructure", "client/organization/OrganizationDirectoryClient.java");
        assertExists("infrastructure", "client/organization/impl/LocalOrganizationDirectoryClientImpl.java");
        assertExists("infrastructure", "client/organization/impl/NativeOrganizationDirectoryClientImpl.java");
        assertExists("infrastructure", "course/service/impl/OrganizationDirectoryServiceImpl.java");

        assertFalse(Files.exists(sources("infrastructure")
                .resolve("client/organization/LocalOrganizationDirectoryStub.java")));
        assertFalse(Files.exists(sources("infrastructure")
                .resolve("client/organization/NativeOrganizationDirectoryClient.java")));
        String all = join(mainSources());
        assertFalse(matches(all, "NativeOrganizationDirectoryClient(?!Impl)"),
                "the client implementation must use the ClientImpl suffix");
        assertFalse(all.contains("LocalOrganizationDirectoryStub"));

        String service = read("infrastructure", "course/service/impl/OrganizationDirectoryServiceImpl.java");
        assertTrue(service.contains("implements OrganizationDirectoryService"));
        assertTrue(service.contains("@Qualifier(\"organizationDirectoryClient\")"),
                "the ServiceImpl must reach the dependency through the named Client");
        assertTrue(service.contains("organizationDirectoryClient.getUser("));
        assertTrue(service.contains("organizationDirectoryClient.getSchoolClass("));
        assertFalse(service.contains("RabbitTemplate"), "no transport detail may leak into the ServiceImpl");
    }

    @Test
    void applicationAndAdapterCarriersUseThePojoDirectories() throws IOException {
        for (String path : List.of(
                "application:course/pojo/command/CreateCourseCommand.java",
                "application:course/pojo/command/ScheduleCourseCommand.java",
                "application:course/pojo/query/GetCourseQuery.java",
                "application:course/pojo/query/PageCourseQuery.java",
                "application:course/pojo/result/CourseResult.java",
                "application:course/pojo/result/CourseScheduleResult.java",
                "application:course/pojo/convertor/CourseApplicationConverter.java",
                "application:exam/pojo/command/CreateExamCommand.java",
                "application:exam/pojo/query/GetExamQuery.java",
                "application:exam/pojo/result/ExamDetailResult.java",
                "application:exam/pojo/convertor/ExamApplicationConverter.java",
                "application:pojo/result/PageResult.java",
                "adapter:exam/pojo/dto/RecordScoreMessage.java",
                "adapter:pojo/convertor/EvaluationFacadeConverter.java",
                "adapter:pojo/dto/FacadeFailureDTO.java")) {
            int separator = path.indexOf(':');
            assertExists(path.substring(0, separator), path.substring(separator + 1));
        }

        for (String obsolete : List.of(
                "application:course/command", "application:course/query", "application:course/result",
                "application:course/converter", "application:exam/command", "application:exam/query",
                "application:exam/result", "application:exam/converter", "application:result",
                "application:exceptions", "adapter:exam/dto", "adapter:exam/validators",
                "adapter:course/validators", "adapter:course/converter", "adapter:exam/converter")) {
            int separator = obsolete.indexOf(':');
            assertFalse(Files.exists(sources(obsolete.substring(0, separator))
                    .resolve(obsolete.substring(separator + 1))), "obsolete carrier package: " + obsolete);
        }

        String all = join(mainSources());
        for (String forbidden : List.of(
                ROOT + ".application.course.command", ROOT + ".application.course.query",
                ROOT + ".application.course.result", ROOT + ".application.course.converter",
                ROOT + ".application.exam.command", ROOT + ".application.exam.query",
                ROOT + ".application.exam.result", ROOT + ".application.exam.converter",
                ROOT + ".application.result", ROOT + ".application.exceptions",
                ROOT + ".adapter.exam.dto")) {
            assertFalse(all.contains(forbidden), "old carrier package still referenced: " + forbidden);
        }
        assertFalse(all.contains("Assembler"), "duplicate snapshot assembly must not exist");
    }

    @Test
    void technicalRepositoryStaysIndependentOfDaoPoAndConverter() throws IOException {
        for (String path : List.of(
                "course/dao/CourseDAO.java", "course/dao/CourseScheduleDAO.java",
                "course/po/CoursePO.java", "course/po/CourseSchedulePO.java",
                "course/converter/CourseConverter.java", "course/converter/CourseScheduleConverter.java",
                "exam/dao/ExamDAO.java", "exam/dao/ExamPaperDAO.java", "exam/dao/ScoreDAO.java",
                "exam/po/ExamPO.java", "exam/po/ExamPaperPO.java", "exam/po/ScorePO.java",
                "exam/converter/ExamConverter.java", "exam/converter/ExamPaperConverter.java",
                "exam/converter/ScoreConverter.java")) {
            assertExists("infrastructure", path);
        }
        assertFalse(Files.exists(sources("infrastructure").resolve("course/repo/dao")));
        assertFalse(Files.exists(sources("infrastructure").resolve("exam/repo/po")));
        assertFalse(Files.exists(sources("infrastructure").resolve("course/repo/converter")));

        String all = join(mainSources());
        for (String forbidden : List.of(
                ROOT + ".infrastructure.course.repo.dao", ROOT + ".infrastructure.exam.repo.dao",
                ROOT + ".infrastructure.course.repo.po", ROOT + ".infrastructure.exam.repo.po",
                ROOT + ".infrastructure.course.repo.converter", ROOT + ".infrastructure.exam.repo.converter")) {
            assertFalse(all.contains(forbidden), "moved persistence package still referenced: " + forbidden);
        }

        for (String xml : List.of("mybatis/mapper/course/CourseDAO.xml",
                "mybatis/mapper/course/CourseScheduleDAO.xml")) {
            String text = resource("infrastructure", xml);
            assertTrue(text.contains(ROOT + ".infrastructure.course.dao."), xml);
            assertTrue(text.contains(ROOT + ".infrastructure.course.po."), xml);
            assertFalse(text.contains("repo.dao"), xml);
        }
        for (String xml : List.of("mybatis/mapper/exam/ExamDAO.xml",
                "mybatis/mapper/exam/ExamPaperDAO.xml", "mybatis/mapper/exam/ScoreDAO.xml")) {
            String text = resource("infrastructure", xml);
            assertTrue(text.contains(ROOT + ".infrastructure.exam.dao."), xml);
            assertTrue(text.contains(ROOT + ".infrastructure.exam.po."), xml);
            assertFalse(text.contains("repo.dao"), xml);
        }

        String scan = read("starter", "EvaluationServiceApplication.java");
        assertTrue(scan.contains(ROOT + ".infrastructure.course.dao"));
        assertTrue(scan.contains(ROOT + ".infrastructure.exam.dao"));
        assertFalse(scan.contains("repo.dao"));
    }

    @Test
    void sqlPredicatesTenantAndVersionSemanticsStayUnchanged() {
        for (String xml : List.of(
                "mybatis/mapper/course/CourseDAO.xml", "mybatis/mapper/course/CourseScheduleDAO.xml",
                "mybatis/mapper/exam/ExamDAO.xml", "mybatis/mapper/exam/ExamPaperDAO.xml",
                "mybatis/mapper/exam/ScoreDAO.xml")) {
            String text = resource("infrastructure", xml);
            assertTrue(text.contains("deleted_at IS NULL"), xml);
            assertTrue(text.contains("MP_OPTLOCK_VERSION_ORIGINAL"), xml);
            assertFalse(text.contains("${"), "no string interpolation may enter the mapper SQL: " + xml);
        }
    }

    @Test
    void cachedReadsAndWritesStayOnTheSpringProxyOfTheSameRegionKey() throws IOException {
        String course = read("infrastructure", "course/repo/CourseRepository.java");
        assertTrue(course.contains("@CacheConfig(cacheNames = \"CoursePO\")"));
        assertTrue(course.contains("public CoursePO findCachedById("));
        assertTrue(course.contains("public boolean updateCachedById("));
        assertTrue(course.contains("condition = \"#result\""), "false write must not evict");

        String domainService = read("infrastructure", "course/service/impl/CourseDomainServiceImpl.java");
        assertTrue(domainService.contains("findCachedById("), "single id read must use the cached entry point");
        assertTrue(domainService.contains("updateCachedById("), "cached entity write must invalidate");
        assertFalse(domainService.contains("courseRepository.updateById(po)"),
                "the cached entity must not be written beside its cache region");

        for (Path path : sourcesOf("infrastructure")) {
            String text = read(path);
            assertFalse(text.contains("this.findCachedById("), "cache entry must not be self-invoked: " + path);
            assertFalse(text.contains("this.updateCachedById("), "cache entry must not be self-invoked: " + path);
        }

        // When the component is switched off the host owns the manager, so no JSR-107 provider can hijack the region.
        assertTrue(read("infrastructure", "config/RedisConfig.java").contains("ConcurrentMapCacheManager"),
                "a disabled cache component still needs a deterministic CacheManager for the cached entry points");

        // Only the single-id Course read is cached; compound queries keep their explicit uncached SQL.
        for (String path : List.of("course/repo/CourseScheduleRepository.java", "exam/repo/ExamRepository.java",
                "exam/repo/ExamPaperRepository.java", "exam/repo/ScoreRepository.java")) {
            assertFalse(read("infrastructure", path).contains("@CacheConfig"),
                    "unselected read must stay uncached: " + path);
        }
        assertFalse(Files.exists(sources("infrastructure").resolve("course/cache")));
        assertFalse(Files.exists(sources("infrastructure").resolve("exam/cache")));
    }

    @Test
    void validatorsExtendBaseValidatorAndNeverWriteSideEffects() throws IOException {
        List<Path> validators = mainSources().stream()
                .filter(path -> path.getFileName().toString().endsWith("Validator.java"))
                .toList();
        assertEquals(6, validators.size(), "one application, domain and persistence validator set per boundary");
        for (Path path : validators) {
            String text = read(path);
            assertTrue(text.contains("extends BaseValidator"), "validator contract: " + path);
            assertTrue(text.contains("getValidationUtils"), "validator must expose the common facade: " + path);
            for (String forbidden : List.of("RedisTemplate", "RabbitTemplate", "Idempotency", "Repository",
                    "Mapper", "CachePort", "updateById(", "save(", "insert(")) {
                assertFalse(text.contains(forbidden),
                        "validation must stay side-effect free: " + forbidden + " in " + path);
            }
        }
        // This family declares no claim/release capability, so no idempotency service may appear.
        assertFalse(join(mainSources()).contains("Idempotency"));
    }

    @Test
    void manageDependsOnlyOnDomainServices() throws IOException {
        List<Path> manages = mainSources().stream()
                .filter(path -> path.getFileName().toString().endsWith("ManageImpl.java"))
                .toList();
        assertEquals(3, manages.size(), "course, exam and score use-case boundary");
        for (Path path : manages) {
            String text = read(path);
            for (String forbidden : List.of("RabbitTemplate", "RedisTemplate", "EventPublisher",
                    "CachePort", "QueryGateway", "MqMessageService", "Repository", "DAO", "Mapper")) {
                assertFalse(text.contains(forbidden),
                        "Manage must not reach infrastructure: " + forbidden + " in " + path);
            }
            assertTrue(text.contains("@Slf4j"), "business classes log through @Slf4j: " + path);
        }
        assertTrue(read("application", "course/manage/impl/CourseManageImpl.java")
                .contains("courseEventService.courseScheduled("));
        assertTrue(read("application", "course/manage/impl/CourseManageImpl.java")
                .contains("@Qualifier(\"courseEventService\")"));
        assertTrue(read("application", "exam/manage/impl/ExamManageImpl.java")
                .contains("examEventService.examPublished("));
        assertTrue(read("application", "exam/manage/impl/ScoreManageImpl.java")
                .contains("examEventService.scoreRecorded("));
    }

    @Test
    void mqTopologyAndRoutingComeFromOneEnumInsteadOfTypePrefixGuessing() {
        assertExists("infrastructure", "mq/MqRouteEnum.java");
        assertExists("infrastructure", "mq/MqMessageService.java");
        assertExists("infrastructure", "mq/impl/RabbitMqMessageServiceImpl.java");
        assertExists("infrastructure", "course/service/impl/CourseEventServiceImpl.java");
        assertExists("infrastructure", "exam/service/impl/ExamEventServiceImpl.java");

        for (String obsolete : List.of(
                "course/mq/RabbitCourseEventPublisher.java", "course/mq/LocalCourseEventPublisher.java",
                "exam/mq/RabbitExamEventPublisher.java", "exam/mq/LocalExamEventPublisher.java")) {
            assertFalse(Files.exists(sources("infrastructure").resolve(obsolete)),
                    "per-event publishers fold into one MQ boundary: " + obsolete);
        }

        String route = read("infrastructure", "mq/MqRouteEnum.java");
        assertTrue(route.contains("enum MqRouteEnum implements EgonEnum"));
        for (String field : List.of("exchangeProperty", "queueProperty", "routingKeyProperty",
                "deadLetterRoutingKey", "durable", "payloadType", "schemaVersion")) {
            assertTrue(route.contains(field), "route contract field missing: " + field);
        }
        for (String constant : List.of("COURSE_SCHEDULED", "EXAM_PUBLISHED", "SCORE_RECORDED", "SCORE_COMMAND")) {
            assertTrue(matches(route, "\n\\s+" + constant + "\\(\\d+, "),
                    "route constant must carry an explicit code: " + constant);
        }
        // The existing wire targets stay verbatim so a generated project keeps its broker topology.
        for (String key : List.of(
                "app.integrations.rabbitmq.exchange",
                "app.integrations.rabbitmq.course-scheduled-routing-key",
                "app.integrations.rabbitmq.exam-published-routing-key",
                "app.integrations.rabbitmq.score-recorded-routing-key",
                "app.integrations.rabbitmq.score-command-routing-key",
                "app.integrations.rabbitmq.score-command-queue")) {
            assertTrue(route.contains(key), "route must reuse the existing configuration key: " + key);
        }
        assertTrue(route.contains("null"), "this family declares no dead-letter target");

        String publish = read("infrastructure", "mq/impl/RabbitMqMessageServiceImpl.java");
        assertTrue(publish.contains("convertAndSend("));
        assertTrue(publish.contains("getPayloadType()"), "payload type must be verified before publish");
        assertTrue(publish.contains("getSchemaVersion()"));
        assertTrue(publish.contains("isActualTransactionActive("),
                "publication must stay deferred to the commit point");
        assertTrue(publish.contains("havingValue = \"true\""), "the broker bean stays opt-in");

        String local = read("infrastructure", "config/LocalAdapterConfiguration.java");
        assertTrue(local.contains("@Bean(\"mqMessageService\")"),
                "a broker-free profile still boots on the same MQ boundary");

        String configuration = read("infrastructure", "config/RabbitMqConfiguration.java");
        assertTrue(configuration.contains("MqRouteEnum"), "topology must reuse the same route constants");

        String consumer = read("adapter", "exam/mq/RecordScoreConsumer.java");
        assertTrue(consumer.contains("${app.integrations.rabbitmq.score-command-queue}"),
                "the consumer still binds the declared queue property");

        String all = joinMain();
        assertFalse(matches(all, "\\.startsWith\\(\"(course|exam|score)\\."),
                "routing must not guess from the event type");
    }

    @Test
    void eventPayloadsAndWireCarriersImplementTheCommonCarrierContract() {
        for (String path : List.of(
                "infrastructure:course/mq/message/CourseScheduledMessage.java",
                "infrastructure:exam/mq/message/ExamPublishedMessage.java",
                "infrastructure:exam/mq/message/ScoreRecordedMessage.java",
                "adapter:exam/pojo/dto/RecordScoreMessage.java")) {
            int separator = path.indexOf(':');
            String text = read(path.substring(0, separator), path.substring(separator + 1));
            assertTrue(text.contains("implements BasePojo"), "wire carrier contract missing: " + path);
        }
        String message = read("infrastructure", "course/mq/message/CourseScheduledMessage.java");
        assertTrue(message.contains("Instant startsAt"), "time values stay on java.time");
        assertFalse(joinMain().contains("java.util.Date"), "legacy date type must not appear");
    }

    @Test
    void customExceptionsAreCommonScopedAndCarryTheStringStatusCode() throws IOException {
        for (String path : List.of(
                "exception/EvaluationBizException.java",
                "exception/EvaluationNotFoundException.java",
                "exception/EvaluationDomainException.java",
                "exception/EvaluationPortException.java",
                "exception/ApplicationException.java",
                "exception/ExternalDependencyException.java",
                "exception/ConfigDecryptException.java")) {
            assertExists("common", path);
        }
        assertFalse(Files.exists(sources("common").resolve("exceptions")));
        for (String obsolete : List.of(
                "common/EvaluationDomainException.java", "common/EvaluationPortException.java",
                "common/EvaluationDomainErrorCode.java", "client/ExternalDependencyException.java",
                "client/ExternalDependencyFailure.java")) {
            assertFalse(Files.exists(sources("domain").resolve(obsolete)), "obsolete path: " + obsolete);
        }
        assertFalse(Files.exists(sources("application").resolve("exceptions")));
        assertFalse(Files.exists(sources("starter").resolve("config/encryption/ConfigDecryptException.java")));

        for (Path path : mainSources()) {
            assertFalse(read(path).contains("extends RuntimeException"),
                    "custom exceptions must re-root on the common hierarchy: " + path);
        }
        String biz = read("common", "exception/EvaluationBizException.java");
        assertTrue(biz.contains("extends BusinessException"), "business failure re-roots on BusinessException");
        assertTrue(biz.contains("getStatus()"), "the stable wire code stays a String status");
        assertTrue(read("common", "exception/ConfigDecryptException.java").contains("extends CommonException"),
                "technical failure re-roots on CommonException");
        assertTrue(read("common", "exception/ApplicationException.java")
                .contains("public ApplicationErrorCode code()"), "the typed accessor keeps its original name");
        assertTrue(read("common", "exception/EvaluationPortException.java").contains("public String operation()"));
        String external = read("common", "exception/ExternalDependencyException.java");
        assertTrue(external.contains("public String dependency()"));
        assertTrue(external.contains("public ExternalDependencyFailure failure()"));
        assertTrue(external.contains("public String externalCode()"));

        String handler = read("adapter", "handler/GlobalFacadeExceptionHandler.java");
        assertTrue(handler.contains("getStatus()"), "the wire code is published through getStatus");
        assertFalse(handler.contains("Failure.getCode()"), "the int code must not become the wire code");
    }

    @Test
    void handwrittenEnumsAndConvertersImplementTheCommonContracts() throws IOException {
        for (Path path : mainSources()) {
            String text = read(path);
            String name = path.getFileName().toString();
            if (text.contains("\npublic enum ") || text.contains("\nenum ")) {
                assertTrue(matches(text, "implements [^{]*\\b(EgonEnum|ErrorStatus|EvaluationError)\\b"),
                        "handwritten enum must implement EgonEnum: " + path);
            }
            if (name.endsWith("Converter.java") || name.endsWith("Convertor.java")) {
                assertTrue(text.contains("BaseConverter<") || text.contains("BaseForwardConverter<"),
                        "semantic mapping must use the common converter: " + name);
                assertTrue(text.contains("@Mapper"), "mapping must be generated: " + name);
            }
        }
        assertFalse(joinMain().contains(".ordinal()"), "ordinal must never act as the code");

        assertExists("common", "enums/ApplicationErrorCode.java");
        assertExists("common", "enums/EvaluationErrorCode.java");
        assertExists("common", "enums/EvaluationDomainErrorCode.java");
        assertExists("common", "enums/ExternalDependencyFailure.java");
        assertExists("common", "enums/EvaluationError.java");

        assertCodes("common", "enums/ApplicationErrorCode.java",
                "COURSE_NOT_FOUND", "COURSE_CODE_DUPLICATED", "EXAM_NOT_FOUND", "EXAM_PAPER_NOT_FOUND",
                "SCORE_NOT_FOUND", "VALIDATION_FAILED", "BUSINESS_REJECTED", "INFRASTRUCTURE_FAILURE");
        assertCodes("common", "enums/EvaluationDomainErrorCode.java",
                "VALIDATION_FAILED", "COURSE_INACTIVE", "COURSE_CODE_DUPLICATED", "SCHEDULE_CONFLICT",
                "EXAM_NOT_PUBLISHABLE", "SCORE_OUT_OF_RANGE", "SCORE_DUPLICATED", "PORT_FAILURE");
        assertCodes("common", "enums/EvaluationErrorCode.java",
                "COURSE_NOT_FOUND", "COURSE_CODE_DUPLICATED", "SCHEDULE_CONFLICT", "EXAM_NOT_FOUND",
                "EXAM_PAPER_NOT_FOUND", "EXAM_NOT_PUBLISHABLE", "SCORE_NOT_FOUND", "SCORE_DUPLICATED",
                "VALIDATION_FAILED", "INFRASTRUCTURE_FAILURE");
        assertCodes("common", "enums/ExternalDependencyFailure.java",
                "NOT_FOUND", "BUSINESS_REJECTED", "VALIDATION_FAILED", "UNAVAILABLE", "TIMEOUT",
                "CONTRACT_INCOMPATIBLE", "SERVICE_FAILURE");
        assertCodes("common", "enums/YesNoEnum.java", "YES", "NO");
        assertCodes("domain", "course/enums/CourseStatus.java", "ACTIVE", "INACTIVE");
        assertCodes("domain", "course/enums/CourseScheduleStatus.java", "SCHEDULED", "CANCELLED");
        assertCodes("domain", "exam/enums/ExamStatus.java", "DRAFT", "PUBLISHED", "CLOSED");
        assertCodes("domain", "exam/enums/ExamPaperStatus.java", "DRAFT", "PUBLISHED");
        assertCodes("domain", "exam/enums/ScoreStatus.java", "RECORDED", "CANCELLED");

        String courseConverter = read("application", "course/pojo/convertor/CourseApplicationConverter.java");
        assertTrue(courseConverter.contains("@AnnotateWith"), "generated bean name must be explicit");
        assertTrue(courseConverter.contains("componentModel = \"spring\""));
        assertTrue(courseConverter.contains("BaseForwardConverter<"), "projection is one-way");
        assertTrue(courseConverter.contains("CourseResult toResult(Course"),
                "the original callable contract stays available");
        assertFalse(matches(courseConverter, "new CourseResult\\("), "no manual bulk copying");
        assertFalse(matches(read("application", "exam/pojo/convertor/ExamApplicationConverter.java"),
                "new (ExamDetailResult|ExamPaperResult|ScoreResult)\\("));
    }

    @Test
    void nativeFacadeChainAndBeanNamesStayStable() throws IOException {
        String all = join(sourcesOf("adapter"));
        assertEquals(3, count(all, "@EgonRpcProvider"), "one provider implementation per facade");
        assertFalse(Files.exists(sources("adapter").resolve("course/rpc")));
        assertFalse(Files.exists(sources("adapter").resolve("exam/rpc")));
        for (String removed : List.of("CourseRpcProvider", "ExamRpcProvider", "ScoreRpcProvider")) {
            assertFalse(all.contains(removed), "the wrapper provider folds into the facade impl: " + removed);
        }
        for (Path path : sourcesOf("facade")) {
            String text = read(path);
            for (String forbidden : List.of(ROOT + ".adapter.", ROOT + ".application.", ROOT + ".infrastructure.")) {
                assertFalse(text.contains(forbidden),
                        "facade must stay a self-contained contract layer: " + forbidden + " in " + path);
            }
        }
        assertTrue(all.contains("@Qualifier(\"courseManage\")"));
        assertTrue(all.contains("@Qualifier(\"evaluationExamManage\")"));
        assertTrue(all.contains("@Qualifier(\"scoreManage\")"));
    }

    @Test
    void obsoletePackagesDoNotSurviveAndTestSurfaceCoversTheMovedContracts() throws IOException {
        for (String obsolete : List.of(
                "common:exceptions", "domain:client", "domain:course/event", "domain:exam/event",
                "application:exceptions", "application:result", "adapter:exam/dto",
                "infrastructure:course/repo/dao", "infrastructure:exam/repo/dao",
                "infrastructure:course/repo/po", "infrastructure:exam/repo/po",
                "infrastructure:course/repo/converter", "infrastructure:exam/repo/converter",
                "infrastructure:course/mq/RabbitCourseEventPublisher.java",
                "infrastructure:exam/mq/RabbitExamEventPublisher.java",
                "starter:config/encryption/ConfigDecryptException.java")) {
            int separator = obsolete.indexOf(':');
            assertFalse(Files.exists(sources(obsolete.substring(0, separator))
                    .resolve(obsolete.substring(separator + 1))), "obsolete path survived: " + obsolete);
        }

        String starterFixtures = join(testSourcesOf("starter").stream()
                .filter(path -> !path.getFileName().toString().equals("ArchetypeContractConvergenceTest.java"))
                .toList());
        for (String stale : List.of("OrganizationDirectoryPort", "CourseEventPublisher",
                "ExamEventPublisher", "LocalOrganizationDirectoryStub", "repo.dao", "repo.po",
                "application.exceptions", "common.exceptions", "domain.client")) {
            assertFalse(starterFixtures.contains(stale),
                    "starter test surface still names the retired contract: " + stale);
        }
        String infrastructureFixtures = join(testSourcesOf("infrastructure"));
        assertTrue(infrastructureFixtures.contains("RecordingMqMessageService"),
                "the local MQ fake stays under test instead of a production publisher");
        assertFalse(infrastructureFixtures.contains("RabbitCourseEventPublisher"));
        assertFalse(infrastructureFixtures.contains("LocalExamEventPublisher"));
    }

    @Test
    void definitionResourcesFollowTheMovedTree() throws IOException {
        Path definition = Path.of("..", "..", "..", "definitions", "egon-cola-archetype-service");
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
        for (Path path : resources) {
            String text = read(path);
            for (String stale : List.of(
                    ROOT + ".application.course.command", ROOT + ".application.result",
                    ROOT + ".application.exceptions", ROOT + ".common.exceptions",
                    ROOT + ".domain.client", ROOT + ".domain.course.event", ROOT + ".domain.exam.event",
                    ROOT + ".infrastructure.course.repo.dao", ROOT + ".infrastructure.exam.repo.dao",
                    "RabbitCourseEventPublisher", "LocalExamEventPublisher")) {
                assertFalse(text.contains(stale),
                        "definition resource still publishes the retired contract: " + stale + " in " + path);
            }
        }
    }

    private static void assertCodes(String module, String relative, String... constants) {
        String text = read(module, relative);
        for (int index = 0; index < constants.length; index++) {
            String constant = constants[index];
            assertTrue(matches(text, "\n\\s+" + constant + "\\(" + index + ", "),
                    "explicit code drift for " + constant + " in " + relative);
        }
    }

    private static String joinMain() {
        try {
            return join(mainSources());
        } catch (IOException exception) {
            throw new AssertionError("cannot read the main source closure", exception);
        }
    }

    private static List<Path> mainSources() throws IOException {
        List<Path> all = new ArrayList<>();
        for (String module : MODULES) {
            all.addAll(sourcesOf(module));
        }
        return all;
    }

    private static List<Path> sourcesOf(String module) throws IOException {
        return sources(sources(module));
    }

    private static Path sources(String module) {
        return Path.of("..").resolve("egon-cola-source-service-" + module).resolve(MAIN)
                .resolve(ROOT.replace('.', '/')).resolve(module);
    }

    private static List<Path> testSourcesOf(String module) throws IOException {
        return sources(Path.of("..").resolve("egon-cola-source-service-" + module).resolve(TEST));
    }

    private static String read(String module, String relative) {
        return read(sources(module).resolve(relative));
    }

    private static String resource(String module, String relative) {
        return read(Path.of("..", "egon-cola-source-service-" + module, "src/main/resources").resolve(relative));
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

    private static int count(String text, String needle) {
        int total = 0;
        for (int index = text.indexOf(needle); index >= 0; index = text.indexOf(needle, index + 1)) {
            total++;
        }
        return total;
    }
}
