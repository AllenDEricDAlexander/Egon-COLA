package top.egon.cola.archetype.source.webopen.facade;

import com.google.protobuf.Descriptors;
import top.egon.cola.archetype.source.webopen.facade.organization.v1.Grade;
import top.egon.cola.archetype.source.webopen.facade.organization.v1.SchoolClass;
import top.egon.cola.archetype.source.webopen.facade.organization.v1.User;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProtoContractTest {

    @Test
    void shouldExposeTheExactV1ServiceInventory() {
        assertEquals("egon.organization.v1", User.getDescriptor().getFile().getPackage());

        assertServices(User.getDescriptor().getFile(), Map.of(
                "UserService", List.of("CreateUser", "GetUser"),
                "RoleService", List.of("AssignRole"),
                "PermissionService", List.of("GrantPermission", "GetPermissionTree")));
        assertServices(Grade.getDescriptor().getFile(), Map.of(
                "GradeService", List.of("CreateGrade", "GetGrade"),
                "SchoolClassService", List.of("CreateSchoolClass", "GetSchoolClass", "AssignUser")));
    }

    @Test
    void shouldKeepLongIdsAndEmptyResults() {
        assertLongFields(User.getDescriptor(), "id");
        assertLongFields(Grade.getDescriptor(), "id");
        assertLongFields(SchoolClass.getDescriptor(), "id");

        assertEquals(1, field(User.getDescriptor(), "id").getNumber());
        assertEquals(1, field(Grade.getDescriptor(), "id").getNumber());
        assertEquals(1, field(SchoolClass.getDescriptor(), "id").getNumber());
        assertEquals("google.protobuf.Empty",
                User.getDescriptor().getFile().findServiceByName("RoleService")
                        .findMethodByName("AssignRole").getOutputType().getFullName());
    }

    private static void assertServices(
            Descriptors.FileDescriptor file, Map<String, List<String>> expected) {
        assertEquals(expected.size(), file.getServices().size());
        expected.forEach((serviceName, methods) -> {
            Descriptors.ServiceDescriptor service = file.findServiceByName(serviceName);
            assertNotNull(service, serviceName);
            assertEquals(methods.size(), service.getMethods().size(), serviceName);
            assertEquals(methods, service.getMethods().stream()
                    .map(Descriptors.MethodDescriptor::getName).toList());
        });
    }

    private static void assertLongFields(Descriptors.Descriptor message, String... names) {
        for (String name : names) {
            assertEquals(Descriptors.FieldDescriptor.Type.INT64, field(message, name).getType(), name);
        }
    }

    private static Descriptors.FieldDescriptor field(
            Descriptors.Descriptor message, String name) {
        Descriptors.FieldDescriptor field = message.findFieldByName(name);
        assertNotNull(field, message.getFullName() + "." + name);
        return field;
    }
}
