package ${package}.application.manage.student;

public interface StudentManage {
    StudentView register(String name, String email);

    StudentView getById(String studentId);
}
