package ${package}.domain.repos.teaching;

import ${package}.domain.entities.teaching.SchoolClass;
import ${package}.domain.vos.teaching.SchoolClassId;
import ${package}.domain.vos.user.UserId;

import java.util.Optional;

public interface SchoolClassRepository {
    SchoolClass save(SchoolClass schoolClass);
    Optional<SchoolClass> findById(SchoolClassId schoolClassId);
    boolean existsByGradeIdAndNameIgnoreCase(String gradeId, String name);
    void addUser(SchoolClassId schoolClassId, UserId userId);
    boolean hasUser(SchoolClassId schoolClassId, UserId userId);

    default Optional<SchoolClass> findById(String schoolClassId) {
        return findById(new SchoolClassId(schoolClassId));
    }
}
