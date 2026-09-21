package architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Generated-project contract for the Light archetype layering and common component rules.
 *
 * <p>Every assertion is a source-shape check so the contract is observable before the Spring
 * context exists; the runtime behaviour behind each rule is covered by the family fixtures.</p>
 */
class ArchetypeContractConvergenceTest {

    private static final String ROOT = "top.egon.cola.archetype.source.lightopen";

    private static final Path MAIN = Path.of("src/main/java");
    private static final Path TEST = Path.of("src/test/java");

    @Test
    void domainOnlyExposesServicesAndKeepsPortsOutOfDomain() throws IOException {
        List<Path> sources = sources(MAIN);
        String all = join(sources);

        for (String forbidden : List.of(
                ROOT + ".domain.teaching.gateway", ROOT + ".domain.user.gateway",
                ROOT + ".domain.teaching.event", ROOT + ".domain.user.event",
                ROOT + ".domain.teaching.client", ROOT + ".domain.user.client")) {
            assertFalse(all.contains(forbidden), "domain port package must be gone: " + forbidden);
        }
        assertFalse(all.contains("QueryGateway"), "gateway capability must be a domain Service");
        assertFalse(all.contains("EventPublisher"), "publisher capability must be a domain Service");
        assertFalse(all.contains("CachePort"), "data cache must not be a domain port");

        assertFalse(Files.exists(modulePath("domain/teaching/gateway")));
        assertFalse(Files.exists(modulePath("domain/user/gateway")));
        assertFalse(Files.exists(modulePath("domain/teaching/client")));
        assertFalse(Files.exists(modulePath("domain/user/client")));

        assertTrue(all.contains(ROOT + ".domain.teaching.service.TeachingQueryService"));
        assertTrue(all.contains(ROOT + ".domain.user.service.UserQueryService"));
        assertTrue(all.contains(ROOT + ".domain.teaching.service.TeachingEventService"));
        assertTrue(all.contains(ROOT + ".domain.user.service.UserEventService"));
        assertTrue(all.contains(ROOT + ".domain.teaching.service.CourseIdempotencyService"));
        assertTrue(all.contains(ROOT + ".domain.user.service.UserIdempotencyService"));
    }

    @Test
    void externalClientsLiveInInfrastructureClientAndImpl() throws IOException {
        List<Path> sources = sources(MAIN);
        String all = join(sources);

        assertTrue(Files.exists(modulePath("infrastructure/teaching/client/TeachingQueryClient.java")));
        assertTrue(Files.exists(modulePath("infrastructure/user/client/UserQueryClient.java")));
        for (String impl : List.of(
                "infrastructure/teaching/client/impl/LocalTeachingQueryClientImpl.java",
                "infrastructure/teaching/client/impl/RestTeachingQueryClientImpl.java",
                "infrastructure/user/client/impl/LocalUserQueryClientImpl.java",
                "infrastructure/user/client/impl/RestUserQueryClientImpl.java")) {
            assertTrue(Files.exists(modulePath(impl)), "client implementation expected: " + impl);
        }
        assertFalse(all.contains("LocalTeachingQueryService") , "client implementation must use the ClientImpl suffix");
        assertFalse(all.contains("RestUserQueryService"));
        for (String impl : List.of(
                "infrastructure/teaching/service/impl/TeachingQueryServiceImpl.java",
                "infrastructure/user/service/impl/UserQueryServiceImpl.java")) {
            assertTrue(Files.exists(modulePath(impl)), "query capability must be an infrastructure ServiceImpl: " + impl);
        }
    }

    @Test
    void applicationAndAdapterCarriersUseThePojoDirectories() throws IOException {
        for (String path : List.of(
                "application/teaching/pojo/command/CreateCourseCommand.java",
                "application/teaching/pojo/query/GetCourseQuery.java",
                "application/teaching/pojo/result/CourseResult.java",
                "application/teaching/pojo/convertor/TeachingApplicationConvertor.java",
                "application/user/pojo/command/CreateUserCommand.java",
                "application/user/pojo/query/GetUserQuery.java",
                "application/user/pojo/result/UserResult.java",
                "application/user/pojo/convertor/UserApplicationConvertor.java",
                "adapter/teaching/pojo/dto/CreateCourseRequest.java",
                "adapter/teaching/pojo/vo/CourseDetailVO.java",
                "adapter/teaching/pojo/convertor/TeachingAdapterConvertor.java",
                "adapter/user/pojo/dto/CreateUserRequest.java",
                "adapter/user/pojo/vo/UserDetailVO.java",
                "adapter/user/pojo/convertor/UserAdapterConvertor.java",
                "application/user/pojo/result/PermissionDetailResult.java")) {
            assertTrue(Files.exists(modulePath(path)), "expected pojo path: " + path);
        }
        assertFalse(Files.exists(modulePath("application/teaching/assemblers")));
        assertFalse(Files.exists(modulePath("application/user/assemblers")));
        String all = join(sources(MAIN));
        assertFalse(all.contains("Assembler"), "duplicate snapshot assembly must be deleted");
        assertFalse(all.contains(ROOT + ".application.teaching.command"));
        assertFalse(all.contains(ROOT + ".adapter.teaching.dto."));
    }

    @Test
    void technicalRepositoryStaysIndependentOfDaoPoAndConverter() throws IOException {
        for (String path : List.of(
                "infrastructure/teaching/dao/CourseDAO.java",
                "infrastructure/teaching/po/CoursePO.java",
                "infrastructure/teaching/converter/CoursePOConverter.java",
                "infrastructure/user/dao/UserDAO.java",
                "infrastructure/user/po/UserPO.java",
                "infrastructure/user/converter/UserPOConverter.java")) {
            assertTrue(Files.exists(modulePath(path)), "dao/po/converter must be a sibling of repo: " + path);
        }
        assertFalse(Files.exists(modulePath("infrastructure/teaching/repo/dao")));
        assertFalse(Files.exists(modulePath("infrastructure/user/repo/po")));
        String all = join(sources(MAIN));
        assertFalse(all.contains(ROOT + ".infrastructure.teaching.repo.dao"));
        assertFalse(all.contains(ROOT + ".infrastructure.user.repo.po"));

        String mapperXml = read(Path.of("src/main/resources/mybatis/mapper/teaching/CourseDAO.xml"));
        assertTrue(mapperXml.contains(ROOT + ".infrastructure.teaching.dao.CourseDAO"));
        assertTrue(mapperXml.contains(ROOT + ".infrastructure.teaching.po.CoursePO"));
        String scan = read(modulePath("start/StudentManagementApplication.java"));
        assertTrue(scan.contains(ROOT + ".infrastructure.teaching.dao"));
        assertTrue(scan.contains(ROOT + ".infrastructure.user.dao"));
        assertFalse(scan.contains("repo.dao"));
    }

    @Test
    void cachedReadsAndWritesStayOnTheSpringProxyOfTheSameRegionKey() throws IOException {
        String course = read(modulePath("infrastructure/teaching/repo/CourseRepository.java"));
        assertTrue(course.contains("@CacheConfig(cacheNames = \"CoursePO\")"));
        assertTrue(course.contains("public CoursePO findCachedById("));
        assertTrue(course.contains("public boolean updateCachedById("));
        assertTrue(course.contains("condition = \"#result\""), "false write must not evict");

        String domainService = read(modulePath("infrastructure/teaching/service/impl/CourseDomainServiceImpl.java"));
        assertTrue(domainService.contains("findCachedById("), "single id read must use the cached entry point");
        assertTrue(domainService.contains("updateCachedById("), "cached entity write must invalidate");
        for (Path path : sources(MAIN)) {
            String text = read(path);
            assertFalse(text.contains("this.findCachedById("), "cache entry must not be self-invoked: " + path);
            assertFalse(text.contains("this.updateCachedById("));
        }
        assertFalse(Files.exists(modulePath("infrastructure/teaching/cache")));
        assertFalse(Files.exists(modulePath("infrastructure/user/cache")));
    }

    @Test
    void idempotencyClaimIsADomainServiceThatValidatorsDoNotTouch() throws IOException {
        for (Path path : sources(MAIN)) {
            String name = path.getFileName().toString();
            if (!name.endsWith("Validator.java")) {
                continue;
            }
            String text = read(path);
            assertFalse(text.contains("RedisTemplate"), "validator must not write the idempotency store: " + name);
            assertFalse(text.contains("Idempotency"));
        }
        String claim = read(modulePath("infrastructure/teaching/service/impl/CourseIdempotencyServiceImpl.java"));
        assertTrue(claim.contains("setIfAbsent("));
        assertTrue(claim.contains("executeAfterRollback("), "rollback must release the original way");
        assertTrue(claim.contains("\":idempotency:course:\""));
        assertTrue(read(modulePath("infrastructure/user/service/impl/UserIdempotencyServiceImpl.java"))
                .contains("\":idempotency:user:\""));
        String manage = read(modulePath("application/teaching/manage/impl/CourseManageImpl.java"));
        assertTrue(manage.contains("courseIdempotencyService.claim("));
        assertTrue(manage.contains("DUPLICATE_REQUEST"));
    }

    @Test
    void manageDependsOnlyOnDomainServices() throws IOException {
        for (Path path : sources(MAIN)) {
            if (!path.getFileName().toString().endsWith("ManageImpl.java")) {
                continue;
            }
            String text = read(path);
            for (String forbidden : List.of("RabbitTemplate", "StringRedisTemplate", "RedisTemplate",
                    "EventPublisher", "CachePort", "QueryGateway", "MqMessageService", "Repository", "DAO")) {
                assertFalse(text.contains(forbidden),
                        "Manage must not reach infrastructure: " + forbidden + " in " + path);
            }
        }
        String teaching = read(modulePath("application/teaching/manage/impl/CourseManageImpl.java"));
        assertTrue(teaching.contains("teachingEventService.publish("));
        assertTrue(teaching.contains("teachingQueryService.findExternalCourse("));
    }

    @Test
    void mqTopologyAndRoutingComeFromOneEnumInsteadOfTypePrefixGuessing() throws IOException {
        String all = join(sources(MAIN));
        assertFalse(all.contains(".startsWith(\"class.\")"), "routing must not guess from the event type");
        assertFalse(all.contains(".startsWith(\"course.\")"));
        assertFalse(all.contains(".startsWith(\"authorization.\")"));

        String route = read(modulePath("infrastructure/mq/MqRouteEnum.java"));
        assertTrue(route.contains("enum MqRouteEnum implements EgonEnum"));
        for (String field : List.of("exchangeProperty", "queueSuffix", "routingKey",
                "deadLetterRoutingKey", "durable", "payloadType", "schemaVersion")) {
            assertTrue(route.contains(field), "route contract field missing: " + field);
        }
        for (String key : List.of("app.integrations.rabbitmq.exchange", "user.imported", "course.imported",
                ".dlq", "class.changed", "course.changed", "schedule.changed", "user.changed",
                "authorization.changed")) {
            assertTrue(route.contains(key), "route constant missing: " + key);
        }
        String topology = read(modulePath("infrastructure/config/RabbitMqConfig.java"));
        assertTrue(topology.contains("MqRouteEnum"), "topology must reuse the same route constants");
        String publish = read(modulePath("infrastructure/mq/impl/RabbitMqMessageServiceImpl.java"));
        assertTrue(publish.contains("convertAndSend("));
        assertTrue(publish.contains("executeAfterCommit("));
        assertTrue(publish.contains("getPayloadType()"), "payload type must be verified before publish");
        assertTrue(publish.contains("getSchemaVersion()"));
    }

    @Test
    void customExceptionsAreCommonScopedAndCarryTheStringStatusCode() throws IOException {
        for (String path : List.of(
                "common/exception/BaseBusinessException.java",
                "common/exception/TeachingDomainException.java",
                "common/exception/UserDomainException.java",
                "common/exception/TeachingUseCaseException.java",
                "common/exception/UserUseCaseException.java",
                "common/exception/TeachingFacadeException.java",
                "common/exception/UserFacadeException.java",
                "common/exception/ConfigDecryptException.java")) {
            assertTrue(Files.exists(modulePath(path)), "exception must sit in common: " + path);
        }
        assertFalse(Files.exists(modulePath("common/exceptions")));
        assertFalse(Files.exists(modulePath("domain/teaching/exceptions")));
        assertFalse(Files.exists(modulePath("facade/teaching/exceptions")));
        assertFalse(Files.exists(modulePath("start/config/encryption/ConfigDecryptException.java")));

        for (Path path : sources(MAIN)) {
            String text = read(path);
            assertFalse(text.contains("extends RuntimeException"),
                    "custom exceptions must inherit the common体系: " + path);
            if (path.startsWith(modulePath("common/exception")) && text.contains("class ") && !text.contains("interface ")) {
                assertTrue(text.contains("CommonException") || text.contains("BusinessException")
                                || text.contains("BaseBusinessException"),
                        "common exception must re-root on the common体系: " + path);
            }
        }
        String base = read(modulePath("common/exception/BaseBusinessException.java"));
        assertTrue(base.contains("extends BusinessException"), "business failure must re-root on common");
        assertTrue(base.contains("String code"), "the stable wire code stays a String");
        assertTrue(read(modulePath("common/exception/ConfigDecryptException.java"))
                .contains("extends CommonException"), "technical failure re-roots on CommonException");
        String handler = read(modulePath("adapter/handler/GlobalExceptionHandler.java"));
        assertTrue(handler.contains("getStatus()"), "the wire code is published through getStatus");
        assertFalse(handler.contains("exception.getCode()"));
    }

    @Test
    void handwrittenEnumsValidatorsConvertersAndCarriersImplementTheCommonContracts() throws IOException {
        for (Path path : sources(MAIN)) {
            String text = read(path);
            String name = path.getFileName().toString();
            if (text.contains("\npublic enum ") || text.contains("\nenum ")) {
                assertTrue(text.contains("implements EgonEnum") || text.contains("implements ErrorStatus")
                                || text.contains("extends "),
                        "handwritten enum must implement EgonEnum: " + name);
            }
            if (name.endsWith("Validator.java")) {
                assertTrue(text.contains("extends BaseValidator"), "validator contract: " + name);
                assertTrue(text.contains("getValidationUtils"), "validator must expose the common facade: " + name);
            }
            if (name.endsWith("Convertor.java") || name.endsWith("Converter.java")) {
                assertTrue(text.contains("BaseConverter<") || text.contains("BaseForwardConverter<"),
                        "semantic mapping must use the common converter: " + name);
                assertTrue(text.contains("@Mapper"), "mapping must be generated: " + name);
            }
        }
        String deleted = read(modulePath("common/enums/DeletedStatus.java"));
        assertTrue(deleted.contains("NOT_DELETED(0"));
        assertTrue(deleted.contains("DELETED(1"));
        String courseStatus = read(modulePath("domain/teaching/enums/CourseStatus.java"));
        assertTrue(courseStatus.contains("ACTIVE(0"));
        assertTrue(courseStatus.contains("DISABLED(1"));
        String facadeStatus = read(modulePath("facade/teaching/enums/CourseFacadeStatus.java"));
        assertTrue(facadeStatus.contains("UNKNOWN(2"));
        assertFalse(join(sources(MAIN)).contains(".ordinal()"), "ordinal must never act as the code");

        for (String dto : List.of("facade/teaching/dto/CourseDTO.java", "facade/user/dto/CreateUserDTO.java")) {
            assertTrue(read(modulePath(dto)).contains("implements BasePojo"), "carrier contract missing: " + dto);
        }
    }

    @Test
    void publishedFacadeIsSelfContainedAndFacadeAssertsDelegateToCommonValidation() throws IOException {
        List<Path> sources = sources(MAIN);
        String all = join(sources);
        assertFalse(all.contains(ROOT + ".facade.rpc"), "the open variant exposes plain facades");
        for (String removed : List.of("CourseRpcProvider", "SchoolClassRpcProvider",
                "UserRpcProvider", "PermissionRpcProvider", "RpcIdQuery", "LightFacadeConverter")) {
            assertFalse(all.contains(removed), "the RPC carrier must stay out of the open variant: " + removed);
        }
        assertFalse(Files.exists(modulePath("adapter/teaching/rpc")));
        assertFalse(Files.exists(modulePath("adapter/user/rpc")));
        assertFalse(Files.exists(modulePath("adapter/pojo")),
                "shared RPC carriers have no place in the open variant");
        assertFalse(Files.exists(modulePath("facade/rpc")));
        assertFalse(Files.exists(modulePath("facade/validation")),
                "the open facade publishes without a native RPC validation group");
        assertFalse(Files.exists(Path.of("src/main/proto")), "no protobuf chain in the open variant");
        for (Path path : sources) {
            if (path.startsWith(modulePath("facade"))) {
                assertFalse(read(path).contains(ROOT + ".adapter."),
                        "facade must stay a self-contained contract layer: " + path);
            }
        }

        for (String impl : List.of(
                "adapter/teaching/facade/impl/CourseFacadeImpl.java",
                "adapter/teaching/facade/impl/SchoolClassFacadeImpl.java",
                "adapter/user/facade/impl/UserFacadeImpl.java",
                "adapter/user/facade/impl/PermissionFacadeImpl.java")) {
            assertTrue(Files.exists(modulePath(impl)), "one published facade implementation per service: " + impl);
        }

        String asserts = read(modulePath("facade/teaching/utils/TeachingFacadeAssert.java"));
        assertTrue(asserts.contains("ValidationUtils.requireNotNull"));
        assertFalse(asserts.contains("if (value == null)"), "assert must not own the check");
        assertFalse(read(modulePath("facade/user/utils/UserFacadeAssert.java")).contains("if (value == null)"));
    }

    @Test
    void daoMonitoringAspectWatchesTheDaoLayerAndInfrastructureLogsStayOnFailureBoundaries() throws IOException {
        assertTrue(Files.exists(modulePath("infrastructure/aop/DaoMonitorAspect.java")));
        assertFalse(Files.exists(modulePath("infrastructure/aop/RepositoryMonitorAspect.java")));
        String aspect = read(modulePath("infrastructure/aop/DaoMonitorAspect.java"));
        assertTrue(aspect.contains("infrastructure.dao"));
        assertTrue(aspect.contains("*DAO"));
        assertFalse(aspect.contains("..repo.."), "service/cache/converter must not be counted as DAO");
        String logAspect = read(modulePath("infrastructure/aop/InfrastructureLogAspect.java"));
        assertTrue(logAspect.contains("client.impl"));
        assertTrue(logAspect.contains("mq"));
        assertFalse(logAspect.contains("converter"));
    }

    @Test
    void obsoleteInfrastructurePackagesAndDuplicateFacadesAreGone() throws IOException {
        for (String path : List.of(
                "domain/teaching/client", "domain/user/client",
                "infrastructure/teaching/cache", "infrastructure/user/cache",
                "infrastructure/teaching/mq", "infrastructure/user/mq",
                "adapter/teaching/rpc", "adapter/user/rpc",
                "facade/rpc", "common/exceptions",
                "application/teaching/assemblers", "application/user/assemblers",
                "application/teaching/command", "application/teaching/query", "application/teaching/result",
                "application/user/command", "application/user/query", "application/user/result",
                "adapter/teaching/dto", "adapter/teaching/vo", "adapter/teaching/convertor",
                "adapter/user/dto", "adapter/user/vo", "adapter/user/convertor")) {
            assertFalse(Files.exists(modulePath(path)), "obsolete package must not survive: " + path);
        }
        String all = join(sources(MAIN));
        assertFalse(all.contains("InMemoryCourseCacheService"));
        assertFalse(all.contains("InMemoryUserCacheService"));
        String local = read(modulePath("infrastructure/config/LocalAdapterConfiguration.java"));
        assertTrue(local.contains("TeachingQueryClient"));
        assertTrue(local.contains("UserQueryClient"));
        assertFalse(local.contains("CachePort"));
        assertFalse(local.contains("EventPublisher"));
    }

    @Test
    void testSurfaceKeepsTheLocalFakeAndCoversTheMovedContracts() throws IOException {
        // This fixture is the verifier, so its own forbidden-path literals must not count as usage.
        List<Path> fixtures = sources(TEST).stream()
                .filter(path -> !path.getFileName().equals(Path.of("ArchetypeContractConvergenceTest.java")))
                .toList();
        String all = join(fixtures);
        assertFalse(all.contains("CourseCachePort"));
        assertFalse(all.contains("TeachingEventPublisher"));
        assertFalse(all.contains("TeachingQueryGateway"));
        assertFalse(all.contains("RepositoryMonitorAspect"));
        assertFalse(all.contains("facade.rpc.LightFacadeConverter"));
        assertTrue(all.contains("RecordingMqMessageService") || all.contains("LocalMqMessageService"),
                "the local MQ fake stays under test instead of a production publisher");
    }

    private static Path modulePath(String relative) {
        return MAIN.resolve(ROOT.replace('.', '/')).resolve(relative);
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

    private static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("cannot read contract path " + path, exception);
        }
    }
}
