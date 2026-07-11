package ${package}.domain.entities.teaching;

import ${package}.domain.enums.teaching.SchoolClassStatus;
import ${package}.domain.vos.teaching.GradeCode;
import ${package}.domain.vos.teaching.SchoolClassId;
import ${package}.domain.vos.user.UserId;

import java.util.ArrayList;
import java.util.List;

public record SchoolClass(
        SchoolClassId id,
        String name,
        String gradeId,
        GradeCode gradeCode,
        String gradeName,
        SchoolClassStatus status,
        List<UserId> userIds) {

    public SchoolClass {
        name = name == null ? "" : name.trim();
        gradeName = gradeName == null ? "" : gradeName.trim();
        userIds = new ArrayList<>(userIds);
    }

    @Override
    public List<UserId> userIds() { return List.copyOf(userIds); }

    public void assignUser(UserId userId) { userIds.add(userId); }
    public boolean hasUser(UserId userId) { return userIds.contains(userId); }

    public String getId() { return id.value(); }
    public String getName() { return name; }
    public String getGradeName() { return gradeName; }
    public List<String> getUserIds() { return userIds.stream().map(UserId::value).toList(); }
}
