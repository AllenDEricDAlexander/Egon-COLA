package ${package}.domain.entities.user;

import ${package}.domain.enums.user.UserStatus;
import ${package}.domain.validators.user.UserDomainValidator;
import ${package}.domain.vos.user.RoleCode;
import ${package}.domain.vos.user.UserId;

import java.util.ArrayList;
import java.util.List;

public final class User {

    private final UserId id;
    private final String name;
    private final String email;
    private final UserStatus status;
    private final List<RoleCode> roleCodes;
    private final List<String> schoolClassIds;

    public User(UserId id, String name, String email, UserStatus status) {
        this(id, name, email, status, List.of(), List.of());
    }

    public User(UserId id, String name, String email, UserStatus status, List<RoleCode> roleCodes) {
        this(id, name, email, status, roleCodes, List.of());
    }

    private User(
            UserId id,
            String name,
            String email,
            UserStatus status,
            List<RoleCode> roleCodes,
            List<String> schoolClassIds) {
        this.id = id;
        this.name = UserDomainValidator.normalizeName(name);
        this.email = UserDomainValidator.normalizeEmail(email);
        this.status = status;
        this.roleCodes = new ArrayList<>(roleCodes);
        this.schoolClassIds = new ArrayList<>(schoolClassIds);
    }

    public static User restore(
            UserId id,
            String name,
            String email,
            UserStatus status,
            List<RoleCode> roleCodes,
            List<String> schoolClassIds) {
        return new User(id, name, email, status, roleCodes, schoolClassIds);
    }

    public void assignToClass(String schoolClassId) { schoolClassIds.add(schoolClassId); }

    public void assignRole(RoleCode roleCode) { roleCodes.add(roleCode); }

    public boolean hasSchoolClass(String schoolClassId) { return schoolClassIds.contains(schoolClassId); }

    public UserId id() { return id; }
    public String name() { return name; }
    public String email() { return email; }
    public UserStatus status() { return status; }
    public List<RoleCode> roleCodes() { return List.copyOf(roleCodes); }
    public String getId() { return id.value(); }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public UserStatus getStatus() { return status; }
    public List<String> getSchoolClassIds() { return List.copyOf(schoolClassIds); }
}
