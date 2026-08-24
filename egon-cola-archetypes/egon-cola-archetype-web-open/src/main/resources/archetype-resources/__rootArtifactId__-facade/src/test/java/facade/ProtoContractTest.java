#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.facade;

import com.google.protobuf.Descriptors;
import ${package}.facade.evaluation.v1.Course;
import ${package}.facade.evaluation.v1.CourseSchedule;
import ${package}.facade.evaluation.v1.Exam;
import ${package}.facade.evaluation.v1.ExamPaper;
import ${package}.facade.evaluation.v1.Score;
import ${package}.facade.organization.v1.Grade;
import ${package}.facade.organization.v1.SchoolClass;
import ${package}.facade.organization.v1.User;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProtoContractTest {

    @Test
    void shouldExposeTheExactV1ServiceInventory() {
        assertEquals("egon.evaluation.v1", Course.getDescriptor().getFile().getPackage());
        assertEquals("egon.organization.v1", User.getDescriptor().getFile().getPackage());

        assertServices(Course.getDescriptor().getFile(), Map.of(
                "CourseService", List.of("CreateCourse", "ScheduleCourse", "GetCourse", "PageCourses")));
        assertServices(Exam.getDescriptor().getFile(), Map.of(
                "ExamService", List.of("CreateExam", "AttachPaper", "PublishExam", "GetExam")));
        assertServices(Score.getDescriptor().getFile(), Map.of(
                "ScoreService", List.of("RecordScore", "GetScore", "PageScores")));
        assertServices(User.getDescriptor().getFile(), Map.of(
                "UserService", List.of("CreateUser", "GetUser"),
                "RoleService", List.of("AssignRole"),
                "PermissionService", List.of("GrantPermission", "GetPermissionTree")));
        assertServices(Grade.getDescriptor().getFile(), Map.of(
                "GradeService", List.of("CreateGrade", "GetGrade"),
                "SchoolClassService", List.of("CreateSchoolClass", "GetSchoolClass", "AssignUser")));
    }

    @Test
    void shouldKeepLongIdsTimesEmptyAndStablePageFields() {
        assertLongFields(Course.getDescriptor(), "id");
        assertLongFields(CourseSchedule.getDescriptor(), "id", "course_id", "class_id");
        assertLongFields(Exam.getDescriptor(), "id", "course_id");
        assertLongFields(ExamPaper.getDescriptor(), "id", "exam_id");
        assertLongFields(Score.getDescriptor(), "id", "exam_id", "course_id", "student_id");
        assertLongFields(User.getDescriptor(), "id");
        assertLongFields(Grade.getDescriptor(), "id");
        assertLongFields(SchoolClass.getDescriptor(), "id");

        assertMessageField(CourseSchedule.getDescriptor(), "starts_at", "google.protobuf.Timestamp");
        assertMessageField(CourseSchedule.getDescriptor(), "ends_at", "google.protobuf.Timestamp");
        assertMessageField(Exam.getDescriptor(), "starts_at", "google.protobuf.Timestamp");
        assertMessageField(Exam.getDescriptor(), "ends_at", "google.protobuf.Timestamp");

        assertEquals(1, field(Course.getDescriptor(), "id").getNumber());
        assertEquals(5, field(Course.getDescriptor(), "status").getNumber());
        assertEquals(1, field(${package}.facade.evaluation.v1.PageCourseResponse.getDescriptor(), "records").getNumber());
        assertEquals(2, field(${package}.facade.evaluation.v1.PageCourseResponse.getDescriptor(), "current_page").getNumber());
        assertEquals(3, field(${package}.facade.evaluation.v1.PageCourseResponse.getDescriptor(), "total_pages").getNumber());
        assertEquals(4, field(${package}.facade.evaluation.v1.PageCourseResponse.getDescriptor(), "page_size").getNumber());
        assertEquals(5, field(${package}.facade.evaluation.v1.PageCourseResponse.getDescriptor(), "total_count").getNumber());
        assertEquals("egon.evaluation.v1.Course",
                Course.getDescriptor().getFile().findServiceByName("CourseService")
                        .findMethodByName("CreateCourse").getOutputType().getFullName());
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

    private static void assertMessageField(
            Descriptors.Descriptor message, String name, String fullName) {
        Descriptors.FieldDescriptor descriptor = field(message, name);
        assertEquals(Descriptors.FieldDescriptor.Type.MESSAGE, descriptor.getType(), name);
        assertEquals(fullName, descriptor.getMessageType().getFullName(), name);
    }

    private static Descriptors.FieldDescriptor field(
            Descriptors.Descriptor message, String name) {
        Descriptors.FieldDescriptor field = message.findFieldByName(name);
        assertNotNull(field, message.getFullName() + "." + name);
        return field;
    }
}
