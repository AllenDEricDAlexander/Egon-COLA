package top.egon.cola.archetype.source.web.contract;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Frozen ownership contract of the web-owned native facade.
 *
 * <p>Every value asserted here is the value the shared organization artifact published before the
 * migration: Protobuf wire names, Egon RPC group/version/idempotence and provider bean names must
 * survive the move into the web-owned facade module unchanged.
 */
class OwnedFacadeContractTest {

    private static final String WIRE_PACKAGE = "top.egon.cola.organization.facade.rpc.v1.";
    private static final String FACADE_ROOT = "top.egon.cola.archetype.source.web.facade.";
    private static final String PROTO_ROOT = FACADE_ROOT + "proto.";
    private static final String ADAPTER_ROOT = "top.egon.cola.archetype.source.web.adapter.";
    private static final String RETIRED_SHARED_ROOT = "top.egon.cola.organization.facade.";
    private static final String GROUP = "student-management-organization";

    private record Operation(String methodName, String rpcName, boolean idempotent,
                             String requestMessage, String responseMessage) {
    }

    private record Contract(String interfaceName, String implName, String beanName,
                            String grpcService, List<Operation> operations) {
    }

    private static final List<Contract> CONTRACTS = List.of(
            new Contract("user.UserFacade", "user.facade.impl.UserFacadeImpl", "userFacade",
                    "UserServiceGrpc", List.of(
                    new Operation("createUser", "CreateUser", false, "CreateUserRpcRequest", "UserRpcResponse"),
                    new Operation("getUser", "GetUser", true, "GetUserRpcRequest", "UserRpcResponse"))),
            new Contract("user.RoleFacade", "user.facade.impl.RoleFacadeImpl", "roleFacade",
                    "RoleServiceGrpc", List.of(
                    new Operation("assignRole", "AssignRole", false, "AssignRoleRpcRequest", "RpcResponse"))),
            new Contract("user.PermissionFacade", "user.facade.impl.PermissionFacadeImpl", "permissionFacade",
                    "PermissionServiceGrpc", List.of(
                    new Operation("grantPermission", "GrantPermission", false, "GrantPermissionRpcRequest", "RpcResponse"),
                    new Operation("getPermissionTree", "GetPermissionTree", true, "GetPermissionTreeRpcRequest", "PermissionTreeRpcResponse"))),
            new Contract("teaching.GradeFacade", "teaching.facade.impl.GradeFacadeImpl", "gradeFacade",
                    "GradeServiceGrpc", List.of(
                    new Operation("createGrade", "CreateGrade", false, "CreateGradeRpcRequest", "GradeRpcResponse"),
                    new Operation("getGrade", "GetGrade", true, "GetGradeRpcRequest", "GradeRpcResponse"))),
            new Contract("teaching.SchoolClassFacade", "teaching.facade.impl.SchoolClassFacadeImpl", "schoolClassFacade",
                    "SchoolClassServiceGrpc", List.of(
                    new Operation("createSchoolClass", "CreateSchoolClass", false, "CreateSchoolClassRpcRequest", "SchoolClassRpcResponse"),
                    new Operation("getSchoolClass", "GetSchoolClass", true, "GetSchoolClassRpcRequest", "SchoolClassRpcResponse"),
                    new Operation("assignUser", "AssignUser", false, "AssignUserRpcRequest", "RpcResponse"))));

    @Test
    void declares_one_owned_facade_per_native_contract_with_frozen_rpc_metadata() {
        for (Contract contract : CONTRACTS) {
            Class<?> facade = load(FACADE_ROOT + contract.interfaceName());
            assertThat(facade.isInterface()).as("%s stays an interface", facade).isTrue();

            Annotation service = annotation(facade, "EgonRpcService");
            assertThat(attribute(service, "group")).as("%s group", facade).isEqualTo(GROUP);
            assertThat(attribute(service, "version")).as("%s version", facade).isEqualTo("1.0.0");
            assertThat(attribute(service, "retries")).as("%s retries", facade).isEqualTo(0);
            assertThat(((Class<?>) attribute(service, "grpcClass")).getName())
                    .as("%s grpc owner", facade).isEqualTo(PROTO_ROOT + contract.grpcService());

            Set<String> declared = new LinkedHashSet<>();
            for (Method method : facade.getDeclaredMethods()) {
                assertThat(method.getParameterCount()).as("%s arity", method).isEqualTo(1);
                Annotation rpcMethod = annotation(method, "EgonRpcMethod");
                declared.add(signature(method.getName(), String.valueOf(attribute(rpcMethod, "name")),
                        String.valueOf(attribute(rpcMethod, "idempotent")),
                        method.getParameterTypes()[0].getName(), method.getReturnType().getName()));
            }
            Set<String> expected = new LinkedHashSet<>();
            for (Operation operation : contract.operations()) {
                expected.add(signature(operation.methodName(), operation.rpcName(),
                        String.valueOf(operation.idempotent()),
                        PROTO_ROOT + operation.requestMessage(), PROTO_ROOT + operation.responseMessage()));
            }
            assertThat(declared).as("%s operations", facade).containsExactlyInAnyOrderElementsOf(expected);
        }
    }

    @Test
    void keeps_every_frozen_protobuf_wire_name_inside_the_owned_java_package() {
        List<String> messages = new ArrayList<>();
        for (Contract contract : CONTRACTS) {
            for (Operation operation : contract.operations()) {
                messages.add(operation.requestMessage());
                messages.add(operation.responseMessage());
            }
        }
        for (String message : new LinkedHashSet<>(messages)) {
            Class<?> type = load(PROTO_ROOT + message);
            Object descriptor = staticNoArg(type, "getDescriptor");
            assertThat(invoke(descriptor, "getFullName"))
                    .as("%s wire name", message).isEqualTo(WIRE_PACKAGE + message);
        }
        for (Contract contract : CONTRACTS) {
            Class<?> grpcOwner = load(PROTO_ROOT + contract.grpcService());
            Object serviceDescriptor = staticNoArg(grpcOwner, "getServiceDescriptor");
            assertThat(invoke(serviceDescriptor, "getName")).as("%s service wire name", grpcOwner)
                    .isEqualTo(WIRE_PACKAGE + contract.grpcService().replace("Grpc", ""));
        }
    }

    @Test
    void merges_the_split_provider_and_dto_facade_into_one_named_provider_component() {
        for (Contract contract : CONTRACTS) {
            Class<?> facade = load(FACADE_ROOT + contract.interfaceName());
            Class<?> impl = load(ADAPTER_ROOT + contract.implName());
            assertThat(facade.isAssignableFrom(impl)).as("%s implements %s", impl, facade).isTrue();
            annotation(impl, "EgonRpcProvider");
            assertThat(springBeanName(impl)).as("%s bean name", impl).isEqualTo(contract.beanName());
            assertThat(impl.getInterfaces()).as("%s owns exactly one facade", impl).containsExactly(facade);
        }
    }

    @Test
    void keeps_the_local_query_carrier_in_the_adapter_and_retires_the_shared_artifact() {
        load(ADAPTER_ROOT + "pojo.dto.RpcIdQuery");
        for (String retired : List.of("user.rpc.UserRpcProvider", "teaching.rpc.SchoolClassRpcProvider")) {
            assertAbsent(ADAPTER_ROOT + retired);
        }
        for (String retired : List.of("rpc.UserRpcService", "rpc.RoleRpcService", "rpc.PermissionRpcService",
                "rpc.GradeRpcService", "rpc.SchoolClassRpcService", "rpc.OrganizationRpcConverter",
                "rpc.RpcIdQuery", "rpc.RpcSchoolClassQuery", "user.UserFacade", "user.RoleFacade",
                "user.PermissionFacade", "teaching.GradeFacade", "teaching.SchoolClassFacade",
                "user.dto.UserDetailDTO", "teaching.dto.GradeDetailDTO", "exceptions.OrganizationFacadeException")) {
            assertAbsent(RETIRED_SHARED_ROOT + retired);
        }
    }

    private static String signature(String... parts) {
        return String.join("|", parts);
    }

    private static void assertAbsent(String typeName) {
        assertThatExceptionOfType(ClassNotFoundException.class)
                .as("%s must no longer resolve", typeName)
                .isThrownBy(() -> Class.forName(typeName));
    }

    private static Class<?> load(String typeName) {
        try {
            return Class.forName(typeName);
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("Missing owned facade contract: " + typeName, exception);
        }
    }

    private static Annotation annotation(AnnotatedElement element, String simpleName) {
        for (Annotation candidate : element.getAnnotations()) {
            if (candidate.annotationType().getSimpleName().equals(simpleName)) {
                return candidate;
            }
        }
        throw new AssertionError(element + " must be annotated with @" + simpleName);
    }

    private static Object attribute(Annotation annotation, String name) {
        return invoke(annotation, name);
    }

    private static String springBeanName(Class<?> type) {
        for (Annotation candidate : type.getAnnotations()) {
            String simpleName = candidate.annotationType().getSimpleName();
            if (simpleName.equals("Component") || simpleName.equals("Service")) {
                return String.valueOf(attribute(candidate, "value"));
            }
        }
        throw new AssertionError(type + " must be a named Spring component");
    }

    private static Object staticNoArg(Class<?> type, String name) {
        try {
            return type.getMethod(name).invoke(null);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(type + " must expose static " + name + "()", exception);
        }
    }

    private static Object invoke(Object target, String name) {
        try {
            return target.getClass().getMethod(name).invoke(target);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(target.getClass() + " must expose " + name + "()", exception);
        }
    }
}
