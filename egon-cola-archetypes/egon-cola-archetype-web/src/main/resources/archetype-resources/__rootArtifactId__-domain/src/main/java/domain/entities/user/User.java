package ${package}.domain.entities.user;

import ${package}.domain.enums.UserStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class User {
    private final String id;
    private final String name;
    private final String email;
    private final UserStatus status;
    private final List<String> schoolClassIds;

    private User(String id, String name, String email, UserStatus status, List<String> schoolClassIds) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.status = status;
        this.schoolClassIds = new ArrayList<>(schoolClassIds);
    }

    public static User create(String id, String name, String email) {
        return new User(id, name, email, UserStatus.ACTIVE, List.of());
    }

    public static User restore(String id, String name, String email, UserStatus status, List<String> schoolClassIds) {
        return new User(id, name, email, status, schoolClassIds);
    }

    public void assignToClass(String schoolClassId) {
        schoolClassIds.add(schoolClassId);
    }

    public boolean hasSchoolClass(String schoolClassId) {
        return schoolClassIds.contains(schoolClassId);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public UserStatus getStatus() { return status; }
    public List<String> getSchoolClassIds() { return Collections.unmodifiableList(schoolClassIds); }
}
