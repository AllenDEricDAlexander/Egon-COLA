package top.egon.cola.archetype.source.web.domain.teaching.entities;

import top.egon.cola.archetype.source.web.domain.teaching.enums.SchoolClassStatus;
import top.egon.cola.archetype.source.web.domain.teaching.vos.GradeCode;
import top.egon.cola.archetype.source.web.domain.teaching.vos.SchoolClassId;
import top.egon.cola.archetype.source.web.domain.user.vos.UserId;

import java.util.ArrayList;
import java.util.List;

public record SchoolClass(
        SchoolClassId id,
        String name,
        Long gradeId,
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

    public String getId() { return Long.toString(id.value()); }
    public String getName() { return name; }
    public String getGradeName() { return gradeName; }
    public List<String> getUserIds() { return userIds.stream().map(id -> Long.toString(id.value())).toList(); }
}
