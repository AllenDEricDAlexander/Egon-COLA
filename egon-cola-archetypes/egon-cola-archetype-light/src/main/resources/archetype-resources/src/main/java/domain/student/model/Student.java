package ${package}.domain.student.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Student {
    private final String id;
    private final String name;
    private final String email;
    private final StudentStatus status;
    private final List<String> courseIds;

    private Student(String id, String name, String email, StudentStatus status, List<String> courseIds) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.status = status;
        this.courseIds = new ArrayList<>(courseIds);
    }

    public static Student register(String id, String name, String email) {
        return new Student(id, name, email, StudentStatus.ACTIVE, List.of());
    }

    public static Student restore(String id, String name, String email, StudentStatus status, List<String> courseIds) {
        return new Student(id, name, email, status, courseIds);
    }

    public void assignCourse(String courseId) {
        courseIds.add(courseId);
    }

    public boolean hasCourse(String courseId) {
        return courseIds.contains(courseId);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public StudentStatus getStatus() { return status; }
    public List<String> getCourseIds() { return Collections.unmodifiableList(courseIds); }
}
