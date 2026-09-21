package top.egon.cola.archetype.source.web.adapter;

import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.MetadataUtils;
import jakarta.validation.Validation;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import top.egon.cola.archetype.source.web.adapter.facade.impl.OrganizationFacadeSupport;
import top.egon.cola.archetype.source.web.adapter.facade.impl.OrganizationRpcContextDTO;
import top.egon.cola.archetype.source.web.adapter.facade.impl.OrganizationRpcContextInterceptor;
import top.egon.cola.archetype.source.web.adapter.pojo.convertor.OrganizationFacadeConverter;
import top.egon.cola.archetype.source.web.adapter.user.facade.impl.UserFacadeImpl;
import top.egon.cola.archetype.source.web.application.context.OrganizationRequestContext;
import top.egon.cola.archetype.source.web.application.context.OrganizationRequestContextHolder;
import top.egon.cola.archetype.source.web.application.exceptions.OrganizationApplicationException;
import top.egon.cola.archetype.source.web.application.exceptions.OrganizationFailureType;
import top.egon.cola.archetype.source.web.application.user.command.CreateUserCommand;
import top.egon.cola.archetype.source.web.application.user.manage.UserManage;
import top.egon.cola.archetype.source.web.application.user.result.UserDetailResult;
import top.egon.cola.archetype.source.web.facade.proto.CreateUserRpcRequest;
import top.egon.cola.archetype.source.web.facade.proto.UserServiceGrpc;
import top.egon.cola.archetype.source.web.facade.user.UserFacade;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.component.rpc.context.invocation.RpcInvocationMetadata;
import top.egon.cola.component.rpc.contract.validation.RpcContractValidator;
import top.egon.cola.component.rpc.provider.binding.RpcProviderBinding;
import top.egon.cola.component.rpc.provider.binding.RpcProviderMethodRegistry;
import top.egon.cola.component.rpc.provider.lifecycle.RpcProviderAvailabilityRegistry;
import top.egon.cola.component.rpc.provider.server.RpcServerServiceDefinitionFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NativeOrganizationRpcContextTest {

    private static final ValidationUtils VALIDATION_UTILS =
            new ValidationUtils(Validation.buildDefaultValidatorFactory().getValidator());

    /** Carries the three envelope strings so a unit check can observe the rejection mapping. */
    private record RejectionDTO(String code, String message, String traceId) {
    }

    @AfterEach
    void clearTestContext() { OrganizationRequestContextHolder.clear(); }

    @Test
    void preserves_existing_http_context_and_its_ownership() {
        var existing = new OrganizationRequestContext("http-actor", Set.of("TEACHER"), "http-trace");
        OrganizationRequestContextHolder.set(existing);
        Context.current().withValue(OrganizationRpcContextDTO.CONTEXT_KEY,
                new OrganizationRpcContextDTO("rpc-actor", "ADMIN", "rpc-trace", "request-1")).run(() -> {
                    OrganizationFacadeSupport.invoke(() -> {
                        assertThat(OrganizationRequestContextHolder.current().orElseThrow()).isSameAs(existing);
                        assertThat(OrganizationFacadeSupport.requestId()).isEqualTo("request-1");
                        return null;
                    }, unexpectedRejection());
                    assertThat(OrganizationRequestContextHolder.current().orElseThrow()).isSameAs(existing);
                });
        assertThat(OrganizationRpcContextDTO.current()).isNull();
    }

    @Test
    void maps_the_bounded_trace_onto_the_rejection_and_releases_only_the_created_context() {
        Context.current().withValue(OrganizationRpcContextDTO.CONTEXT_KEY,
                new OrganizationRpcContextDTO("actor-1", "TEACHER", "trace-1", "request-1")).run(() -> {
                    var rejection = OrganizationFacadeSupport.invoke(
                            () -> {
                                throw new OrganizationApplicationException(
                                        OrganizationFailureType.FORBIDDEN, "DENIED", "reason");
                            },
                            (code, message, traceId) -> new RejectionDTO(code, message, traceId));
                    assertThat(rejection.code()).isEqualTo("DENIED");
                    assertThat(rejection.traceId()).isEqualTo("trace-1");
                    assertThat(OrganizationRequestContextHolder.current()).isEmpty();
                });
        assertThat(OrganizationRpcContextDTO.current()).isNull();
    }

    @Test
    void retains_default_actor_roles_and_uuid_request_ids() {
        OrganizationFacadeSupport.invoke(() -> {
            var context = OrganizationRequestContextHolder.current().orElseThrow();
            assertThat(context.actorId()).isEqualTo("facade-system");
            assertThat(context.actorRoles()).isEqualTo(Set.of("SYSTEM"));
            assertThat(UUID.fromString(context.traceId())).isNotNull();
            assertThat(UUID.fromString(OrganizationFacadeSupport.requestId())).isNotNull();
            return null;
        }, unexpectedRejection());
        assertThat(OrganizationRequestContextHolder.current()).isEmpty();
    }

    @Test
    void does_not_promote_a_nonblank_but_empty_role_list_to_system() {
        Context.current().withValue(OrganizationRpcContextDTO.CONTEXT_KEY,
                new OrganizationRpcContextDTO("actor-1", " , , ", "trace-1", null)).run(() ->
                OrganizationFacadeSupport.invoke(() -> {
                    assertThat(OrganizationRequestContextHolder.current()
                            .orElseThrow().actorRoles()).isEmpty();
                    return null;
                }, unexpectedRejection()));
        assertThat(OrganizationRequestContextHolder.current()).isEmpty();
    }

    @Test
    void uses_framework_trace_only_when_the_explicit_trace_header_is_missing() {
        var trace = mock(TraceContext.class);
        when(trace.traceId()).thenReturn("framework-trace");
        var invocation = new RpcInvocationMetadata("service", "group", "1.0", "call-1", trace);
        Context.current().withValue(RpcInvocationMetadata.CONTEXT_KEY, invocation).run(() ->
                OrganizationFacadeSupport.invoke(() -> {
                    assertThat(OrganizationRequestContextHolder.current()
                            .orElseThrow().traceId()).isEqualTo("framework-trace");
                    return null;
                }, unexpectedRejection()));
        assertThat(OrganizationRequestContextHolder.current()).isEmpty();
    }

    @Test
    void positive_id_helper_still_rejects_non_positive_and_non_numeric_values() {
        assertThat(OrganizationFacadeSupport.positiveId("42", "id")).isEqualTo(42L);
        assertThatThrownBy(() -> OrganizationFacadeSupport.positiveId("0", "id"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OrganizationFacadeSupport.positiveId("abc", "id"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void carries_headers_through_native_grpc_binding_and_isolates_consecutive_calls() throws Exception {
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            var observed = new AtomicReference<OrganizationRequestContext>();
            var requestId = new AtomicReference<String>();
            var manage = mock(UserManage.class);
            when(manage.createUser(any())).thenAnswer(invocation -> {
                observed.set(OrganizationRequestContextHolder.current().orElseThrow());
                requestId.set(OrganizationFacadeSupport.requestId());
                if (invocation.getArgument(0, CreateUserCommand.class).name().equals("reject")) {
                    throw new OrganizationApplicationException(OrganizationFailureType.FORBIDDEN, "DENIED", "reason");
                }
                return new UserDetailResult(1L, "Mario", "mario@example.com", "ACTIVE", List.of());
            });
            var provider = new UserFacadeImpl(manage, Mappers.getMapper(OrganizationFacadeConverter.class),
                    new ValidationUtils(validators.getValidator()));
            var binding = new RpcProviderBinding(provider,
                    new RpcContractValidator(VALIDATION_UTILS).validate(UserFacade.class));
            var registry = new RpcProviderMethodRegistry(List.of(binding));
            var availability = new RpcProviderAvailabilityRegistry();
            availability.available(binding.serviceIdentity());
            var definition = new RpcServerServiceDefinitionFactory(availability).create(registry).getFirst();
            var name = InProcessServerBuilder.generateName();
            var server = InProcessServerBuilder.forName(name).directExecutor()
                    .addService(ServerInterceptors.intercept(definition, new OrganizationRpcContextInterceptor()))
                    .build().start();
            var channel = InProcessChannelBuilder.forName(name).directExecutor().build();
            try {
                var headers = headers("actor-1", " TEACHER,ADMIN,TEACHER ", "trace-1", "request-1");
                var stub = UserServiceGrpc.newBlockingStub(channel)
                        .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers));
                var response = stub.createUser(CreateUserRpcRequest.newBuilder()
                        .setName("Mario").setEmail("mario@example.com").build());
                assertThat(response.getSuccess()).isTrue();
                assertThat(observed.get()).isEqualTo(new OrganizationRequestContext("actor-1", Set.of("TEACHER", "ADMIN"), "trace-1"));
                assertThat(requestId.get()).isEqualTo("request-1");
                assertThat(OrganizationRequestContextHolder.current()).isEmpty();
                assertThat(OrganizationRpcContextDTO.current()).isNull();

                var rejected = UserServiceGrpc.newBlockingStub(channel).withInterceptors(
                        MetadataUtils.newAttachHeadersInterceptor(headers("actor-2", "STUDENT", "trace-2", "request-2")))
                        .createUser(CreateUserRpcRequest.newBuilder().setName("reject").setEmail("mario@example.com").build());
                assertThat(rejected.getSuccess()).isFalse();
                assertThat(rejected.getCode()).isEqualTo("DENIED");
                assertThat(rejected.getMessage()).isEqualTo("reason");
                assertThat(rejected.getTraceId()).isEqualTo("trace-2");
                assertThat(observed.get().actorId()).isEqualTo("actor-2");
                assertThat(requestId.get()).isEqualTo("request-2");
                assertThat(OrganizationRequestContextHolder.current()).isEmpty();
                assertThat(OrganizationRpcContextDTO.current()).isNull();

                UserServiceGrpc.newBlockingStub(channel).createUser(CreateUserRpcRequest.newBuilder()
                        .setName("Mario").setEmail("mario@example.com").build());
                assertThat(observed.get().actorId()).isEqualTo("facade-system");
                assertThat(observed.get().actorRoles()).isEqualTo(Set.of("SYSTEM"));
                assertThat(requestId.get()).isNotEqualTo("request-2");
            } finally {
                channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
                server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
            }
        }
    }

    private static <T> OrganizationFacadeSupport.RejectionMapper<T> unexpectedRejection() {
        return (code, message, traceId) -> {
            throw new AssertionError("unexpected rejection: " + code);
        };
    }

    private Metadata headers(String actorId, String roles, String traceId, String requestId) {
        var headers = new Metadata();
        Map.of("x-actor-id", actorId, "x-actor-roles", roles, "x-trace-id", traceId, "idempotency-key", requestId)
                .forEach((name, value) -> headers.put(Metadata.Key.of(name, Metadata.ASCII_STRING_MARSHALLER), value));
        return headers;
    }
}
