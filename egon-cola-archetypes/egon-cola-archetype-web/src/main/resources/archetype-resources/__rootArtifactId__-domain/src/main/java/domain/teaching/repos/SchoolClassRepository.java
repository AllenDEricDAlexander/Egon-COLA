package ${package}.domain.teaching.repos;

import ${package}.domain.teaching.entities.SchoolClass;
import ${package}.domain.teaching.vos.SchoolClassId;
import ${package}.domain.user.vos.UserId;

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
