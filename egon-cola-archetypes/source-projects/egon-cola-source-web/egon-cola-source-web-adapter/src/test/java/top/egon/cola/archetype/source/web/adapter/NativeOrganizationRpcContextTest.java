package top.egon.cola.archetype.source.web.adapter;

import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.MetadataUtils;
import jakarta.validation.Validation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import top.egon.cola.archetype.source.web.adapter.facade.impl.OrganizationFacadeSupport;
import top.egon.cola.archetype.source.web.adapter.facade.impl.OrganizationRpcContextDTO;
import top.egon.cola.archetype.source.web.adapter.facade.impl.OrganizationRpcContextInterceptor;
import top.egon.cola.archetype.source.web.adapter.user.rpc.UserRpcProvider;
import top.egon.cola.archetype.source.web.application.context.OrganizationRequestContext;
import top.egon.cola.archetype.source.web.application.context.OrganizationRequestContextHolder;
import top.egon.cola.archetype.source.web.application.exceptions.OrganizationApplicationException;
import top.egon.cola.archetype.source.web.application.exceptions.OrganizationFailureType;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.component.rpc.context.invocation.RpcInvocationMetadata;
import top.egon.cola.component.rpc.contract.validation.RpcContractValidator;
import top.egon.cola.component.rpc.provider.binding.RpcProviderBinding;
import top.egon.cola.component.rpc.provider.binding.RpcProviderMethodRegistry;
import top.egon.cola.component.rpc.provider.lifecycle.RpcProviderAvailabilityRegistry;
import top.egon.cola.component.rpc.provider.server.RpcServerServiceDefinitionFactory;
import top.egon.cola.organization.facade.rpc.OrganizationRpcConverter;
import top.egon.cola.organization.facade.rpc.UserRpcService;
import top.egon.cola.organization.facade.rpc.proto.CreateUserRpcRequest;
import top.egon.cola.organization.facade.rpc.proto.UserServiceGrpc;
import top.egon.cola.organization.facade.user.UserFacade;
import top.egon.cola.organization.facade.user.RoleFacade;
import top.egon.cola.organization.facade.user.PermissionFacade;
import top.egon.cola.organization.facade.user.dto.UserDetailDTO;
import top.egon.cola.organization.facade.exceptions.OrganizationFacadeException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NativeOrganizationRpcContextTest {
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
                    });
                    assertThat(OrganizationRequestContextHolder.current().orElseThrow()).isSameAs(existing);
                });
        assertThat(OrganizationRpcContextDTO.current()).isNull();
    }

    @Test
    void removes_only_the_context_created_for_the_rpc_even_on_business_failure() {
        Context.current().withValue(OrganizationRpcContextDTO.CONTEXT_KEY,
                new OrganizationRpcContextDTO("actor-1", "TEACHER", "trace-1", "request-1")).run(() -> {
                    assertThatThrownBy(() -> OrganizationFacadeSupport.invoke(() -> {
                        throw new OrganizationApplicationException(OrganizationFailureType.FORBIDDEN, "DENIED", "reason");
                    })).isInstanceOfSatisfying(OrganizationFacadeException.class, failure -> {
                        assertThat(failure.code()).isEqualTo("DENIED");
                        assertThat(failure.traceId()).isEqualTo("trace-1");
                    });
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
        });
        assertThat(OrganizationRequestContextHolder.current()).isEmpty();
    }

    @Test
    void does_not_promote_a_nonblank_but_empty_role_list_to_system() {
        Context.current().withValue(OrganizationRpcContextDTO.CONTEXT_KEY,
                new OrganizationRpcContextDTO("actor-1", " , , ", "trace-1", null)).run(() ->
                OrganizationFacadeSupport.invoke(() -> assertThat(OrganizationRequestContextHolder.current()
                        .orElseThrow().actorRoles()).isEmpty()));
        assertThat(OrganizationRequestContextHolder.current()).isEmpty();
    }

    @Test
    void uses_framework_trace_only_when_the_explicit_trace_header_is_missing() {
        var trace = mock(TraceContext.class);
        when(trace.traceId()).thenReturn("framework-trace");
        var invocation = new RpcInvocationMetadata("service", "group", "1.0", "call-1", trace);
        Context.current().withValue(RpcInvocationMetadata.CONTEXT_KEY, invocation).run(() ->
                OrganizationFacadeSupport.invoke(() -> assertThat(OrganizationRequestContextHolder.current()
                        .orElseThrow().traceId()).isEqualTo("framework-trace")));
        assertThat(OrganizationRequestContextHolder.current()).isEmpty();
    }

    @Test
    void carries_headers_through_native_grpc_binding_and_isolates_consecutive_calls() throws Exception {
        try (var validators = Validation.buildDefaultValidatorFactory()) {
            var observed = new AtomicReference<OrganizationRequestContext>();
            var requestId = new AtomicReference<String>();
            var facade = mock(UserFacade.class);
            when(facade.createUser(any())).thenAnswer(invocation -> OrganizationFacadeSupport.invoke(() -> {
                observed.set(OrganizationRequestContextHolder.current().orElseThrow());
                requestId.set(OrganizationFacadeSupport.requestId());
                if (invocation.getArgument(0, top.egon.cola.organization.facade.user.dto.CreateUserDTO.class)
                        .name().equals("reject")) {
                    throw new OrganizationApplicationException(OrganizationFailureType.FORBIDDEN, "DENIED", "reason");
                }
                return new UserDetailDTO(1L, "Mario", "mario@example.com", "ACTIVE", List.of());
            }));
            var converter = Mappers.getMapper(OrganizationRpcConverter.class);
            var provider = new UserRpcProvider(facade, mock(RoleFacade.class), mock(PermissionFacade.class),
                    converter, new ValidationUtils(validators.getValidator()));
            var binding = new RpcProviderBinding(provider, new RpcContractValidator().validate(UserRpcService.class));
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

    private Metadata headers(String actorId, String roles, String traceId, String requestId) {
        var headers = new Metadata();
        Map.of("x-actor-id", actorId, "x-actor-roles", roles, "x-trace-id", traceId, "idempotency-key", requestId)
                .forEach((name, value) -> headers.put(Metadata.Key.of(name, Metadata.ASCII_STRING_MARSHALLER), value));
        return headers;
    }
}
