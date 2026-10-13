package top.egon.cola.archetype.source.light.adapter.graphql;

import graphql.GraphQLContext;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironmentImpl;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mapstruct.factory.Mappers;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.api.stream.StreamAddParams;
import org.redisson.client.RedisException;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.graphql.GraphQlAutoConfiguration;
import org.springframework.boot.autoconfigure.graphql.servlet.GraphQlWebMvcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockAsyncContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.context.request.ServletRequestAttributes;
import reactor.core.publisher.Mono;
import top.egon.cola.archetype.source.light.adapter.filter.RequestContext;
import top.egon.cola.archetype.source.light.adapter.handler.GraphQlExceptionResolver;
import top.egon.cola.archetype.source.light.adapter.user.graphql.UserResolver;
import top.egon.cola.archetype.source.light.adapter.teaching.graphql.CourseResolver;
import top.egon.cola.archetype.source.light.adapter.teaching.pojo.convertor.TeachingAdapterConvertor;
import top.egon.cola.archetype.source.light.application.teaching.manage.CourseManage;
import top.egon.cola.archetype.source.light.application.teaching.manage.SchoolClassManage;
import top.egon.cola.archetype.source.light.application.teaching.pojo.result.CourseResult;
import top.egon.cola.archetype.source.light.application.teaching.pojo.result.SchoolClassResult;
import top.egon.cola.archetype.source.light.adapter.user.pojo.convertor.UserAdapterConvertorImpl;
import top.egon.cola.archetype.source.light.application.user.manage.PermissionManage;
import top.egon.cola.archetype.source.light.application.user.manage.UserManage;
import top.egon.cola.archetype.source.light.infrastructure.user.po.UserPO;
import top.egon.cola.archetype.source.light.infrastructure.user.repo.UserRepository;
import top.egon.cola.archetype.source.light.start.config.NativeSecurityContractTest;
import top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.IdpAuthorizationObservationContextBO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Real schema, resolver and application/domain conversion; absence of callback authority must stop the data port. */
@GraphQlTest(UserResolver.class)
@ContextConfiguration(classes = {UserResolver.class, UserAdapterConvertorImpl.class, GraphQlExceptionResolver.class,
        NativeGraphQlSecurityContractTest.ReadFixtureConfiguration.class})
class NativeGraphQlSecurityContractTest {
    @Autowired private GraphQlTester graphQl;
    private static final UserRepository DATA = mock(UserRepository.class);

    @Test
    void missingAuthorizationContextCannotReadUserThroughTheRealSchema() {
        var row = UserPO.builder().externalId("external-1").name("Reader").email("reader@example.test").status("ACTIVE").build();
        row.setId(1001L); when(DATA.findCachedById(1001L)).thenReturn(row);
        graphQl.document("query NativeLightUser($id: ID!) { user(id: $id) { id name email status } }")
                .operationName("NativeLightUser").variable("id", "1001").execute()
                .errors().satisfy(errors -> assertThat(errors).as("Missing verified callback authority must fail before the real read").isNotEmpty());
        verify(DATA, never()).findCachedById(1001L);
    }

    @Test
    void everyNamedRootNeedsItsScopeAndItsSeparateResource(@TempDir Path temporary) throws Exception {
        var users = mock(UserRepository.class);
        var row = UserPO.builder().externalId("external-1").name("Reader").email("reader@example.test").status("ACTIVE").build();
        row.setId(1001L);
        when(users.findCachedById(1001L)).thenReturn(row);
        var courses = mock(CourseManage.class);
        var classes = mock(SchoolClassManage.class);
        when(courses.get(Mockito.any())).thenReturn(new CourseResult(1002L, "MATH", "Math", "ACTIVE"));
        when(classes.get(Mockito.any())).thenReturn(new SchoolClassResult(1003L, "Class", "2026-FALL", "ACTIVE", 0));
        try (var fixture = new NativeSecurityContractTest.SecurityFixture(temporary)) {
            NativeSecurityContractTest.runner(fixture, users)
                    .withConfiguration(AutoConfigurations.of(GraphQlAutoConfiguration.class, GraphQlWebMvcAutoConfiguration.class))
                    .withUserConfiguration(HttpGraphQlConfiguration.class)
                    .withBean("courseManageImpl", CourseManage.class, () -> courses)
                    .withBean("schoolClassManageImpl", SchoolClassManage.class, () -> classes)
                    .withBean("teachingAdapterConvertorImpl", TeachingAdapterConvertor.class,
                            () -> Mappers.getMapper(TeachingAdapterConvertor.class))
                    .withBean("courseResolver", CourseResolver.class,
                            () -> new CourseResolver(courses, classes, Mappers.getMapper(TeachingAdapterConvertor.class)))
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        var mvc = MockMvcBuilders.webAppContextSetup(context)
                                .addFilter(context.getBean("springSecurityFilterChain", Filter.class)).build();
                        var roots = Map.of("user", "user:read", "permissions", "permission:read",
                                "course", "course:read", "schoolClass", "school-class:read");
                        for (var root : roots.entrySet()) {
                            String argument = "permissions".equals(root.getKey()) ? "userId" : "id";
                            String query = "query Root { " + root.getKey() + "(" + argument + ": \"1001\") { "
                                    + ("permissions".equals(root.getKey()) ? "code" : "id") + " } }";
                            String body = fixture.mapper.writeValueAsString(Map.of("query", query, "operationName", "Root"));
                            String permission = NativeSecurityContractTest.SecurityFixture.SYSTEM + ":" + root.getValue();
                            String resource = NativeSecurityContractTest.SecurityFixture.SYSTEM + ".graphql.query." + root.getKey();
                            int checksBefore = fixture.permissionChecks;
                            mvc.perform(MockMvcRequestBuilders.post("/graphql").contentType("application/json").content(body)
                                            .header("Authorization", "Bearer " + fixture.token("1001", Set.of("openid"))))
                                    .andExpect(MockMvcResultMatchers.jsonPath("$.errors[0].extensions.code").value("AUTHORIZATION_DENIED"));
                            assertThat(fixture.permissionChecks).isEqualTo(checksBefore);
                            fixture.resources.remove(resource); fixture.clearSnapshots();
                            mvc.perform(MockMvcRequestBuilders.post("/graphql").contentType("application/json").content(body)
                                            .header("Authorization", "Bearer " + fixture.token("1001", Set.of(permission))))
                                    .andExpect(MockMvcResultMatchers.jsonPath("$.errors[0].extensions.code").value("AUTHORIZATION_DENIED"));
                            assertThat(fixture.permissionChecks).isGreaterThan(checksBefore);
                            Mockito.verifyNoInteractions(users, courses, classes, context.getBean("permissionManageImpl"));
                            fixture.resources.add(resource); fixture.clearSnapshots();
                            mvc.perform(MockMvcRequestBuilders.post("/graphql").contentType("application/json").content(body)
                                            .header("Authorization", "Bearer " + fixture.token("1001", Set.of(permission))))
                                    .andExpect(MockMvcResultMatchers.status().isOk())
                                    .andExpect(MockMvcResultMatchers.jsonPath("$.errors").doesNotExist())
                                    .andExpect("permissions".equals(root.getKey())
                                            ? MockMvcResultMatchers.jsonPath("$.data.permissions").isArray()
                                            : MockMvcResultMatchers.jsonPath("$.data." + root.getKey()).isNotEmpty());
                            Mockito.clearInvocations(users, courses, classes, context.getBean("permissionManageImpl"));
                        }
                    });
        }
    }

    @Test
    void signedHttpGraphQlUsesTheActualCaptureInstrumentationAndAdvisors(@TempDir Path temporary) throws Exception {
        var repository = mock(UserRepository.class);
        var row = UserPO.builder().externalId("external-1").name("Reader").email("reader@example.test").status("ACTIVE").build();
        row.setId(1001L); when(repository.findCachedById(1001L)).thenReturn(row);
        try (var fixture = new NativeSecurityContractTest.SecurityFixture(temporary)) {
            NativeSecurityContractTest.runner(fixture, repository)
                    .withConfiguration(AutoConfigurations.of(
                            GraphQlAutoConfiguration.class,
                            GraphQlWebMvcAutoConfiguration.class))
                    .withUserConfiguration(HttpGraphQlConfiguration.class)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        var mvc = MockMvcBuilders.webAppContextSetup(context)
                                .addFilter(context.getBean("springSecurityFilterChain", Filter.class)).build();
                        String query = "{\"query\":\"query NativeLightUser($id: ID!) { user(id: $id) { id name email status } }\",\"operationName\":\"NativeLightUser\",\"variables\":{\"id\":\"1001\"}}";
                        mvc.perform(MockMvcRequestBuilders.post("/graphql").contentType("application/json")
                                        .header("Authorization", "Bearer " + fixture.token("1001", Set.of("openid"))).content(query))
                                .andExpect(MockMvcResultMatchers.status().isOk())
                                .andExpect(MockMvcResultMatchers.jsonPath("$.errors[0].extensions.code").value("AUTHORIZATION_DENIED"));
                        verify(repository, never()).findCachedById(1001L);
                        mvc.perform(MockMvcRequestBuilders.post("/graphql").contentType("application/json")
                                        .header("Authorization", "Bearer " + fixture.token("1001", Set.of(NativeSecurityContractTest.SecurityFixture.SYSTEM + ":user:read"))).content(query))
                                .andExpect(MockMvcResultMatchers.status().isOk())
                                .andExpect(MockMvcResultMatchers.jsonPath("$.data.user.name").value("Reader"));
                        verify(repository).findCachedById(1001L);
                        long deniedBefore = fixture.events.stream().filter(event -> "AUTHORIZATION_DENIED".equals(event.get("reasonCode"))).count();
                        String aliases = "{\"query\":\"query TwoDeniedAliases { left: user(id: \\\"1001\\\") { id } right: user(id: \\\"1001\\\") { id } }\",\"operationName\":\"TwoDeniedAliases\"}";
                        mvc.perform(MockMvcRequestBuilders.post("/graphql").contentType("application/json")
                                        .header("Authorization", "Bearer " + fixture.token("1001", Set.of("openid"))).content(aliases))
                                .andExpect(MockMvcResultMatchers.status().isOk())
                                .andExpect(MockMvcResultMatchers.jsonPath("$.errors.length()").value(2))
                                .andExpect(MockMvcResultMatchers.jsonPath("$.errors[0].extensions.code").value("AUTHORIZATION_DENIED"))
                                .andExpect(MockMvcResultMatchers.jsonPath("$.errors[1].extensions.code").value("AUTHORIZATION_DENIED"));
                        assertThat(fixture.events.stream().filter(event -> "AUTHORIZATION_DENIED".equals(event.get("reasonCode"))).count()).isEqualTo(deniedBefore + 1);
                        Mockito.verifyNoMoreInteractions(repository);
                        assertThat(top.egon.cola.archetype.source.light.adapter.filter.RequestContextHolder.get()).isEmpty();
                        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
                        assertThat(MDC.get("tenantId")).isNull();
                    });
        }
    }

    @Test
    void twoConcurrentDelayedCallbacksRestoreExpiredDispatchAttributesAndRecordOneDenial(@TempDir Path temporary) throws Exception {
        var repository = mock(UserRepository.class);
        try (var fixture = new NativeSecurityContractTest.SecurityFixture(temporary)) {
            NativeSecurityContractTest.runner(fixture,repository).withUserConfiguration(HttpGraphQlConfiguration.class).run(context -> {
                assertThat(context).hasNotFailed();
                var request = new MockHttpServletRequest("POST","/graphql");
                var response = new MockHttpServletResponse();
                var captured = new AtomicReference<GraphQLContext>();
                SecurityContextHolder.getContext().setAuthentication(fixture.authenticatedUser("1001", Set.of("openid")));
                top.egon.cola.archetype.source.light.adapter.filter.RequestContextHolder.set(new RequestContext("subject-1001","request-1","trace-1",1001L));
                MDC.setContextMap(Map.of("tenantId","1001","userId","subject-1001","traceId","trace-1"));
                var dispatchAttributes = new ServletRequestAttributes(request,response);
                org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(dispatchAttributes);
                fixture.observationFilter.doFilter(request,response,(incoming,outgoing)-> {
                    var webRequest = new WebGraphQlRequest(URI.create("https://light.example.test/graphql"),
                            new HttpHeaders(),new LinkedMultiValueMap<>(),Map.of(),
                            Map.of("query","query TwoAliases { left: user(id: \"1001\") { id } right: user(id: \"1001\") { id } }"),"request-1",Locale.ENGLISH);
                    context.getBean(NativeGraphQlSecurityInterceptor.class).intercept(webRequest,next -> {
                        captured.set(next.toExecutionInput().getGraphQLContext()); return Mono.empty();
                    });
                    dispatchAttributes.requestCompleted();
                    var environment = DataFetchingEnvironmentImpl.newDataFetchingEnvironment().graphQLContext(captured.get())
                            .arguments(Map.of("id","1001")).build();
                    DataFetcher<?> delegate = env -> context.getBean(UserResolver.class).user(env.getArgument("id"));
                    var fetcher = context.getBean(NativeGraphQlSecurityInstrumentation.class).instrumentDataFetcher(delegate,null,null);
                    var ready = new CountDownLatch(2); var release = new CountDownLatch(1);
                    try (var workers = Executors.newFixedThreadPool(2)) {
                        var results = new ArrayList<Future<?>>();
                        for (int index=0;index<2;index++) {
                            int workerId=index;
                            results.add(workers.submit(()-> {
                                var previousSecurity=SecurityContextHolder.createEmptyContext();
                                SecurityContextHolder.setContext(previousSecurity);
                                var previousBusiness=new RequestContext("worker-"+workerId,"previous","previous",77L);
                                top.egon.cola.archetype.source.light.adapter.filter.RequestContextHolder.set(previousBusiness);
                                MDC.setContextMap(Map.of("tenantId","77","worker","worker-"+workerId)); var previousMdc=MDC.getCopyOfContextMap();
                                var previousAttributes=new ServletRequestAttributes(new MockHttpServletRequest()); previousAttributes.requestCompleted();
                                org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(previousAttributes);
                                ready.countDown();
                                try {
                                    assertThat(release.await(5,TimeUnit.SECONDS)).isTrue();
                                    Assertions.assertThatThrownBy(()->fetcher.get(environment)).isInstanceOf(AccessDeniedException.class);
                                    assertThat(SecurityContextHolder.getContext()).isSameAs(previousSecurity);
                                    assertThat(top.egon.cola.archetype.source.light.adapter.filter.RequestContextHolder.getRequired()).isSameAs(previousBusiness);
                                    assertThat(org.springframework.web.context.request.RequestContextHolder.getRequestAttributes()).isSameAs(previousAttributes);
                                    assertThat(MDC.getCopyOfContextMap()).isEqualTo(previousMdc);
                                } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException(interrupted); }
                                finally { SecurityContextHolder.clearContext(); top.egon.cola.archetype.source.light.adapter.filter.RequestContextHolder.clear(); org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes(); MDC.clear(); }
                            }));
                        }
                        try {
                            assertThat(ready.await(5,TimeUnit.SECONDS)).isTrue(); release.countDown();
                            for (var result:results) { result.get(5,TimeUnit.SECONDS); }
                        } catch (Exception failure) { throw new ServletException("Controlled concurrent callback failed",failure); }
                    }
                    var marker=(IdpAuthorizationObservationContextBO) captured.get().get(NativeGraphQlSecurityInterceptor.OBSERVATION_MARKER_KEY);
                    assertThat(marker.isConfirmed()).isTrue();
                });
                assertThat(fixture.events.stream().filter(event->"AUTHORIZATION_DENIED".equals(event.get("reasonCode"))).count()).isEqualTo(1);
                Mockito.verifyNoInteractions(repository);
                var marker=(IdpAuthorizationObservationContextBO) captured.get().get(NativeGraphQlSecurityInterceptor.OBSERVATION_MARKER_KEY);
                assertThat(marker.isActive()).isFalse();
                var lateEnvironment=DataFetchingEnvironmentImpl.newDataFetchingEnvironment().graphQLContext(captured.get()).build();
                var late=context.getBean(NativeGraphQlSecurityInstrumentation.class).instrumentDataFetcher(env->{throw new AssertionError("Closed request cannot run business");},null,null);
                Assertions.assertThatThrownBy(()->late.get(lateEnvironment)).isInstanceOf(AuthorizationService.RuntimeUnavailableException.class);
                SecurityContextHolder.clearContext(); top.egon.cola.archetype.source.light.adapter.filter.RequestContextHolder.clear(); org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes(); MDC.clear();
            });
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void asyncGraphQlDenialAuditFailureIsUnavailableAndNeverOrdinaryForbidden(@TempDir Path temporary) throws Exception {
        var repository = mock(UserRepository.class);
        try (var fixture = new NativeSecurityContractTest.SecurityFixture(temporary)) {
            Mockito.doAnswer(call -> {
                StreamAddParams<String,String> fields=call.getArgument(0);
                if ("DENIED".equals(fields.getEntries().get("result"))) { throw new RedisException("private-broker-address"); }
                fixture.events.add(Map.copyOf(fields.getEntries())); return new StreamMessageId(fixture.events.size(),1);
            }).when(fixture.auditStream).add(ArgumentMatchers.any(StreamAddArgs.class));
            NativeSecurityContractTest.runner(fixture,repository)
                    .withConfiguration(AutoConfigurations.of(GraphQlAutoConfiguration.class,
                            GraphQlWebMvcAutoConfiguration.class)).withUserConfiguration(HttpGraphQlConfiguration.class).run(context -> {
                assertThat(context).hasNotFailed();
                var mvc=MockMvcBuilders.webAppContextSetup(context).addFilter(context.getBean("springSecurityFilterChain",Filter.class)).build();
                var result=mvc.perform(MockMvcRequestBuilders.post("/graphql").contentType("application/json")
                                .header("Authorization","Bearer "+fixture.token("1001",Set.of("openid"))).content("{\"query\":\"query AuditFailure { user(id: \\\"1001\\\") { id } }\",\"operationName\":\"AuditFailure\"}"))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andExpect(MockMvcResultMatchers.jsonPath("$.errors[0].extensions.code").value("OAUTH_AUDIT_DELIVERY_UNAVAILABLE"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.errors[0].extensions.classification").value("INTERNAL_ERROR")).andReturn();
                assertThat(result.getResponse().getContentAsString()).doesNotContain("private-broker-address"); Mockito.verifyNoInteractions(repository);
            });
        }
    }

    @Test
    void asyncTimeoutClosesSharedAuthorityBeforeALateCallback(@TempDir Path temporary) throws Exception {
        var repository=mock(UserRepository.class);
        try (var fixture=new NativeSecurityContractTest.SecurityFixture(temporary)) {
            NativeSecurityContractTest.runner(fixture,repository).withUserConfiguration(HttpGraphQlConfiguration.class).run(context -> {
                assertThat(context).hasNotFailed();
                var request=new MockHttpServletRequest("POST","/graphql"); request.setAsyncSupported(true);
                var response=new MockHttpServletResponse();
                var captured=new AtomicReference<GraphQLContext>();
                SecurityContextHolder.getContext().setAuthentication(fixture.authenticatedUser("1001",Set.of("egon-cola-source-light:user:read")));
                top.egon.cola.archetype.source.light.adapter.filter.RequestContextHolder.set(new RequestContext("subject-1001","request-1","trace-1",1001L));
                org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request,response));
                fixture.observationFilter.doFilter(request,response,(incoming,outgoing)-> {
                    var web=new WebGraphQlRequest(URI.create("https://light.example.test/graphql"),new HttpHeaders(),
                            new LinkedMultiValueMap<>(),Map.of(),Map.of("query","{ user(id: \"1001\") { id } }"),"request-1",Locale.ENGLISH);
                    context.getBean(NativeGraphQlSecurityInterceptor.class).intercept(web,next->{captured.set(next.toExecutionInput().getGraphQLContext());return Mono.empty();});
                    request.startAsync(request,response);
                });
                var marker=(IdpAuthorizationObservationContextBO)captured.get().get(NativeGraphQlSecurityInterceptor.OBSERVATION_MARKER_KEY);
                assertThat(marker.isActive()).isTrue();
                var async=(MockAsyncContext)request.getAsyncContext();
                assertThat(async.getListeners()).hasSize(1); async.getListeners().getFirst().onTimeout(new AsyncEvent(async));
                assertThat(marker.isActive()).isFalse();
                var environment=DataFetchingEnvironmentImpl.newDataFetchingEnvironment().graphQLContext(captured.get()).build();
                var callback=context.getBean(NativeGraphQlSecurityInstrumentation.class).instrumentDataFetcher(env->{throw new AssertionError("Timed-out authority cannot enter business");},null,null);
                Assertions.assertThatThrownBy(()->callback.get(environment)).isInstanceOf(AuthorizationService.RuntimeUnavailableException.class);
                Mockito.verifyNoInteractions(repository);
                SecurityContextHolder.clearContext(); top.egon.cola.archetype.source.light.adapter.filter.RequestContextHolder.clear(); org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes(); MDC.clear();
            });
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import({UserResolver.class, UserAdapterConvertorImpl.class, GraphQlExceptionResolver.class,
            NativeGraphQlSecurityInterceptor.class, NativeGraphQlSecurityInstrumentation.class})
    static class HttpGraphQlConfiguration {
        @Bean("permissionManageImpl") PermissionManage unusedPermissions() { return mock(PermissionManage.class); }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class ReadFixtureConfiguration {
        @Bean("userManageImpl")
        UserManage userManage() { return NativeSecurityContractTest.readManage(DATA); }

        @Bean("permissionManageImpl")
        PermissionManage unusedPermissionPort() { return mock(PermissionManage.class); }
    }
}
