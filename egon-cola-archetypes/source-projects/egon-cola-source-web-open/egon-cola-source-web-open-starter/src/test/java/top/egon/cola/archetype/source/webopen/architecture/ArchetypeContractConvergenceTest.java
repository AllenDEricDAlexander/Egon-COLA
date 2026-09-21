package top.egon.cola.archetype.source.webopen.architecture;

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
import top.egon.cola.archetype.source.webopen.common.exception.OrganizationApplicationException;
import top.egon.cola.archetype.source.webopen.common.enums.OrganizationFailureType;
import top.egon.cola.archetype.source.webopen.common.enums.OrganizationDomainErrorCode;
import top.egon.cola.archetype.source.webopen.common.exception.OrganizationDomainException;
import top.egon.cola.archetype.source.webopen.common.exception.OrganizationPortException;
import top.egon.cola.archetype.source.webopen.common.exception.ExternalDependencyException;
import top.egon.cola.archetype.source.webopen.common.enums.ExternalDependencyFailure;
import top.egon.cola.archetype.source.webopen.common.exception.ConfigDecryptException;
import top.egon.cola.archetype.source.webopen.common.exception.RetryableOrganizationMessageException;
import top.egon.cola.archetype.source.webopen.domain.service.CommandIdempotencyService;
import top.egon.cola.archetype.source.webopen.domain.service.OrganizationEventService;
import top.egon.cola.archetype.source.webopen.domain.teaching.service.EvaluationQueryService;
import top.egon.cola.archetype.source.webopen.domain.teaching.vos.EvaluationCourseBO;
import top.egon.cola.archetype.source.webopen.domain.teaching.vos.EvaluationExamBO;
import top.egon.cola.archetype.source.webopen.domain.teaching.vos.EvaluationScoreBO;
import top.egon.cola.archetype.source.webopen.application.teaching.pojo.convertor.GradeConverter;
import top.egon.cola.archetype.source.webopen.application.user.pojo.convertor.PermissionConverter;
import top.egon.cola.archetype.source.webopen.infrastructure.service.impl.InMemoryCommandIdempotencyServiceImpl;
import top.egon.cola.archetype.source.webopen.infrastructure.client.evaluation.impl.GrpcEvaluationQueryClientImpl;
import top.egon.cola.archetype.source.webopen.infrastructure.client.evaluation.impl.LocalEvaluationQueryClientImpl;

/**
 * Generated-project contract for the Web archetype layering and common component rules.
 *
 * <p>Every assertion is a source-shape check, so the contract is observable before the Spring
 * context exists; the runtime behaviour behind each rule is covered by the family fixtures.</p>
 */
class ArchetypeContractConvergenceTest {

    private static final String ROOT = "top.egon.cola.archetype.source.webopen";

    private static final List<String> MODULES = List.of(
            "common", "facade", "domain", "application", "infrastructure", "adapter", "starter");

    private static final Path MAIN = Path.of("src/main/java");

    private static final Path TEST = Path.of("src/test/java");

    /**
     * Converters that assemble one target from several sources. The single-source common base
     * interface cannot express them, so they stay plain generated mappers.
     */
    private static final List<String> MULTI_SOURCE_CONVERTERS = List.of(
            "application:teaching/pojo/convertor/GradeApplicationConverter.java",
            "application:teaching/pojo/convertor/SchoolClassApplicationConverter.java",
            "application:user/pojo/convertor/PermissionApplicationConverter.java",
            "application:user/pojo/convertor/RoleApplicationConverter.java",
            "application:user/pojo/convertor/PermissionConverter.java",
            "adapter:user/pojo/convertor/RoleAdapterConverter.java");

    /** Persistence converters keep the generated default bean name {@code <interface>Impl}. */
    private static final List<String> DEFAULT_BEAN_NAME_CONVERTERS = List.of(
            "teaching/converter/GradePOConverter.java", "teaching/converter/SchoolClassPOConverter.java",
            "user/converter/PermissionPOConverter.java", "user/converter/RolePOConverter.java",
            "user/converter/UserPOConverter.java");

    @Test
    void domainOwnsEveryCapabilityAndStaysFrameworkFree() throws IOException {
        String all = joinMain();
        for (String forbidden : List.of("CachePort", "QueryGateway", "Gateway", "EventPublisher",
                "Assembler", "IdempotencyAdapter")) {
            assertFalse(all.contains(forbidden), "retired port shape must be gone: " + forbidden);
        }

        for (String obsolete : List.of("client", "gateway", "exceptions", "teaching/client",
                "user/client", "teaching/cache", "user/cache")) {
            assertFalse(Files.exists(sources("domain").resolve(obsolete)),
                    "obsolete domain package survived: " + obsolete);
        }

        assertExists("domain", "service/CommandIdempotencyService.java");
        assertExists("domain", "service/OrganizationEventService.java");
        assertExists("domain", "teaching/service/EvaluationQueryService.java");
        assertExists("domain", "teaching/service/GradeDomainService.java");
        assertExists("domain", "teaching/service/SchoolClassDomainService.java");
        assertExists("domain", "user/service/UserDomainService.java");
        assertExists("domain", "user/service/PermissionDomainService.java");
        // The peer-contract records answer as domain value objects, not as a domain client.
        assertExists("domain", "teaching/vos/EvaluationCourseBO.java");
        assertExists("domain", "teaching/vos/EvaluationExamBO.java");
        assertExists("domain", "teaching/vos/EvaluationScoreBO.java");

        for (Path path : sourcesOf("domain")) {
            String text = read(path);
            assertFalse(text.contains(ROOT + ".infrastructure."),
                    "domain must not depend on infrastructure: " + path);
            assertFalse(text.contains(ROOT + ".application."),
                    "domain must not depend on application: " + path);
            assertFalse(text.contains(ROOT + ".adapter."),
                    "domain must not depend on adapter: " + path);
            assertFalse(text.contains("import org.springframework."),
                    "domain must stay framework-free: " + path);
            assertFalse(text.contains("Client"), "domain exposes a Service, never a Client: " + path);
        }
    }

    @Test
    void externalContractStaysBehindTheNamedClientInInfrastructure() throws IOException {
        assertExists("infrastructure", "client/evaluation/EvaluationQueryClient.java");
        assertExists("infrastructure", "client/evaluation/impl/LocalEvaluationQueryClientImpl.java");
        assertExists("infrastructure", "client/evaluation/impl/GrpcEvaluationQueryClientImpl.java");
        assertExists("infrastructure", "teaching/service/impl/EvaluationQueryServiceImpl.java");

        assertFalse(Files.exists(sources("infrastructure")
                .resolve("client/evaluation/LocalEvaluationQueryClientImpl.java")));
        assertFalse(Files.exists(sources("infrastructure")
                .resolve("client/evaluation/GrpcEvaluationQueryClientImpl.java")));
        String all = joinMain();
        assertFalse(matches(all, "(GrpcEvaluationQueryClient|LocalEvaluationQueryClient)(?!Impl)"),
                "the client implementation must use the ClientImpl suffix");
        assertFalse(all.contains("LocalEvaluationQueryStub"), "the stub folds into the local ClientImpl");

        String service = read("infrastructure", "teaching/service/impl/EvaluationQueryServiceImpl.java");
        assertTrue(service.contains("implements EvaluationQueryService"));
        assertTrue(service.contains("@Qualifier(\"evaluationQueryClient\")"),
                "the ServiceImpl must reach the dependency through the named Client");
        assertTrue(service.contains("client.getCourse("));
        assertTrue(service.contains("client.getExam("));
        assertTrue(service.contains("client.getScore("),
                "the compound key must stay (examId, scoreId), never a forced single id");
        assertFalse(service.contains("ManagedChannel"), "transport must stay behind the Client");

        // One bean name per capability, so exactly one profile-implant is active at a time.
        String local = read("infrastructure", "client/evaluation/impl/LocalEvaluationQueryClientImpl.java");
        String grpc = read("infrastructure", "client/evaluation/impl/GrpcEvaluationQueryClientImpl.java");
        assertTrue(local.contains("@Component(\"evaluationQueryClient\")"));
        assertTrue(grpc.contains("@Component(\"evaluationQueryClient\")"));
        assertTrue(local.contains("@Profile(\"test\")"));
        assertTrue(grpc.contains("@Profile({\"dev\", \"prod\"})"));
        // The peer package is a generation property, so only the contract path below it is stable.
        assertTrue(grpc.contains(".facade.evaluation.v1."),
                "the grpc client is the only place the peer contract may appear");
        assertTrue(grpc.contains("ManagedChannel"), "the transport stays inside the grpc ClientImpl");

        // The published peer contract may only be read inside the infrastructure client package.
        for (Path path : mainSources()) {
            if (path.toString().contains("/infrastructure/client/evaluation/")) {
                continue;
            }
            assertFalse(read(path).contains(".facade.evaluation.v1."),
                    "the peer Evaluation contract escaped the infrastructure client: " + path);
        }
    }

    @Test
    void boundaryCarriersLiveInThePojoDirectories() throws IOException {
        for (String path : List.of(
                "application:teaching/pojo/command/CreateGradeCommand.java",
                "application:teaching/pojo/command/CreateSchoolClassCommand.java",
                "application:teaching/pojo/command/AssignUserToClassCommand.java",
                "application:teaching/pojo/query/GradeDetailQuery.java",
                "application:teaching/pojo/query/SchoolClassDetailQuery.java",
                "application:teaching/pojo/result/GradeDetailResult.java",
                "application:teaching/pojo/result/SchoolClassDetailResult.java",
                "application:user/pojo/command/CreateUserCommand.java",
                "application:user/pojo/command/AssignRoleCommand.java",
                "application:user/pojo/command/GrantPermissionCommand.java",
                "application:user/pojo/query/UserDetailQuery.java",
                "application:user/pojo/query/PermissionTreeQuery.java",
                "application:user/pojo/result/UserDetailResult.java",
                "application:user/pojo/result/PermissionTreeResult.java",
                "adapter:teaching/pojo/dto/CreateGradeRequest.java",
                "adapter:teaching/pojo/dto/CreateSchoolClassRequest.java",
                "adapter:teaching/pojo/dto/AssignUserToClassRequest.java",
                "adapter:teaching/pojo/dto/CreateSchoolClassMessage.java",
                "adapter:teaching/pojo/vo/GradeDetailVO.java",
                "adapter:teaching/pojo/vo/SchoolClassDetailVO.java",
                "adapter:user/pojo/dto/CreateUserRequest.java",
                "adapter:user/pojo/dto/AssignRoleRequest.java",
                "adapter:user/pojo/dto/GrantPermissionRequest.java",
                "adapter:user/pojo/dto/CreateUserMessage.java",
                "adapter:user/pojo/vo/UserDetailVO.java",
                "adapter:user/pojo/vo/PermissionTreeVO.java")) {
            int separator = path.indexOf(':');
            assertExists(path.substring(0, separator), path.substring(separator + 1));
        }

        for (String obsolete : List.of(
                "application:teaching/command", "application:teaching/query", "application:teaching/result",
                "application:teaching/converter", "application:teaching/assemblers",
                "application:user/command", "application:user/query", "application:user/result",
                "application:user/converter", "application:user/assemblers", "application:exceptions",
                "adapter:teaching/dto", "adapter:teaching/vo", "adapter:teaching/converter",
                "adapter:user/dto", "adapter:user/vo", "adapter:user/converter")) {
            int separator = obsolete.indexOf(':');
            assertFalse(Files.exists(sources(obsolete.substring(0, separator))
                    .resolve(obsolete.substring(separator + 1))), "obsolete carrier package: " + obsolete);
        }

        String all = joinMain();
        for (String forbidden : List.of(
                ROOT + ".application.teaching.command", ROOT + ".application.teaching.query",
                ROOT + ".application.teaching.result", ROOT + ".application.teaching.converter",
                ROOT + ".application.teaching.assemblers", ROOT + ".application.user.command",
                ROOT + ".application.user.query", ROOT + ".application.user.result",
                ROOT + ".application.user.converter", ROOT + ".application.user.assemblers",
                ROOT + ".adapter.teaching.dto", ROOT + ".adapter.teaching.vo",
                ROOT + ".adapter.user.dto", ROOT + ".adapter.user.vo")) {
            assertFalse(all.contains(forbidden), "old carrier package still referenced: " + forbidden);
        }

        List<Path> carriers = mainSources().stream()
                .filter(path -> path.toString().contains("/pojo/"))
                .filter(path -> !path.getFileName().toString().endsWith("Converter.java"))
                .filter(path -> !path.getFileName().toString().endsWith("Convertor.java"))
                .filter(path -> !path.getFileName().toString().equals("package-info.java"))
                .toList();
        assertEquals(26, carriers.size(), "every reviewed inbound or outbound carrier");
        for (Path path : carriers) {
            assertTrue(read(path).contains("implements BasePojo"),
                    "hand-written carrier must implement the common carrier contract: " + path);
        }
    }

    @Test
    void persistenceRolesStaySiblingsOfTheRepository() throws IOException {
        for (String path : List.of(
                "teaching/dao/GradeDAO.java", "teaching/dao/SchoolClassDAO.java", "teaching/dao/SchoolClassUserDAO.java",
                "teaching/po/GradePO.java", "teaching/po/SchoolClassPO.java", "teaching/po/SchoolClassUserPO.java",
                "teaching/converter/GradePOConverter.java", "teaching/converter/SchoolClassPOConverter.java",
                "user/dao/UserDAO.java", "user/dao/RoleDAO.java", "user/dao/PermissionDAO.java",
                "user/dao/RolePermissionDAO.java", "user/dao/UserRoleDAO.java",
                "user/po/UserPO.java", "user/po/RolePO.java", "user/po/PermissionPO.java",
                "user/po/RolePermissionPO.java", "user/po/UserRolePO.java",
                "user/converter/UserPOConverter.java", "user/converter/RolePOConverter.java",
                "user/converter/PermissionPOConverter.java")) {
            assertExists("infrastructure", path);
        }
        for (String business : List.of("teaching", "user")) {
            assertFalse(Files.exists(sources("infrastructure").resolve(business + "/repo/dao")));
            assertFalse(Files.exists(sources("infrastructure").resolve(business + "/repo/po")));
            assertFalse(Files.exists(sources("infrastructure").resolve(business + "/repo/converter")));
            for (Path path : sources(sources("infrastructure").resolve(business + "/repo"))) {
                String name = path.getFileName().toString();
                assertTrue(name.endsWith("Repository.java") || name.equals("package-info.java"),
                        "repo holds only the technical Repository: " + path);
            }
        }

        String all = joinMain();
        for (String forbidden : List.of(
                ROOT + ".infrastructure.teaching.repo.dao", ROOT + ".infrastructure.user.repo.dao",
                ROOT + ".infrastructure.teaching.repo.po", ROOT + ".infrastructure.user.repo.po",
                ROOT + ".infrastructure.teaching.repo.converter", ROOT + ".infrastructure.user.repo.converter")) {
            assertFalse(all.contains(forbidden), "moved persistence package still referenced: " + forbidden);
        }

        for (String xml : List.of("mybatis/mapper/teaching/GradeDAO.xml",
                "mybatis/mapper/teaching/SchoolClassDAO.xml", "mybatis/mapper/teaching/SchoolClassUserDAO.xml")) {
            String text = resource("infrastructure", xml);
            assertTrue(text.contains(ROOT + ".infrastructure.teaching.dao."), xml);
            assertTrue(text.contains(ROOT + ".infrastructure.teaching.po."), xml);
            assertFalse(text.contains("repo.dao"), xml);
        }
        for (String xml : List.of("mybatis/mapper/user/UserDAO.xml", "mybatis/mapper/user/RoleDAO.xml",
                "mybatis/mapper/user/PermissionDAO.xml", "mybatis/mapper/user/RolePermissionDAO.xml",
                "mybatis/mapper/user/UserRoleDAO.xml")) {
            String text = resource("infrastructure", xml);
            assertTrue(text.contains(ROOT + ".infrastructure.user.dao."), xml);
            assertTrue(text.contains(ROOT + ".infrastructure.user.po."), xml);
            assertFalse(text.contains("repo.dao"), xml);
        }

        String scan = read("starter", "OrganizationApplication.java");
        assertTrue(scan.contains(ROOT + ".infrastructure.teaching.dao"));
        assertTrue(scan.contains(ROOT + ".infrastructure.user.dao"));
        assertFalse(scan.contains("repo.dao"));

        // The ServiceImpl composes the sibling roles instead of inheriting the persistence stack.
        String grade = read("infrastructure", "teaching/service/impl/GradeDomainServiceImpl.java");
        assertTrue(grade.contains("@Qualifier(\"gradeRepository\")"));
        assertTrue(grade.contains("@Qualifier(\"gradePOConverterImpl\")"));
        assertFalse(grade.contains("extends EgonColaServiceImpl"),
                "the domain ServiceImpl must compose repositories, never inherit the mapper stack");
    }

    @Test
    void sqlPredicatesTenantAndVersionSemanticsStayUnchanged() {
        for (String xml : List.of(
                "mybatis/mapper/teaching/GradeDAO.xml", "mybatis/mapper/teaching/SchoolClassDAO.xml",
                "mybatis/mapper/teaching/SchoolClassUserDAO.xml", "mybatis/mapper/user/UserDAO.xml",
                "mybatis/mapper/user/RoleDAO.xml", "mybatis/mapper/user/PermissionDAO.xml",
                "mybatis/mapper/user/RolePermissionDAO.xml", "mybatis/mapper/user/UserRoleDAO.xml")) {
            String text = resource("infrastructure", xml);
            assertTrue(text.contains("deleted_at IS NULL"), xml);
            assertTrue(text.contains("MP_OPTLOCK_VERSION_ORIGINAL"), xml);
            assertTrue(text.contains("tenant_id"), xml);
            assertTrue(text.contains("selectActiveById"), xml);
            assertTrue(text.contains("deleteVersionedById"), xml);
            assertFalse(text.contains("${"), "no string interpolation may enter the mapper SQL: " + xml);
        }
    }

    @Test
    void cachedReadsAndWritesStayOnTheSpringProxyOfTheSameRegionKey() throws IOException {
        for (String path : List.of("teaching/repo/GradeRepository.java", "user/repo/UserRepository.java")) {
            String text = read("infrastructure", path);
            String entity = path.contains("Grade") ? "GradePO" : "UserPO";
            assertTrue(text.contains("@CacheConfig(cacheNames = \"" + entity + "\")"),
                    "one region per cached entity: " + path);
            assertTrue(text.contains("public " + entity + " findCachedById("),
                    "the cached read must be a non-final proxy entry point: " + path);
            assertTrue(text.contains("public boolean updateCachedById("),
                    "the cached write must be a non-final proxy entry point: " + path);
            assertTrue(text.contains("T(org.slf4j.MDC).get('tenantId')"),
                    "the region key must carry the tenant: " + path);
            assertTrue(text.contains("sync = true"), "the cached read must not stampede: " + path);
            assertTrue(text.contains("condition = \"#result\""), "a false write must not evict: " + path);
        }

        String user = read("infrastructure", "user/service/impl/UserDomainServiceImpl.java");
        assertTrue(user.contains("userRepository.findCachedById("),
                "single id read must use the cached entry point");
        assertTrue(user.contains("userRepository.updateCachedById("),
                "cached entity write must invalidate");
        assertFalse(user.contains("userRepository.updateById("),
                "the cached entity must not be written beside its cache region");
        String grade = read("infrastructure", "teaching/service/impl/GradeDomainServiceImpl.java");
        assertTrue(grade.contains("gradeRepository.findCachedById("));
        assertTrue(grade.contains("gradeRepository.updateCachedById("));
        assertFalse(grade.contains("gradeRepository.updateById("));

        for (Path path : sourcesOf("infrastructure")) {
            String text = read(path);
            assertFalse(text.contains("this.findCachedById("), "cache entry must not be self-invoked: " + path);
            assertFalse(text.contains("this.updateCachedById("), "cache entry must not be self-invoked: " + path);
        }

        // When the component is switched off the host owns the manager, so no JSR-107 provider can hijack the region.
        assertTrue(read("infrastructure", "config/OrganizationRedisConfig.java")
                        .contains("ConcurrentMapCacheManager"),
                "a disabled cache component still needs a deterministic CacheManager for the cached entry points");

        // Only the two selected single-id reads are cached; the compound ones keep their explicit uncached SQL.
        for (String path : List.of("teaching/repo/SchoolClassRepository.java",
                "teaching/repo/SchoolClassUserRepository.java", "user/repo/RoleRepository.java",
                "user/repo/PermissionRepository.java", "user/repo/RolePermissionRepository.java",
                "user/repo/UserRoleRepository.java")) {
            assertFalse(read("infrastructure", path).contains("@CacheConfig"),
                    "unselected read must stay uncached: " + path);
        }
        assertTrue(read("infrastructure", "teaching/repo/SchoolClassRepository.java").contains("gradeId"),
                "the parent-child lookup must keep owning gradeId instead of a single-id key");
        assertFalse(Files.exists(sources("infrastructure").resolve("teaching/cache")));
        assertFalse(Files.exists(sources("infrastructure").resolve("user/cache")));
    }

    @Test
    void claimAndReleaseIdempotencyIsADomainServiceWithTheOriginalKeyAndTtl() throws IOException {
        String port = read("domain", "service/CommandIdempotencyService.java");
        assertTrue(port.contains("boolean claim("), "the claim decision belongs to the domain");
        assertTrue(port.contains("void release("));
        assertFalse(port.contains("import org.springframework."), "the capability port stays framework-free");

        String redis = read("infrastructure", "service/impl/CommandIdempotencyServiceImpl.java");
        assertTrue(redis.contains("implements CommandIdempotencyService"));
        assertTrue(redis.contains("@Service(\"commandIdempotencyService\")"));
        assertTrue(redis.contains("setIfAbsent("), "the claim stays an atomic set-if-absent");
        assertTrue(redis.contains("OrganizationCacheKey.command(operation, requestId)"),
                "the claim key keeps its original namespaced shape");
        assertTrue(redis.contains("\"1\""), "the recorded claim value stays unchanged");
        assertTrue(redis.contains("properties.getCommandIdempotencyTtl()"),
                "the TTL stays configurable instead of being re-hardcoded");
        assertTrue(redis.contains("redisTemplate.delete("), "release deletes the claim again");
        assertTrue(redis.contains("prefix = \"organization.integrations.redis\", name = \"enabled\", havingValue = \"true\""));

        // The key that a claim already took before this class existed must still be honoured.
        String key = read("infrastructure", "cache/OrganizationCacheKey.java");
        assertTrue(key.contains("student-management-organization:"));
        assertTrue(key.contains("\"command:\" + operation + \":\" + requestId"));
        assertTrue(read("infrastructure", "config/OrganizationIntegrationProperties.java")
                .contains("Duration.ofHours(24)"), "the default claim TTL stays 24h");

        String fallback = read("infrastructure", "config/OrganizationLocalFallbackConfig.java");
        assertTrue(fallback.contains("@Bean(\"commandIdempotencyService\")"),
                "a Redis-free profile boots on the same Domain Service");
        String inMemory = read("infrastructure", "service/impl/InMemoryCommandIdempotencyServiceImpl.java");
        assertTrue(inMemory.contains("implements CommandIdempotencyService"));
        assertTrue(inMemory.contains("OrganizationCacheKey.command("),
                "the fallback must reuse the same key shape as Redis");

        assertFalse(Files.exists(sources("infrastructure").resolve("cache/CommandIdempotencyServiceImpl.java")));
        assertFalse(Files.exists(sources("infrastructure").resolve("cache/InMemoryCommandIdempotencyServiceImpl.java")));
        assertFalse(Files.exists(sources("infrastructure").resolve("service/impl/RedisCommandIdempotencyServiceImpl.java")));

        // A claim is taken before any side effect and released again when the action fails.
        String envelope = read("application", "support/IdempotentCommand.java");
        assertTrue(envelope.contains("if (!port.claim(operation, requestId))"));
        assertTrue(envelope.contains("throw new OrganizationApplicationException("));
        assertTrue(envelope.contains("OrganizationFailureType.CONFLICT"), "a duplicate stays a conflict");
        assertTrue(envelope.contains("port.release(operation, requestId)"));
    }

    @Test
    void validatorsExtendBaseValidatorAndNeverWriteSideEffects() throws IOException {
        List<Path> validators = mainSources().stream()
                .filter(path -> path.getFileName().toString().endsWith("Validator.java"))
                .toList();
        assertEquals(7, validators.size(), "one application, domain and code validator set per boundary");
        for (Path path : validators) {
            String text = read(path);
            assertTrue(text.contains("extends BaseValidator"), "validator contract: " + path);
            assertTrue(text.contains("getValidationUtils"), "validator must expose the common facade: " + path);
            for (String forbidden : List.of("RedisTemplate", "RabbitTemplate", "Idempotency", "claim(",
                    "CachePort", "Repository", "DAO", "Mapper", "updateById(", "save(", "insert(")) {
                assertFalse(text.contains(forbidden),
                        "validation must stay side-effect free: " + forbidden + " in " + path);
            }
        }
        // The claim moved out of validation into the command envelope that the Manage calls.
        String teaching = read("application", "teaching/validators/TeachingApplicationValidator.java");
        assertFalse(teaching.contains("Idempotency"));
    }

    @Test
    void manageDependsOnlyOnDomainServices() throws IOException {
        List<Path> manages = mainSources().stream()
                .filter(path -> path.getFileName().toString().endsWith("ManageImpl.java"))
                .toList();
        assertEquals(5, manages.size(), "grade, school class, user, role and permission use-case boundary");
        for (Path path : manages) {
            String text = read(path);
            for (String forbidden : List.of("RabbitTemplate", "RedisTemplate", "RedisConnectionFactory",
                    "EventPublisher", "CachePort", "QueryGateway", "MqMessageService", "MqRouteEnum",
                    "OrganizationEventProducer", "Repository", "DAO", "Mapper", "Cacheable", "CacheEvict")) {
                assertFalse(text.contains(forbidden),
                        "Manage must not reach infrastructure: " + forbidden + " in " + path);
            }
            assertTrue(text.contains("@Slf4j"), "business classes log through @Slf4j: " + path);
            assertTrue(text.contains("@Qualifier(\"commandIdempotencyService\")"),
                    "the claim goes through the idempotency Domain Service: " + path);
            assertTrue(text.contains("@Qualifier(\"organizationEventService\")"),
                    "publication goes through the event Domain Service: " + path);
            assertTrue(text.contains("IdempotentCommand.execute("), "the envelope owns claim and release: " + path);
            assertTrue(text.contains("OrganizationTransactionHooks.afterCommit("),
                    "publication stays deferred to the commit point: " + path);
        }
        assertTrue(matches(read("application", "user/manage/impl/UserManageImpl.java"),
                "eventService\\.publish\\(\\s*new UserChangedEvent\\("));
        assertTrue(matches(read("application", "teaching/manage/impl/SchoolClassManageImpl.java"),
                "eventService\\.publish\\(\\s*new SchoolClassMembershipChangedEvent\\("));
        assertTrue(matches(read("application", "user/manage/impl/RoleManageImpl.java"),
                "eventService\\.publish\\(\\s*new RoleAssignedEvent\\("));
    }

    @Test
    void mqTopologyAndRoutingComeFromOneEnumInsteadOfTypePrefixGuessing() throws IOException {
        assertExists("infrastructure", "mq/MqRouteEnum.java");
        assertExists("infrastructure", "mq/MqMessageService.java");
        assertExists("infrastructure", "mq/impl/RabbitMqMessageServiceImpl.java");
        assertExists("infrastructure", "service/impl/OrganizationEventServiceImpl.java");
        for (String obsolete : List.of("mq/RabbitOrganizationEventPublisher.java",
                "mq/LocalOrganizationEventPublisher.java")) {
            assertFalse(Files.exists(sources("infrastructure").resolve(obsolete)),
                    "per-event publishers fold into one MQ boundary: " + obsolete);
        }

        String route = read("infrastructure", "mq/MqRouteEnum.java");
        assertTrue(route.contains("enum MqRouteEnum implements EgonEnum"));
        for (String field : List.of("exchange", "queue", "routingKey", "deadLetterRoutingKey", "durable",
                "payloadType", "schemaVersion", "eventType", "eventClass", "payloadExtractor")) {
            assertTrue(route.contains(field), "route contract field missing: " + field);
        }
        assertCodes("infrastructure", "mq/MqRouteEnum.java", "USER_CHANGED", "ROLE_ASSIGNED",
                "PERMISSION_GRANTED", "GRADE_CHANGED", "SCHOOL_CLASS_CHANGED",
                "SCHOOL_CLASS_MEMBERSHIP_CHANGED", "USER_CREATE_COMMAND", "SCHOOL_CLASS_CREATE_COMMAND");
        // The existing wire targets stay verbatim so a generated project keeps its broker topology.
        for (String wire : List.of("student.organization.command.v1", "student.organization.event.v1",
                "student.organization.dlx.v1", "student.organization.user.create.v1",
                "student.organization.school-class.create.v1", ".dlq",
                "organization.command.user.create.v1", "organization.event.user.changed.v1",
                "organization.dead.user.create.v1")) {
            assertTrue(route.contains(wire), "route must keep the existing wire name: " + wire);
        }
        assertTrue(route.contains("requireEventRoute"), "the producer resolves the route from the event");
        assertTrue(route.contains("EVENT_ROUTES.get(Objects.requireNonNull(event, \"event\").getClass())"),
                "the lookup must match the event class, never its name");
        assertTrue(route.contains("payloadOf("), "the payload projection is declared once per route");

        String publish = read("infrastructure", "mq/impl/RabbitMqMessageServiceImpl.java");
        assertTrue(publish.contains("producer.send(route.getExchange(), route.getRoutingKey(), message)"),
                "publication must reuse the declared route");
        assertTrue(publish.contains("route.accepts(payload)"), "payload type must be verified before publish");
        assertTrue(publish.contains("getSchemaVersion()"));
        assertTrue(publish.contains("isActualTransactionActive("),
                "publication must stay deferred to the commit point");
        assertTrue(publish.contains("prefix = \"organization.integrations.rabbit\", name = \"enabled\", havingValue = \"true\""),
                "the broker bean stays opt-in");

        String event = read("infrastructure", "service/impl/OrganizationEventServiceImpl.java");
        assertTrue(event.contains("@Service(\"organizationEventService\")"));
        assertTrue(event.contains("@Qualifier(\"mqMessageService\")"),
                "one MQ boundary serves every event type");

        String local = read("infrastructure", "config/OrganizationLocalFallbackConfig.java");
        assertTrue(local.contains("@Bean(\"mqMessageService\")"),
                "a broker-free profile still boots on the same MQ boundary");

        String configuration = read("infrastructure", "config/OrganizationRabbitConfig.java");
        assertTrue(configuration.contains("MqRouteEnum"), "topology must reuse the same route constants");
        assertTrue(configuration.contains("getDeadLetterRoutingKey()"),
                "the dead-letter binding comes from the route, not a second literal");

        String userConsumer = read("adapter", "user/mq/UserCreatedConsumer.java");
        String classConsumer = read("adapter", "teaching/mq/SchoolClassChangedConsumer.java");
        assertTrue(userConsumer.contains("queues = \"student.organization.user.create.v1\""),
                "the consumer still binds the declared queue");
        assertTrue(classConsumer.contains("queues = \"student.organization.school-class.create.v1\""));

        String all = joinMain();
        assertFalse(matches(all, "\\.startsWith\\(\"(user|teaching|grade|role|permission|school)\\."),
                "routing must not guess from the event type");
    }

    @Test
    void wireCarriersAndEnumsImplementTheCommonContracts() throws IOException {
        String message = read("infrastructure", "mq/OrganizationEventMessage.java");
        assertTrue(message.contains("Instant occurredAt"), "time values stay on java.time");
        assertTrue(message.contains("Map.copyOf(payload)"), "the wire carrier stays immutable");
        assertTrue(read("domain", "events/OrganizationDomainEvent.java").contains("Instant occurredAt()"));
        String all = joinMain();
        assertFalse(all.contains("java.util.Date"), "legacy date type must not appear");
        assertFalse(all.contains(".ordinal()"), "ordinal must never act as the code");

        for (Path path : mainSources()) {
            String text = read(path);
            if (text.contains("\npublic enum ") || text.contains("\nenum ")) {
                assertTrue(matches(text, "implements [^{]*\\b(EgonEnum|ErrorStatus)\\b"),
                        "handwritten enum must implement EgonEnum or ErrorStatus: " + path);
                assertTrue(matches(text, "public int getCode\\(\\)"),
                        "the enum code must be an explicit accessor: " + path);
            }
        }
        assertCodes("common", "enums/OrganizationFailureType.java",
                "VALIDATION", "FORBIDDEN", "NOT_FOUND", "CONFLICT", "DOMAIN_REJECTED",
                "DEPENDENCY_UNAVAILABLE", "INTERNAL");
        assertCodes("common", "enums/ExternalDependencyFailure.java",
                "NOT_FOUND", "BUSINESS_REJECTED", "VALIDATION_FAILED", "UNAVAILABLE", "TIMEOUT",
                "CONTRACT_INCOMPATIBLE", "SERVICE_FAILURE");
        assertCodes("common", "enums/OrganizationDomainErrorCode.java",
                "INVALID_USER_ID", "INVALID_USER_NAME", "INVALID_EMAIL", "INVALID_CODE",
                "DUPLICATE_ROLE_ASSIGNMENT", "DUPLICATE_PERMISSION_GRANT", "USER_DISABLED", "ROLE_ARCHIVED",
                "PERMISSION_INACTIVE", "DOMAIN_REJECTED", "CONFLICT", "DEPENDENCY_UNAVAILABLE");
        assertCodes("domain", "teaching/enums/GradeStatus.java", "ACTIVE", "ARCHIVED");
        assertCodes("domain", "teaching/enums/SchoolClassStatus.java", "ACTIVE", "ARCHIVED");
        assertCodes("domain", "user/enums/UserStatus.java", "ACTIVE", "DISABLED");
        assertCodes("domain", "user/enums/RoleStatus.java", "ACTIVE", "ARCHIVED");
        assertCodes("domain", "user/enums/PermissionStatus.java", "ACTIVE", "INACTIVE");
        assertCodes("domain", "user/enums/PermissionType.java", "API", "MENU", "ACTION");
        // The wire code stays the stable String of ErrorStatus, never the int code.
        assertTrue(read("common", "enums/OrganizationFailureType.java").contains("public String getStatus()"));
    }

    @Test
    void customExceptionsLiveInCommonOnTheCommonHierarchy() throws IOException {
        for (String path : List.of("exception/BizException.java", "exception/NotFoundException.java",
                "exception/OrganizationApplicationException.java", "exception/OrganizationDomainException.java",
                "exception/OrganizationPortException.java", "exception/ExternalDependencyException.java",
                "exception/ConfigDecryptException.java", "exception/RetryableOrganizationMessageException.java")) {
            assertExists("common", path);
        }
        assertFalse(Files.exists(sources("common").resolve("exceptions")));
        for (String obsolete : List.of("domain:exceptions", "application:exceptions",
                "adapter:exceptions", "infrastructure:exceptions")) {
            int separator = obsolete.indexOf(':');
            assertFalse(Files.exists(sources(obsolete.substring(0, separator))
                    .resolve(obsolete.substring(separator + 1))), "obsolete exception package: " + obsolete);
        }
        assertFalse(Files.exists(sources("starter").resolve("config/encryption/ConfigDecryptException.java")));

        for (Path path : mainSources()) {
            String name = path.getFileName().toString();
            if (name.endsWith("Exception.java")) {
                assertTrue(path.toString().contains("/common/exception/"),
                        "a custom exception may only live in common: " + path);
            }
            assertFalse(read(path).contains("extends RuntimeException"),
                    "custom exceptions must re-root on the common hierarchy: " + path);
        }

        assertTrue(read("common", "exception/BizException.java").contains("extends BusinessException"));
        assertTrue(read("common", "exception/NotFoundException.java").contains("extends BizException"));
        assertTrue(read("common", "exception/OrganizationApplicationException.java")
                .contains("extends BusinessException"));
        assertTrue(read("common", "exception/OrganizationDomainException.java").contains("extends BusinessException"));
        assertTrue(read("common", "exception/ExternalDependencyException.java").contains("extends BusinessException"));
        assertTrue(read("common", "exception/ConfigDecryptException.java").contains("extends CommonException"));
        assertTrue(read("common", "exception/OrganizationPortException.java").contains("extends CommonException"));
        assertTrue(read("common", "exception/RetryableOrganizationMessageException.java")
                .contains("extends CommonException"));
        String application = read("common", "exception/OrganizationApplicationException.java");
        assertTrue(application.contains("public String code()"), "the stable wire code stays a String");
        assertTrue(application.contains("public OrganizationFailureType failureType()"));
        assertTrue(read("common", "exception/OrganizationDomainException.java")
                .contains("public OrganizationDomainErrorCode code()"), "the typed accessor keeps its name");
        assertTrue(read("common", "exception/OrganizationPortException.java")
                .contains("public OrganizationDomainErrorCode code()"));
        String external = read("common", "exception/ExternalDependencyException.java");
        assertTrue(external.contains("public String dependency()"));
        assertTrue(external.contains("public ExternalDependencyFailure failure()"));
        assertTrue(external.contains("public String externalCode()"));

        String handler = read("adapter", "handler/OrganizationGlobalExceptionHandler.java");
        assertTrue(handler.contains("failure.code()"), "the wire code is published through the String code");
        assertFalse(handler.contains("getCode()"), "the int code must not become the wire code");
        // A rolled back publication must never be retried by the broker for a permanent rejection.
        String support = read("adapter", "mq/OrganizationMessageSupport.java");
        assertTrue(support.contains("AmqpRejectAndDontRequeueException"));
        assertTrue(support.contains("RetryableOrganizationMessageException"));
    }

    @Test
    void convertersUseTheCommonMappingContract() throws IOException {
        List<Path> converters = mainSources().stream()
                .filter(path -> path.getFileName().toString().endsWith("Converter.java")
                        || path.getFileName().toString().endsWith("Convertor.java"))
                .toList();
        assertEquals(18, converters.size(), "every reviewed semantic mapping pair");
        List<String> reviewed = new ArrayList<>();
        for (Path path : converters) {
            String text = read(path);
            String module = moduleOf(path);
            String relative = sources(module).relativize(path).toString();
            String key = module + ":" + relative;
            reviewed.add(key);
            assertTrue(text.contains("@Mapper"), "mapping must be generated, never handwritten: " + path);
            assertTrue(text.contains("unmappedTargetPolicy") || text.contains("nullValueCheckStrategy"),
                    "the mapping policy stays declared: " + path);
            if (MULTI_SOURCE_CONVERTERS.contains(key)) {
                // Several sources build one target, so the single-source base interface cannot express it.
                assertTrue(text.contains("componentModel = \"spring\""), key);
                assertTrue(text.contains("@AnnotateWith"), "generated bean name must be explicit: " + path);
                continue;
            }
            assertTrue(text.contains("BaseConverter<") || text.contains("BaseForwardConverter<"),
                    "semantic mapping must use the common converter contract: " + path);
            if (!text.contains("componentModel = \"spring\"")) {
                assertTrue(joinMain().contains("@Bean(\"" + beanName(path) + "\")"),
                        "a mapper without componentModel must be published as a named bean: " + path);
            } else if (!DEFAULT_BEAN_NAME_CONVERTERS.contains(relative)) {
                assertTrue(text.contains("@AnnotateWith"),
                        "the generated bean name must be explicit: " + path);
            }
        }
        assertTrue(reviewed.containsAll(MULTI_SOURCE_CONVERTERS),
                "stale multi-source converter entry: " + MULTI_SOURCE_CONVERTERS);

        String grade = read("application", "teaching/pojo/convertor/GradeConverter.java");
        assertTrue(grade.contains("BaseForwardConverter<"), "the result projection is one-way");
        assertFalse(matches(joinMain(), "new (UserDetailResult|GradeDetailResult|SchoolClassDetailResult"
                + "|PermissionTreeResult)\\("), "no manual bulk copying of a result carrier");
    }

    @Test
    void collaboratorsAreInjectedThroughNamedBeans() throws IOException {
        List<String> converterNames = mainSources().stream()
                .filter(path -> path.getFileName().toString().endsWith("Converter.java")
                        || path.getFileName().toString().endsWith("Convertor.java"))
                .map(path -> path.getFileName().toString().replace(".java", ""))
                .toList();
        for (Path path : mainSources()) {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (int index = 0; index < lines.size(); index++) {
                String line = lines.get(index);
                var matcher = java.util.regex.Pattern.compile("private final (\\w+) \\w+;").matcher(line);
                if (!matcher.find()) {
                    continue;
                }
                String type = matcher.group(1);
                boolean injected = type.endsWith("Service") || type.endsWith("ServiceImpl")
                        || type.endsWith("Repository") || type.endsWith("DAO") || type.endsWith("Manage")
                        || type.endsWith("Validator") || type.endsWith("Converter") || type.endsWith("Convertor")
                        || type.endsWith("Client") || type.endsWith("Producer") || type.endsWith("Properties")
                        || type.endsWith("Facade") || converterNames.contains(type);
                if (!injected) {
                    continue;
                }
                String window = String.join("\n", lines.subList(Math.max(0, index - 3), index));
                assertTrue(window.contains("@Qualifier(\""),
                        "injected collaborators must name their bean: " + path + " -> " + line.trim());
            }
        }
        assertFalse(joinMain().contains("LoggerFactory"), "business classes log through @Slf4j");
    }

    @Test
    void dubboFacadeChainPublishesEveryContractOnce() throws IOException {
        // Every generated Triple contract is implemented by exactly one named provider bean.
        for (String path : List.of(
                "teaching/facade/impl/GradeFacadeImpl.java", "teaching/facade/impl/SchoolClassFacadeImpl.java",
                "user/facade/impl/UserFacadeImpl.java", "user/facade/impl/RoleFacadeImpl.java",
                "user/facade/impl/PermissionFacadeImpl.java")) {
            String text = read("adapter", path);
            assertTrue(matches(text, "@Service\\(\"\\w+Facade\"\\)"),
                    "a Spring-managed provider needs an explicit bean name: " + path);
            assertTrue(text.contains("extends Dubbo"), "the provider implements the generated contract: " + path);
            assertTrue(text.contains("@Slf4j"), "business classes log through @Slf4j: " + path);
            assertTrue(text.contains("@Qualifier(\"organizationFacadeSupport\")"),
                    "the shared mapping boundary is injected by name: " + path);
        }
        assertEquals(5, count(read("adapter", "user/rpc/UserRpcProvider.java")
                + read("adapter", "teaching/rpc/SchoolClassRpcProvider.java"), "public ServiceBean<"),
                "each generated service is exported once over Triple");
        String adapter = join(sourcesOf("adapter"));
        for (String qualifier : List.of("@Qualifier(\"userManage\")", "@Qualifier(\"roleManage\")",
                "@Qualifier(\"permissionManage\")", "@Qualifier(\"gradeManage\")",
                "@Qualifier(\"schoolClassManage\")")) {
            assertTrue(adapter.contains(qualifier),
                    "the facade impl must reach the named collaborator: " + qualifier);
        }
        // The facade module publishes Protobuf contracts only, so it has no Java source tree.
        assertFalse(Files.exists(sources("facade")), "the generated contract stays out of the Java layers");
        assertTrue(adapter.contains("@RestControllerAdvice(name = \"organizationGlobalExceptionHandler\")"),
                "the HTTP boundary keeps its explicit bean name");
    }

    @Test
    void obsoletePackagesDoNotSurviveAndTestSurfaceCoversTheMovedContracts() throws IOException {
        for (String obsolete : List.of(
                "common:exceptions", "domain:client", "domain:exceptions", "domain:gateway",
                "domain:teaching/client", "domain:user/client",
                "application:exceptions", "application:teaching/assemblers", "application:user/assemblers",
                "adapter:teaching/dto", "adapter:teaching/vo", "adapter:teaching/converter",
                "adapter:user/dto", "adapter:user/vo", "adapter:user/converter",
                "infrastructure:teaching/cache", "infrastructure:user/cache",
                "infrastructure:teaching/repo/dao", "infrastructure:user/repo/dao",
                "infrastructure:teaching/repo/po", "infrastructure:user/repo/po",
                "infrastructure:mq/RabbitOrganizationEventPublisher.java",
                "infrastructure:mq/LocalOrganizationEventPublisher.java",
                "infrastructure:client/evaluation/GrpcEvaluationQueryClientImpl.java",
                "infrastructure:client/evaluation/LocalEvaluationQueryClientImpl.java",
                "adapter:mq/RetryableOrganizationMessageException.java",
                "starter:config/encryption/ConfigDecryptException.java")) {
            int separator = obsolete.indexOf(':');
            assertFalse(Files.exists(sources(obsolete.substring(0, separator))
                    .resolve(obsolete.substring(separator + 1))), "obsolete path survived: " + obsolete);
        }

        List<String> retired = List.of("EvaluationQueryPort", "UserCachePort", "GradeCachePort",
                "SchoolClassCachePort", "RedisUserCache", "InMemoryUserCache", "RedisGradeCache",
                "InMemoryGradeCache", "RedisSchoolClassCache", "InMemorySchoolClassCache",
                "RedisCommandIdempotencyAdapter", "InMemoryCommandIdempotencyAdapter",
                "RabbitOrganizationEventPublisher", "LocalOrganizationEventPublisher", "CachePort",
                "QueryGateway", "Assembler", "repo.dao", "repo.po", ROOT + ".domain.client",
                "application.exceptions", "common.exceptions", "CommandIdempotencyPort",
                "OrganizationEventPublisher", "LocalEvaluationQueryStub");
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
        for (String stale : retired) {
            assertFalse(testSurface.contains(stale),
                    "the test surface still names the retired contract: " + stale);
        }
        assertTrue(testSurface.contains("student-management-organization:command:"),
                "the claim key shape stays asserted by the family fixture");
        assertTrue(testSurface.contains("LocalMqMessageService"),
                "the broker-free profile records publications through the same MQ boundary");
        // A rolled back transaction must leave neither a publication nor a claim behind.
        String rollback = read(testFile("starter", "OrganizationRollbackTest.java"));
        assertTrue(rollback.contains("publishedMessages()).isEmpty()"),
                "the rollback fixture must prove nothing reached the broker");
        assertTrue(rollback.contains("idempotency.contains("),
                "the rollback fixture must prove the claim was released");
    }

    @Test
    void definitionResourcesFollowTheMovedTree() throws IOException {
        Path definition = Path.of("..", "..", "..", "definitions", "egon-cola-archetype-web-open");
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
        documented.addAll(List.of(
                Path.of("..", "README.md"), Path.of("..", "README.zh-CN.md"),
                Path.of("..", "deploy", "container", "README.md")));
        for (Path path : documented) {
            // The generated-project verifier names the retired symbols on purpose to forbid them,
            // so only the resources that describe the tree positively are scanned here.
            if (path.getFileName().toString().equals("verify.groovy")) {
                continue;
            }
            String text = read(path);
            for (String stale : List.of(
                    ROOT + ".domain.client", ROOT + ".domain.teaching.client", ROOT + ".domain.user.client",
                    ROOT + ".application.teaching.command", ROOT + ".application.teaching.assemblers",
                    ROOT + ".application.user.command", ROOT + ".application.exceptions",
                    ROOT + ".adapter.teaching.dto", ROOT + ".adapter.teaching.vo",
                    ROOT + ".infrastructure.teaching.cache", ROOT + ".infrastructure.user.cache",
                    ROOT + ".infrastructure.teaching.repo.dao", ROOT + ".infrastructure.user.repo.dao",
                    "infrastructure/teaching/repo/po", "infrastructure/user/repo/po",
                    "infrastructure/teaching/repo/dao", "infrastructure/user/repo/dao",
                    "infrastructure/teaching/repo/converter", "infrastructure/user/repo/converter",
                    "application/exceptions", "domain/exceptions", "common/exceptions",
                    "CommandIdempotencyPort", "EvaluationQueryPort", "OrganizationEventPublisher",
                    "domain/client", "domain/gateway", "domain/teaching/client", "domain/user/client",
                    "teaching/cache/RedisGradeCache", "user/cache/RedisUserCache",
                    "LocalEvaluationQueryStub", "RabbitOrganizationEventPublisher",
                    "CachePort", "EventPublisher", "IdempotencyAdapter")) {
                assertFalse(text.contains(stale),
                        "definition resource still publishes the retired contract: " + stale + " in " + path);
            }
        }

        // The verifier must expect the moved tree, which is what keeps a generated project honest.
        String verifier = read(definition.resolve("src/test/resources/projects/basic/verify.groovy"));
        assertTrue(verifier.contains("path.contains(\"/po/\")"),
                "the verifier must expect the persistence PO role beside the Repository");
        assertTrue(verifier.contains("path.contains(\"/dao/\")"),
                "the verifier must expect the persistence DAO role beside the Repository");
        assertTrue(verifier.contains("client/evaluation/impl/LocalEvaluationQueryClientImpl.java"),
                "the verifier must expect the local profile client behind the Client boundary");
    }

    private static Path testFile(String module, String relative) {
        return Path.of("..").resolve("egon-cola-source-web-open-" + module).resolve(TEST)
                .resolve(ROOT.replace('.', '/')).resolve(module).resolve(relative);
    }

    private static String moduleOf(Path path) {
        String text = path.toString().replace('\\', '/');
        for (String module : MODULES) {
            if (text.contains("/egon-cola-source-web-open-" + module + "/")) {
                return module;
            }
        }
        throw new AssertionError("path is outside the reviewed modules: " + path);
    }

    private static String beanName(Path converterPath) {
        String simple = converterPath.getFileName().toString().replace(".java", "");
        return Character.toLowerCase(simple.charAt(0)) + simple.substring(1);
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
        Path root = sources(module);
        if (!Files.isDirectory(root)) {
            // The open facade module ships Protobuf contracts only, so it has no Java source tree.
            return List.of();
        }
        return sources(root);
    }

    private static Path sources(String module) {
        return Path.of("..").resolve("egon-cola-source-web-open-" + module).resolve(MAIN)
                .resolve(ROOT.replace('.', '/')).resolve(module);
    }

    private static Path testSources(String module) {
        return Path.of("..").resolve("egon-cola-source-web-open-" + module).resolve(TEST)
                .resolve(ROOT.replace('.', '/'));
    }

    private static String read(String module, String relative) {
        return read(sources(module).resolve(relative));
    }

    private static String resource(String module, String relative) {
        return read(Path.of("..", "egon-cola-source-web-open-" + module, "src/main/resources").resolve(relative));
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
